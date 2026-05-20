#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import socket
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WATCH_TOOLS = ROOT / "tools/watch-capture"
BACKEND_RUNNER = ROOT / "tools/health-wsl-backend-run.sh"
STACK = ROOT / "tools/health-wsl-stack.sh"
ENV_SCRIPT = ROOT / "tools/health-wsl-env.sh"
EXTRACT_SCRIPT = WATCH_TOOLS / "extract_docx_text.py"
CONVERT_SCRIPT = WATCH_TOOLS / "backend_log_to_capture.py"
COMPARE_SCRIPT = WATCH_TOOLS / "compare_watch_protocol.py"


def iso_now() -> str:
    return datetime.now().astimezone().isoformat(timespec="seconds")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-id", default=datetime.now().strftime("%Y%m%d-%H%M%S"))
    parser.add_argument("--duration-seconds", type=int, default=10800)
    parser.add_argument("--target-ip", default="10.8.138.180")
    parser.add_argument("--target-imei", default="861265062579435")
    parser.add_argument("--protocol-docx", default="/mnt/c/Users/j/Desktop/智能手表开发协议.docx")
    return parser.parse_args()


def socket_open(port: int, timeout: float = 1.5) -> bool:
    sock = socket.socket()
    sock.settimeout(timeout)
    try:
        sock.connect(("127.0.0.1", port))
        return True
    except OSError:
        return False
    finally:
        sock.close()


def wait_port(port: int, timeout_seconds: int = 180) -> bool:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        if socket_open(port):
            return True
        time.sleep(1)
    return False


def stack_action(service: str, action: str) -> None:
    completed = subprocess.run(["bash", str(STACK), service, action], cwd=ROOT, text=True, capture_output=True, check=False)
    if completed.returncode != 0:
        raise RuntimeError((completed.stderr or completed.stdout or f"stack {service} {action} failed").strip())


def start_backend(stdout_path: Path, stderr_path: Path, **env_vars: str) -> subprocess.Popen[str]:
    env_cmd = " ".join(f"{key}={json.dumps(value)}" for key, value in env_vars.items())
    command = f"source {ENV_SCRIPT} && {env_cmd} bash {BACKEND_RUNNER}"
    return subprocess.Popen(
        ["/bin/bash", "-lc", command],
        cwd=ROOT,
        text=True,
        stdout=stdout_path.open("w", encoding="utf-8"),
        stderr=stderr_path.open("w", encoding="utf-8"),
    )


def write_status(path: Path, phase: str, run_id: str, run_dir: Path, target_ip: str, target_imei: str, extra: dict[str, object] | None = None) -> None:
    payload: dict[str, object] = {
        "phase": phase,
        "updated_at": iso_now(),
        "run_id": run_id,
        "run_dir": str(run_dir),
        "target_ip": target_ip,
        "target_imei": target_imei,
    }
    if extra:
        payload.update(extra)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def collect_tcp_observe(path: Path, target_ip: str) -> None:
    completed = subprocess.run(["ss", "-tn"], text=True, capture_output=True, check=False)
    now = iso_now()
    with path.open("a", encoding="utf-8") as handle:
        for line in (completed.stdout or "").splitlines():
            if target_ip not in line:
                continue
            parts = line.split()
            if len(parts) < 5:
                continue
            record = {
                "ts": now,
                "state": parts[0],
                "local_address": parts[3],
                "remote_address": parts[4],
            }
            handle.write(json.dumps(record, ensure_ascii=False) + "\n")


def main() -> int:
    args = parse_args()
    run_dir = ROOT / f"runtime-logs/watch-capture/{args.run_id}"
    run_dir.mkdir(parents=True, exist_ok=True)
    status_path = run_dir / "status.json"
    backend_out = run_dir / "backend.out.log"
    backend_err = run_dir / "backend.err.log"
    tcp_observe = run_dir / "tcp-observe.jsonl"
    protocol_text_path = run_dir / "protocol.txt"
    capture_path = run_dir / "capture-from-backend-log.jsonl"
    report_path = run_dir / "comparison-report.md"

    backend_proc: subprocess.Popen[str] | None = None
    try:
        write_status(status_path, "preparing", args.run_id, run_dir, args.target_ip, args.target_imei)
        subprocess.run(
            ["python3", str(EXTRACT_SCRIPT), args.protocol_docx, "--output", str(protocol_text_path)],
            cwd=ROOT,
            check=True,
            stdout=(run_dir / "extract-docx.log").open("w", encoding="utf-8"),
            stderr=subprocess.STDOUT,
        )

        stack_action("simulator", "stop")
        stack_action("backend", "stop")
        time.sleep(2)

        write_status(status_path, "starting_backend", args.run_id, run_dir, args.target_ip, args.target_imei)
        backend_proc = start_backend(
            backend_out,
            backend_err,
            SERVER_PORT="8080",
            NETTY_SERVER_PORT="9000",
            LOGGING_LEVEL_COM_XZKJ_HEALTH_PROTOCOL="DEBUG",
            LOGGING_LEVEL_COM_XZKJ_HEALTH_HANDLER="INFO",
        )
        if not wait_port(9000, 180):
            raise RuntimeError("Backend did not listen on 9000 within timeout")

        end = time.time() + args.duration_seconds
        write_status(
            status_path,
            "capturing",
            args.run_id,
            run_dir,
            args.target_ip,
            args.target_imei,
            {
                "backend_pid": backend_proc.pid,
                "started_at": iso_now(),
                "expected_end_at": datetime.fromtimestamp(end).astimezone().isoformat(timespec="seconds"),
            },
        )
        while time.time() < end:
            collect_tcp_observe(tcp_observe, args.target_ip)
            time.sleep(1)

        write_status(status_path, "converting", args.run_id, run_dir, args.target_ip, args.target_imei)
        subprocess.run(
            ["python3", str(CONVERT_SCRIPT), "--backend-log", str(backend_out), "--output", str(capture_path)],
            cwd=ROOT,
            check=True,
            stdout=(run_dir / "convert.log").open("w", encoding="utf-8"),
            stderr=subprocess.STDOUT,
        )

        write_status(status_path, "comparing", args.run_id, run_dir, args.target_ip, args.target_imei)
        subprocess.run(
            [
                "python3",
                str(COMPARE_SCRIPT),
                "--capture",
                str(capture_path),
                "--protocol-text",
                str(protocol_text_path),
                "--target-ip",
                args.target_ip,
                "--target-imei",
                args.target_imei,
                "--output",
                str(report_path),
            ],
            cwd=ROOT,
            check=True,
            stdout=(run_dir / "compare.log").open("w", encoding="utf-8"),
            stderr=subprocess.STDOUT,
        )

        tcp_hit_count = len(tcp_observe.read_text(encoding="utf-8").splitlines()) if tcp_observe.exists() else 0
        write_status(status_path, "complete", args.run_id, run_dir, args.target_ip, args.target_imei, {"backend_pid": backend_proc.pid, "completed_at": iso_now(), "tcp_hit_count": tcp_hit_count})
        return 0
    except Exception as exc:  # noqa: BLE001
        write_status(status_path, "failed", args.run_id, run_dir, args.target_ip, args.target_imei, {"error": str(exc)})
        raise
    finally:
        if backend_proc and backend_proc.poll() is None:
            backend_proc.terminate()
            try:
                backend_proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                backend_proc.kill()
        if status_path.exists():
            phase = json.loads(status_path.read_text(encoding="utf-8")).get("phase")
            if phase in {"complete", "failed"}:
                write_status(status_path, "restoring_backend", args.run_id, run_dir, args.target_ip, args.target_imei)
                stack_action("backend", "start")
                write_status(status_path, "done", args.run_id, run_dir, args.target_ip, args.target_imei, {"restored_backend": True, "restored_at": iso_now()})


if __name__ == "__main__":
    sys.exit(main())
