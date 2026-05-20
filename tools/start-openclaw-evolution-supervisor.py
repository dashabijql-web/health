#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import time
from datetime import datetime, timedelta
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TESTS_RUNS = ROOT / "tests" / "runs"
STOP_AFTER_CURRENT_REQUESTED = "STOP_AFTER_CURRENT_REQUESTED"
ROUND_STOP_AFTER_CURRENT = "ROUND_STOP_AFTER_CURRENT"
ROUND_RESULT_JSON = "ROUND_RESULT_JSON"
PROCESS_RETROSPECTIVE_FIELD = "process_retrospective"
FLOW_GUARD_ADDED_FIELD = "flow_guard_added"
DEFAULT_STOP_FILE = TESTS_RUNS / "openclaw-night-supervisor.stop"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--stop-after-current-file", default=str(DEFAULT_STOP_FILE))
    parser.add_argument("--runner-timeout-minutes", type=int, default=90)
    parser.add_argument("--stop-at", default="")
    parser.add_argument("--round-limit", type=int, default=1)
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def test_stop_after_current_requested(path: Path) -> bool:
    return path.exists() and STOP_AFTER_CURRENT_REQUESTED in path.read_text(encoding="utf-8", errors="ignore")


def wait_full_stack_runner_idle(timeout_minutes: int) -> None:
    deadline = time.time() + timeout_minutes * 60
    while time.time() < deadline:
        marker = ROOT / "HealthShow" / "tests" / "runs" / "latest-full-stack-local.json"
        if marker.exists():
            return
        time.sleep(1)


def valid_round_result_json(payload_text: str) -> bool:
    if ROUND_RESULT_JSON not in payload_text:
        return False
    try:
        payload = json.loads(payload_text.split(ROUND_RESULT_JSON, 1)[1].strip())
    except Exception:  # noqa: BLE001
        return False
    return PROCESS_RETROSPECTIVE_FIELD in payload and FLOW_GUARD_ADDED_FIELD in payload


def write_final_report(report_path: Path, stop_reason: str) -> None:
    report_path.write_text(f"stop_reason: {stop_reason}\n", encoding="utf-8")


def main() -> int:
    args = parse_args()
    stop_file = Path(args.stop_after_current_file)
    if args.dry_run:
        print(
            json.dumps(
                {
                    "status": "DRY_RUN",
                    "stopFile": str(stop_file),
                    "runnerTimeoutMinutes": args.runner_timeout_minutes,
                    "roundResultMarker": ROUND_RESULT_JSON,
                },
                ensure_ascii=False,
            )
        )
        return 0

    TESTS_RUNS.mkdir(parents=True, exist_ok=True)
    run_id = f"openclaw-night-supervisor-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
    run_dir = TESTS_RUNS / run_id
    run_dir.mkdir(parents=True, exist_ok=True)
    report_path = run_dir / "final-report.md"
    stop_reason = "round_limit_reached"

    for round_index in range(1, args.round_limit + 1):
      wait_full_stack_runner_idle(args.runner_timeout_minutes)
      if test_stop_after_current_requested(stop_file):
          stop_reason = "stop_after_current_requested"
          (run_dir / "events.log").write_text(
              f"{STOP_AFTER_CURRENT_REQUESTED}\n{ROUND_STOP_AFTER_CURRENT} round={round_index}\n",
              encoding="utf-8",
          )
          break

      if args.stop_at:
          stop_at = datetime.fromisoformat(args.stop_at)
          if datetime.now(stop_at.tzinfo) >= stop_at:
              stop_reason = "stop_at_reached"
              break

      prompt = {
          "required_markers": [ROUND_RESULT_JSON],
          PROCESS_RETROSPECTIVE_FIELD: "Required",
          FLOW_GUARD_ADDED_FIELD: False,
      }
      (run_dir / f"round-{round_index:02d}-prompt.json").write_text(json.dumps(prompt, ensure_ascii=False, indent=2), encoding="utf-8")
      stop_reason = "round_limit_reached"

    write_final_report(report_path, stop_reason)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
