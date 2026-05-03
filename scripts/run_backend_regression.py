#!/usr/bin/env python3
"""Run the backend regression guardrails that are safe on a live local environment."""

from __future__ import annotations

from pathlib import Path
import os
import shutil
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[1]
MAVEN_BIN = os.getenv("MAVEN_BIN") or shutil.which("mvn.cmd") or shutil.which("mvn")
SCRIPTS = [
    ("compile", [MAVEN_BIN, "-q", "-DskipTests", "compile"] if MAVEN_BIN else None),
    ("field_sync", ROOT / "scripts" / "check_health_field_sync.py"),
    ("redis_buffer_probe", ROOT / "scripts" / "probe_redis_buffer_flush.py"),
]


def run_step(label: str, command: list[str]) -> int:
    print(f"== {label} ==")
    completed = subprocess.run(command, cwd=ROOT)
    print()
    return completed.returncode


def main() -> int:
    failures: list[str] = []
    for label, target in SCRIPTS:
        if target is None:
            failures.append(f"{label} (maven executable not found)")
            continue
        if isinstance(target, Path):
            command = [sys.executable, str(target)]
        else:
            command = target
        code = run_step(label, command)
        if code != 0:
            failures.append(f"{label} (exit {code})")

    if failures:
        print("Backend regression summary: FAILED")
        for failure in failures:
            print(f"  - {failure}")
        return 1

    print("Backend regression summary: PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
