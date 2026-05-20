#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/health-wsl-env.sh"

PID="${1:-}"
if [[ -z "$PID" ]]; then
  PID="$(pgrep -f 'com.xzkj.health.HealthApplication' | head -n 1 || true)"
fi

if [[ -z "$PID" ]]; then
  echo "No running HealthApplication process found." >&2
  exit 1
fi

"$HEALTH_PYTHON_CMD" - "$PID" "$HEALTH_HOME" <<'PY'
from pathlib import Path
import sys

pid = sys.argv[1]
health_home = sys.argv[2]
args = Path(f'/proc/{pid}/cmdline').read_bytes().split(b'\0')
args = [arg.decode() for arg in args if arg]
classpath = ''
for index, arg in enumerate(args):
    if arg == '-cp' and index + 1 < len(args):
        classpath = args[index + 1]
        break

if not classpath:
    raise SystemExit('Classpath not found in process command line.')

prefix = f'{health_home}/HealthData/target/classes:'
if classpath.startswith(prefix):
    classpath = classpath[len(prefix):]

output = Path(f'{health_home}/HealthData/target/runtime-classpath.txt')
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(classpath, encoding='utf-8')
print(output)
print(f'entries={len(classpath.split(":"))}')
PY
