#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/health-wsl-env.sh"

cd "$HEALTH_HOME/HealthShow"
export HEALTH_SIM_SERVER_HOST="${HEALTH_SIM_SERVER_HOST:-127.0.0.1}"
export HEALTH_SIM_SERVER_PORT="${HEALTH_SIM_SERVER_PORT:-9000}"
export HEALTH_SIM_START_ID="${HEALTH_SIM_START_ID:-1}"
export HEALTH_SIM_TOTAL_WATCHES="${HEALTH_SIM_TOTAL_WATCHES:-1000}"

exec "$HEALTH_PYTHON_CMD" watch_tcp_simulator_1000.py "$HEALTH_SIM_START_ID" "$HEALTH_SIM_TOTAL_WATCHES"
