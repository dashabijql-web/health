#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/health-wsl-env.sh"

cd "$HEALTH_HOME/HealthShow"
export FRONTEND_PORT="${FRONTEND_PORT:-9528}"
export VITE_TARGET="${VITE_TARGET:-http://localhost:8080}"

exec "$HEALTH_NPM_CMD" run dev -- --host 0.0.0.0 --port "$FRONTEND_PORT"
