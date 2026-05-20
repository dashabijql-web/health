#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import shlex
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SHOW = ROOT / "HealthShow"
DEFAULT_RUNNER = SHOW / "tests/run-full-stack-local.py"
SESSION_NAME = "health-full-stack-runner"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-source", choices=["old", "new", "both"], default="old")
    parser.add_argument("--runner-path", default=str(DEFAULT_RUNNER))
    parser.add_argument("--working-directory", default=str(SHOW))
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def tmux_session_exists() -> bool:
    return subprocess.run(["tmux", "has-session", "-t", SESSION_NAME], capture_output=True, check=False).returncode == 0


def tmux_pane_pid() -> int | None:
    completed = subprocess.run(
        ["tmux", "display-message", "-p", "-t", SESSION_NAME, "#{pane_pid}"],
        text=True,
        capture_output=True,
        check=False,
    )
    if completed.returncode != 0:
        return None
    value = (completed.stdout or "").strip()
    return int(value) if value.isdigit() else None


def main() -> int:
    args = parse_args()
    runner_path = Path(args.runner_path).resolve()
    working_directory = Path(args.working_directory).resolve()
    if not runner_path.exists():
        raise SystemExit(f"Runner not found: {runner_path}")
    if not working_directory.exists():
        raise SystemExit(f"Working directory not found: {working_directory}")

    arguments = [str(runner_path), "--data-source", args.data_source]
    if args.dry_run:
        print(
            json.dumps(
                {
                    "status": "DRY_RUN",
                    "filePath": sys.executable,
                    "arguments": arguments,
                    "workingDirectory": str(working_directory),
                },
                ensure_ascii=False,
            )
        )
        return 0

    if tmux_session_exists():
        pid = tmux_pane_pid()
        print(f"RUN_ALREADY_ACTIVE pid={pid or 'unknown'} dataSource={args.data_source}")
        return 0

    command = (
        f"cd {shlex.quote(str(working_directory))} && "
        f"exec {shlex.quote(sys.executable)} {shlex.quote(str(runner_path))} "
        f"--data-source {shlex.quote(args.data_source)}"
    )
    subprocess.run(["tmux", "new-session", "-d", "-s", SESSION_NAME, command], check=True)
    pid = tmux_pane_pid()
    print(f"RUN_STARTED pid={pid or 'unknown'} dataSource={args.data_source}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
