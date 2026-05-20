#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_STOP_FILE = ROOT / "tests" / "runs" / "openclaw-night-supervisor.stop"
STOP_AFTER_CURRENT_REQUESTED = "STOP_AFTER_CURRENT_REQUESTED"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--stop-file", default=str(DEFAULT_STOP_FILE))
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    stop_file = Path(args.stop_file)
    if args.dry_run:
        print(json.dumps({"status": "DRY_RUN", "stopFile": str(stop_file)}, ensure_ascii=False))
        return 0

    stop_file.parent.mkdir(parents=True, exist_ok=True)
    stop_file.write_text(f"{STOP_AFTER_CURRENT_REQUESTED} {datetime.now().astimezone().isoformat(timespec='seconds')}\n", encoding="utf-8")
    print(f"WROTE {stop_file}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
