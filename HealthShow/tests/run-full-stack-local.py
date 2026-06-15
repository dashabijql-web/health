#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
import shlex
import socket
import subprocess
import sys
from contextlib import closing
from datetime import datetime
from pathlib import Path
from typing import Any
from urllib.error import URLError
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[2]
SHOW = ROOT / "HealthShow"
DATA = ROOT / "HealthData"
TOOLS = ROOT / "tools"
WSL_STACK = TOOLS / "health-wsl-stack.sh"
APP_YML = DATA / "src/main/resources/application.yml"
BACKEND_POM = DATA / "pom.xml"
BACKEND_REGRESSION_SCRIPT = DATA / "scripts/run_backend_regression.py"
LATEST_FILE = SHOW / "tests/runs/latest-full-stack-local.json"
SQLCMD_CANDIDATES = [os.environ.get("SQLCMD_BIN", ""), "sqlcmd"]


def now_iso() -> str:
    return datetime.now().astimezone().isoformat(timespec="seconds")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-source", choices=["old", "new", "both"], default="old")
    parser.add_argument("--skip-backend-tests", action="store_true")
    parser.add_argument("--no-start-simulator-for-old", action="store_true")
    parser.add_argument("--no-stop-simulator-for-new", action="store_true")
    return parser.parse_args()


class Runner:
    def __init__(self, args: argparse.Namespace) -> None:
        self.args = args
        self.run_id = f"{datetime.now().strftime('%Y%m%d-%H%M%S')}-full-stack-local-{args.data_source}"
        self.out_dir = SHOW / "tests/runs" / self.run_id
        self.summary_file = self.out_dir / "full-stack-local-summary.json"
        self.summary_md_file = self.out_dir / "summary.md"
        self.hermes_archive_file = self.out_dir / "hermes-archive.md"
        self.log_file = self.out_dir / "full-stack-local.log"
        self.started_at = datetime.now().astimezone()
        self.started_processes: list[dict[str, Any]] = []
        self.stopped_processes: list[dict[str, Any]] = []
        self.steps: list[dict[str, Any]] = []
        self.phases: list[dict[str, Any]] = []
        self.data_source_facts: dict[str, Any] = {}
        self.out_dir.mkdir(parents=True, exist_ok=True)
        self.sql_password = self.resolve_sql_password()
        if self.sql_password:
            os.environ.setdefault("SQL_PASSWORD", self.sql_password)
            os.environ.setdefault("DB_PASSWORD", self.sql_password)

    def log(self, message: str) -> None:
        line = f"[{now_iso()}] {message}"
        print(line, flush=True)
        with self.log_file.open("a", encoding="utf-8") as handle:
            handle.write(f"{line}\n")

    def resolve_sql_password(self) -> str:
        for key in ("SQL_PASSWORD", "DB_PASSWORD"):
            value = os.environ.get(key, "").strip()
            if value:
                return value
        if APP_YML.exists():
            match = re.search(r"DB_PASSWORD:([^}]+)", APP_YML.read_text(encoding="utf-8", errors="ignore"))
            if match:
                return match.group(1).strip()
        return ""

    def shell_env_command(self, inner: str) -> str:
        return f"source {shlex.quote(str(TOOLS / 'health-wsl-env.sh'))} && {inner}"

    def run_shell(
        self,
        command: str,
        cwd: Path,
        stdout_path: Path | None = None,
        stderr_path: Path | None = None,
        env: dict[str, str] | None = None,
    ) -> int:
        merged_env = os.environ.copy()
        if env:
            merged_env.update(env)
        if stdout_path is None and stderr_path is None:
            completed = subprocess.run(
                ["/bin/bash", "-lc", command],
                cwd=cwd,
                env=merged_env,
                text=True,
                capture_output=True,
                check=False,
            )
            output = (completed.stdout or "") + (completed.stderr or "")
            if output:
                self.log(output.rstrip())
            return completed.returncode

        with stdout_path.open("w", encoding="utf-8") as stdout_handle, stderr_path.open(
            "w", encoding="utf-8"
        ) as stderr_handle:
            completed = subprocess.run(
                ["/bin/bash", "-lc", command],
                cwd=cwd,
                env=merged_env,
                stdout=stdout_handle,
                stderr=stderr_handle,
                check=False,
            )
        return completed.returncode

    def check_port(self, port: int, timeout: float = 1.5) -> bool:
        with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as sock:
            sock.settimeout(timeout)
            try:
                sock.connect(("127.0.0.1", port))
                return True
            except OSError:
                return False

    def check_http(self, url: str, timeout: float = 3.0) -> bool:
        request = Request(url, method="GET")
        try:
            with urlopen(request, timeout=timeout) as response:
                return 200 <= response.status < 400
        except URLError:
            return False

    def parse_stack_status(self) -> dict[str, dict[str, Any]]:
        completed = subprocess.run(
            ["bash", str(WSL_STACK), "status"],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )
        services: dict[str, dict[str, Any]] = {}
        for raw_line in (completed.stdout or "").splitlines():
            line = raw_line.strip()
            match = re.match(r"^(backend|frontend|simulator) (running|stopped)(?: pid=(\d+))?(.*)$", line)
            if not match:
                continue
            name, state, pid, tail = match.groups()
            services[name] = {
                "state": state,
                "pid": int(pid) if pid else None,
                "tail": tail.strip(),
                "raw": line,
            }
        return services

    def stack_action(self, service: str, action: str, stop_reason: str | None = None) -> None:
        before = self.parse_stack_status().get(service, {"state": "stopped", "pid": None})
        completed = subprocess.run(
            ["bash", str(WSL_STACK), service, action],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )
        output = "\n".join(part for part in [(completed.stdout or "").strip(), (completed.stderr or "").strip()] if part)
        if output:
            for line in output.splitlines():
                self.log(f"stack[{service}:{action}] {line}")
        if completed.returncode != 0:
            raise RuntimeError(f"stack {service} {action} failed with exit={completed.returncode}")

        after = self.parse_stack_status().get(service, {"state": "stopped", "pid": None})
        service_name = service if service != "simulator" else "watch-simulator"
        if action == "start" and before.get("state") != "running" and after.get("state") == "running":
            self.started_processes.append({"name": service_name, "id": after.get("pid"), "startedAt": now_iso()})
        if action == "stop" and before.get("state") == "running" and after.get("state") != "running":
            self.stopped_processes.append(
                {
                    "name": service_name,
                    "id": before.get("pid"),
                    "stoppedAt": now_iso(),
                    "reason": stop_reason or "manual-stop",
                }
            )

    def save_git_status(self, repo: Path, name: str) -> str:
        output_path = self.out_dir / f"git-{name}-status.log"
        completed = subprocess.run(["git", "status", "--short"], cwd=repo, text=True, capture_output=True, check=False)
        output_path.write_text(completed.stdout or completed.stderr or "", encoding="utf-8")
        return str(output_path)

    def get_readiness(self) -> dict[str, Any]:
        stack = self.parse_stack_status()
        return {
            "sql11433": self.check_port(11433),
            "redis6379": self.check_port(6379),
            "backend8080": self.check_http("http://127.0.0.1:8080/health/actuator/health"),
            "backendTcp9000": self.check_port(9000),
            "frontend9528": self.check_http("http://127.0.0.1:9528/"),
            "sqlPasswordSet": bool(self.sql_password),
            "simulatorRunning": stack.get("simulator", {}).get("state") == "running",
        }

    def invoke_sql_scalar(self, database: str, query: str) -> int:
        if not self.sql_password:
            raise RuntimeError("SQL_PASSWORD is not set")
        sqlcmd = ""
        for candidate in SQLCMD_CANDIDATES:
            candidate = candidate.strip()
            if not candidate:
                continue
            if subprocess.run(["bash", "-lc", f"command -v {shlex.quote(candidate)}"], capture_output=True).returncode == 0:
                sqlcmd = candidate
                break
        if not sqlcmd:
            raise RuntimeError("sqlcmd is not available in WSL PATH")
        completed = subprocess.run(
            [
                sqlcmd,
                "-S",
                "localhost,11433",
                "-U",
                "sa",
                "-P",
                self.sql_password,
                "-d",
                database,
                "-h",
                "-1",
                "-W",
                "-Q",
                f"SET NOCOUNT ON; {query}",
            ],
            text=True,
            capture_output=True,
            check=False,
        )
        if completed.returncode != 0:
            raise RuntimeError((completed.stderr or completed.stdout or "sqlcmd failed").strip())
        line = next((item.strip() for item in (completed.stdout or "").splitlines() if item.strip()), "0")
        return int(line)

    def get_data_source_facts(self) -> dict[str, Any]:
        sources = ["old", "new"] if self.args.data_source == "both" else [self.args.data_source]
        facts: dict[str, Any] = {}
        for source in sources:
            database = "health_new" if source == "new" else "health"
            record: dict[str, Any] = {
                "database": database,
                "ok": True,
                "error": None,
                "departmentRows": 0,
                "employeeRows": 0,
                "deviceRows": 0,
                "deviceUserRows": 0,
                "realtimeRows": 0,
                "userOnlineRows": 0,
                "simulatorDeviceRows": 0,
            }
            try:
                record["departmentRows"] = self.invoke_sql_scalar(database, "SELECT COUNT(*) FROM department")
                record["employeeRows"] = self.invoke_sql_scalar(database, "SELECT COUNT(*) FROM employee")
                record["deviceRows"] = self.invoke_sql_scalar(database, "SELECT COUNT(*) FROM device")
                record["deviceUserRows"] = self.invoke_sql_scalar(database, "SELECT COUNT(*) FROM device_user")
                record["realtimeRows"] = self.invoke_sql_scalar(database, "SELECT COUNT(*) FROM realtime_data")
                record["userOnlineRows"] = self.invoke_sql_scalar(database, "SELECT COUNT(*) FROM user_online_status")
                record["simulatorDeviceRows"] = self.invoke_sql_scalar(
                    database, "SELECT COUNT(*) FROM device WHERE imei LIKE '3594567800_____'"
                )
            except Exception as exc:  # noqa: BLE001
                record["ok"] = False
                record["error"] = str(exc)
            facts[source] = record
        return facts

    def write_combined_log(self, stdout_path: Path, stderr_path: Path, combined_path: Path) -> None:
        stdout = stdout_path.read_text(encoding="utf-8", errors="ignore") if stdout_path.exists() else ""
        stderr = stderr_path.read_text(encoding="utf-8", errors="ignore") if stderr_path.exists() else ""
        combined_path.write_text(f"### STDOUT\n{stdout}\n### STDERR\n{stderr}", encoding="utf-8")

    def invoke_step(self, name: str, command: str, working_directory: Path) -> dict[str, Any]:
        started = datetime.now().astimezone()
        self.log(f"RUN {name} :: {command}")
        stdout = self.out_dir / f"{name}.out.log"
        stderr = self.out_dir / f"{name}.err.log"
        combined = self.out_dir / f"{name}.combined.log"
        exit_code = self.run_shell(command, working_directory, stdout, stderr)
        self.write_combined_log(stdout, stderr, combined)
        finished = datetime.now().astimezone()
        combined_text = combined.read_text(encoding="utf-8", errors="ignore")
        contains_skipped = bool(
            re.search(r'"status"\s*:\s*"skipped"', combined_text)
            or re.search(r"\|\s*[^|]+\s*\|\s*skipped\s*\|", combined_text, re.IGNORECASE)
        )
        step_status = "failed" if exit_code != 0 else "skipped" if contains_skipped else "passed"
        record = {
            "name": name,
            "command": command,
            "workingDirectory": str(working_directory),
            "exitCode": exit_code,
            "status": step_status,
            "containsSkipped": contains_skipped,
            "startedAt": started.isoformat(),
            "finishedAt": finished.isoformat(),
            "durationMs": int((finished - started).total_seconds() * 1000),
            "stdout": str(stdout),
            "stderr": str(stderr),
            "combined": str(combined),
        }
        self.steps.append(record)
        self.log(f"DONE {name} exit={exit_code} combined={combined}")
        return record

    def invoke_phase(self, name: str, callback: Any) -> None:
        started = datetime.now().astimezone()
        before_index = len(self.steps)
        self.log(f"PHASE {name} start")
        callback()
        finished = datetime.now().astimezone()
        phase_steps = self.steps[before_index:]
        if any(step["exitCode"] != 0 for step in phase_steps):
            status = "failed"
        elif any(step["status"] == "skipped" for step in phase_steps):
            status = "skipped"
        else:
            status = "passed"
        self.phases.append(
            {
                "name": name,
                "status": status,
                "startedAt": started.isoformat(),
                "finishedAt": finished.isoformat(),
                "durationMs": int((finished - started).total_seconds() * 1000),
                "stepCount": len(phase_steps),
            }
        )
        self.log(f"PHASE {name} {status}")

    def write_hermes_archive(self, summary: dict[str, Any], skipped_steps: list[dict[str, Any]]) -> None:
        failed_steps = [step for step in summary["steps"] if step["exitCode"] != 0]
        lines = [
            "# Hermes Archive",
            "",
            f"- run_id: `{summary['runId']}`",
            f"- status: `{summary['status']}`",
            f"- status_reason: `{summary['statusReason']}`",
            f"- data_source: `{summary['dataSource']}`",
            "- phase statuses:",
        ]
        for phase in summary["phases"]:
            lines.append(f"  - {phase['name']}: `{phase['status']}`")
        if not summary["phases"]:
            lines.append("  - none")

        if summary["dataSourceFacts"]:
            for source, fact in summary["dataSourceFacts"].items():
                lines.append(f"- {source} dataSourceFacts:")
                for key in (
                    "database",
                    "departmentRows",
                    "employeeRows",
                    "deviceRows",
                    "deviceUserRows",
                    "realtimeRows",
                    "userOnlineRows",
                    "simulatorDeviceRows",
                ):
                    lines.append(f"  - {key}: `{fact[key]}`")
                if fact.get("error"):
                    lines.append(f"  - error: `{fact['error']}`")
        else:
            lines.append("- dataSourceFacts: `none`")

        lines.append("- simulator evidence:")
        simulator_started = [row for row in summary["startedProcesses"] if row["name"] == "watch-simulator"]
        simulator_stopped = [row for row in summary["stoppedProcesses"] if row["name"] == "watch-simulator"]
        for row in simulator_started:
            lines.append(f"  - started: `watch-simulator`, pid `{row['id']}`, `{row['startedAt']}`")
        for row in simulator_stopped:
            lines.append(
                f"  - stopped: `watch-simulator`, pid `{row['id']}`, `{row['stoppedAt']}`, reason `{row['reason']}`"
            )
        if not simulator_started and not simulator_stopped:
            lines.append("  - none")
        lines.append(f"- skipped_steps: `{', '.join(step['name'] for step in skipped_steps) if skipped_steps else 'none'}`")
        lines.append("- issue triage:")
        lines.append(f"  - failed steps: `{len(failed_steps)}`")
        lines.append(f"  - allowed skipped steps: `{len(skipped_steps)}`")
        lines.append(
            "  - action: no Health code repair required from this cycle"
            if not failed_steps
            else "  - action: hand failed steps to Codex for root-cause analysis"
        )
        self.hermes_archive_file.write_text("\n".join(lines) + "\n", encoding="utf-8")

    def write_summary_files(self, status: str, status_reason: str, readiness: dict[str, Any], git_status: dict[str, str]) -> None:
        finished_at = datetime.now().astimezone()
        skipped_steps = [step for step in self.steps if step["status"] == "skipped"]
        summary = {
            "runId": self.run_id,
            "profile": "full-stack-local",
            "dataSource": self.args.data_source,
            "status": status,
            "statusReason": status_reason,
            "startedAt": self.started_at.isoformat(),
            "finishedAt": finished_at.isoformat(),
            "durationMs": int((finished_at - self.started_at).total_seconds() * 1000),
            "outDir": str(self.out_dir),
            "logFile": str(self.log_file),
            "hermesArchive": str(self.hermes_archive_file),
            "parameters": {
                "dataSource": self.args.data_source,
                "skipBackendTests": self.args.skip_backend_tests,
                "noStartSimulatorForOld": self.args.no_start_simulator_for_old,
                "noStopSimulatorForNew": self.args.no_stop_simulator_for_new,
            },
            "environment": {
                "root": str(ROOT),
                "HealthShow": str(SHOW),
                "HealthData": str(DATA),
                "frontendUrl": "http://127.0.0.1:9528/",
                "backendUrl": "http://127.0.0.1:8080/health",
                "sqlServer": "127.0.0.1:11433",
                "redis": "127.0.0.1:6379",
                "tcp": "127.0.0.1:9000",
            },
            "readiness": readiness,
            "dataSourceFacts": self.data_source_facts,
            "gitStatus": git_status,
            "startedProcesses": self.started_processes,
            "stoppedProcesses": self.stopped_processes,
            "phases": self.phases,
            "steps": self.steps,
            "skippedSteps": skipped_steps,
        }
        self.summary_file.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        LATEST_FILE.write_text(
            json.dumps(
                {
                    "runId": self.run_id,
                    "status": status,
                    "statusReason": status_reason,
                    "dataSource": self.args.data_source,
                    "outDir": str(self.out_dir),
                    "summaryJson": str(self.summary_file),
                    "summaryMd": str(self.summary_md_file),
                    "hermesArchive": str(self.hermes_archive_file),
                    "finishedAt": finished_at.isoformat(),
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )

        failed_steps = [step for step in self.steps if step["exitCode"] != 0]
        lines = [
            "# HealthShow full stack local run",
            "",
            f"- run_id: {self.run_id}",
            f"- status: {status}",
            f"- status_reason: {status_reason}",
            f"- data_source: {self.args.data_source}",
            f"- out_dir: {self.out_dir}",
            f"- summary_json: {self.summary_file}",
            f"- hermes_archive: {self.hermes_archive_file}",
            f"- latest_pointer: {LATEST_FILE}",
            "",
            "## Readiness",
            "",
        ]
        for key, value in readiness.items():
            lines.append(f"- {key}: {value}")
        lines.extend(["", "## Data Source Facts", ""])
        if not self.data_source_facts:
            lines.append("- none")
        else:
            for source, fact in self.data_source_facts.items():
                lines.append(
                    f"- {source}: database={fact['database']}, ok={fact['ok']}, "
                    f"department={fact['departmentRows']}, employee={fact['employeeRows']}, "
                    f"device={fact['deviceRows']}, device_user={fact['deviceUserRows']}, "
                    f"realtime_data={fact['realtimeRows']}, user_online_status={fact['userOnlineRows']}, "
                    f"simulatorDeviceRows={fact['simulatorDeviceRows']}"
                )
                if fact.get("error"):
                    lines.append(f"  - error: {fact['error']}")
        lines.extend(["", "## Steps", "", "| step | status | exit | ms | log |", "| --- | --- | ---: | ---: | --- |"])
        for step in self.steps:
            lines.append(
                f"| {step['name']} | {step['status']} | {step['exitCode']} | {step['durationMs']} | "
                f"{Path(step['combined']).name} |"
            )
        if not self.steps:
            lines.append("| - | - | - | - | - |")
        lines.extend(["", "## Failed Steps", ""])
        if not failed_steps:
            lines.append("- none")
        else:
            for step in failed_steps:
                lines.append(f"- {step['name']}: exit={step['exitCode']}, log={step['combined']}")
        lines.extend(["", "## Skipped Steps", ""])
        if not skipped_steps:
            lines.append("- none")
        else:
            for step in skipped_steps:
                lines.append(f"- {step['name']}: exit={step['exitCode']}, log={step['combined']}")
        lines.extend(["", "## Git Status Logs", "", f"- HealthShow: {git_status['HealthShow']}", f"- HealthData: {git_status['HealthData']}"])
        self.summary_md_file.write_text("\n".join(lines) + "\n", encoding="utf-8")
        self.write_hermes_archive(summary, skipped_steps)

    def main(self) -> int:
        self.log(f"full stack local run start: {self.run_id}")
        self.log(f"outDir={self.out_dir}")
        self.log(f"dataSource={self.args.data_source} skipBackendTests={self.args.skip_backend_tests}")

        git_status = {
            "HealthShow": self.save_git_status(SHOW, "healthshow"),
            "HealthData": self.save_git_status(DATA, "healthdata"),
        }

        self.stack_action("backend", "start")
        self.stack_action("frontend", "start")

        readiness = self.get_readiness()
        self.log(f"READINESS {json.dumps(readiness, ensure_ascii=False)}")
        blocked_keys = [
            key
            for key in ("sql11433", "redis6379", "backend8080", "backendTcp9000", "frontend9528", "sqlPasswordSet")
            if not readiness[key]
        ]
        if blocked_keys:
            reason = f"blocked readiness: {', '.join(blocked_keys)}"
            self.log(reason)
            self.write_summary_files("blocked", reason, readiness, git_status)
            return 3

        if not self.args.skip_backend_tests:
            self.invoke_phase(
                "backend",
                lambda: (
                    self.invoke_step(
                        "backend-maven-test",
                        self.shell_env_command(f'"$HEALTH_MAVEN_CMD" -q test -f {shlex.quote(str(BACKEND_POM))}'),
                        DATA,
                    ),
                    self.invoke_step(
                        "backend-regression",
                        f"python3 {shlex.quote(str(BACKEND_REGRESSION_SCRIPT))}",
                        DATA,
                    ),
                ),
            )

        self.invoke_phase(
            "frontend-common",
            lambda: (
                self.invoke_step("test-fast", "npm run test:fast", SHOW),
                self.invoke_step("test-frontend", "npm run test:frontend", SHOW),
                self.invoke_step("test-quality", "npm run test:quality", SHOW),
            ),
        )

        if self.args.data_source in {"old", "both"}:
            def run_old_phase() -> None:
                if not self.args.no_start_simulator_for_old:
                    self.stack_action("simulator", "start")
                else:
                    self.log("SKIP simulator start for old data source because --no-start-simulator-for-old was provided")
                self.invoke_step("test-integration-old", "npm run test:integration:old", SHOW)
                self.invoke_step("test-perf-old", "npm run test:perf:old", SHOW)
                self.stack_action("simulator", "stop", stop_reason="old-full-pipeline-exclusive-tcp")
                self.invoke_step("test-full-old", "npm run test:full:old", SHOW)

            self.invoke_phase("old-data-source", run_old_phase)

        if self.args.data_source in {"new", "both"}:
            def run_new_phase() -> None:
                if not self.args.no_stop_simulator_for_new:
                    self.stack_action("simulator", "stop", stop_reason="new-data-source-phase")
                else:
                    self.log("SKIP simulator stop for new data source because --no-stop-simulator-for-new was provided")
                self.invoke_step("test-integration-new", "npm run test:integration:new", SHOW)
                self.invoke_step("test-perf-new", "npm run test:perf:new", SHOW)
                self.invoke_step("test-full-new", "npm run test:full:new", SHOW)

            self.invoke_phase("new-data-source", run_new_phase)

        readiness = self.get_readiness()
        self.data_source_facts = self.get_data_source_facts()
        failed = [step for step in self.steps if step["exitCode"] != 0]
        skipped = [step for step in self.steps if step["status"] == "skipped"]
        if failed:
            status = "failed"
            reason = f"nonzero steps: {len(failed)}"
        elif skipped:
            status = "skipped"
            reason = f"allowed skipped steps: {len(skipped)}"
        else:
            status = "passed"
            reason = "all steps passed"

        self.write_summary_files(status, reason, readiness, git_status)
        self.log(f"summary={self.summary_file}")
        self.log(f"summary_md={self.summary_md_file}")
        self.log(f"hermes_archive={self.hermes_archive_file}")

        if failed:
            self.log(f"RESULT failed steps: {len(failed)}")
            return 1
        if skipped:
            self.log(f"RESULT SKIPPED {reason}")
            return 0
        self.log("RESULT PASS all steps")
        return 0


def main() -> int:
    return Runner(parse_args()).main()


if __name__ == "__main__":
    sys.exit(main())
