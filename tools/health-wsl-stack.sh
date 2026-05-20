#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/health-wsl-env.sh"

LOG_ROOT="${HEALTH_STACK_LOG_ROOT:-$HEALTH_HOME/runtime-logs/wsl-stack}"
STACK_LAUNCHER="${HEALTH_STACK_LAUNCHER:-auto}"
STACK_SESSION_PREFIX="${HEALTH_STACK_SESSION_PREFIX:-health-wsl}"
mkdir -p "$LOG_ROOT"

BACKEND_RUNNER="$SCRIPT_DIR/health-wsl-backend-run.sh"
FRONTEND_RUNNER="$SCRIPT_DIR/health-wsl-frontend-run.sh"
SIMULATOR_RUNNER="$SCRIPT_DIR/health-wsl-simulator-run.sh"

BACKEND_HTTP_PORT="${BACKEND_HTTP_PORT:-8080}"
BACKEND_TCP_PORT="${BACKEND_TCP_PORT:-9000}"
FRONTEND_PORT="${FRONTEND_PORT:-9528}"
SIM_START_ID="${SIM_START_ID:-1}"
SIM_TOTAL_WATCHES="${SIM_TOTAL_WATCHES:-1000}"
SIM_SERVER_HOST="${SIM_SERVER_HOST:-127.0.0.1}"
SIM_SERVER_PORT="${SIM_SERVER_PORT:-$BACKEND_TCP_PORT}"

backend_pid_file="$LOG_ROOT/backend.pid"
frontend_pid_file="$LOG_ROOT/frontend.pid"
sim_pid_file="$LOG_ROOT/simulator.pid"

usage() {
  cat <<'EOF'
Usage:
  health-wsl-stack.sh status
  health-wsl-stack.sh backend start|stop|restart|status
  health-wsl-stack.sh frontend start|stop|restart|status
  health-wsl-stack.sh simulator start|stop|restart|status
  health-wsl-stack.sh all start|stop|restart|status
EOF
}

service_session_name() {
  printf '%s-%s\n' "$STACK_SESSION_PREFIX" "$1"
}

use_tmux_launcher() {
  case "$STACK_LAUNCHER" in
    tmux)
      command -v tmux >/dev/null 2>&1
      ;;
    auto)
      command -v tmux >/dev/null 2>&1
      ;;
    *)
      return 1
      ;;
  esac
}

command_line_from_args() {
  local quoted=()
  local arg
  for arg in "$@"; do
    quoted+=("$(printf '%q' "$arg")")
  done
  printf '%s\n' "${quoted[*]}"
}

read_pid() {
  local pid_file="$1"
  if [[ -f "$pid_file" ]]; then
    tr -d '[:space:]' <"$pid_file"
  fi
}

clear_stale_pid_file() {
  local pid_file="$1"
  local service_name="${2:-}"
  if [[ -n "$service_name" ]] && use_tmux_launcher; then
    if tmux has-session -t "$(service_session_name "$service_name")" 2>/dev/null; then
      return 0
    fi
  fi
  local pid
  pid="$(read_pid "$pid_file")"
  if [[ -n "$pid" ]] && ! kill -0 "$pid" 2>/dev/null; then
    rm -f "$pid_file"
  fi
}

is_running() {
  local pid_file="$1"
  local service_name="${2:-}"
  clear_stale_pid_file "$pid_file" "$service_name"
  if [[ -n "$service_name" ]] && use_tmux_launcher; then
    tmux has-session -t "$(service_session_name "$service_name")" 2>/dev/null
    return $?
  fi
  local pid
  pid="$(read_pid "$pid_file")"
  [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

current_pid() {
  local pid_file="$1"
  local service_name="${2:-}"
  if [[ -n "$service_name" ]] && use_tmux_launcher; then
    if tmux has-session -t "$(service_session_name "$service_name")" 2>/dev/null; then
      tmux display-message -p -t "$(service_session_name "$service_name")" '#{pane_pid}'
      return 0
    fi
  fi
  read_pid "$pid_file"
}

stop_service() {
  local pid_file="$1"
  local label="$2"
  local service_name="${3:-$label}"
  if use_tmux_launcher; then
    local session_name
    session_name="$(service_session_name "$service_name")"
    if tmux has-session -t "$session_name" 2>/dev/null; then
      tmux kill-session -t "$session_name"
      rm -f "$pid_file"
      echo "$label stopped"
      return 0
    fi
    rm -f "$pid_file"
    echo "$label not running"
    return 0
  fi

  local pid
  pid="$(read_pid "$pid_file")"
  if [[ -z "$pid" ]]; then
    rm -f "$pid_file"
    echo "$label not running"
    return 0
  fi

  kill "$pid" 2>/dev/null || true
  for _ in {1..20}; do
    if ! kill -0 "$pid" 2>/dev/null; then
      rm -f "$pid_file"
      echo "$label stopped"
      return 0
    fi
    sleep 1
  done

  kill -9 "$pid" 2>/dev/null || true
  rm -f "$pid_file"
  echo "$label stopped (forced)"
}

start_with_pid_file() {
  local pid_file="$1"
  local log_file="$2"
  local service_name="$3"
  shift 3

  if use_tmux_launcher; then
    local session_name
    local command_line
    local log_file_quoted
    local pane_pid

    session_name="$(service_session_name "$service_name")"
    command_line="$(command_line_from_args "$@")"
    log_file_quoted="$(printf '%q' "$log_file")"
    rm -f "$log_file"
    tmux new-session -d -s "$session_name" "exec ${command_line} >${log_file_quoted} 2>&1"
    sleep 1
    pane_pid="$(tmux display-message -p -t "$session_name" '#{pane_pid}')"
    if [[ -n "$pane_pid" ]]; then
      echo "$pane_pid" >"$pid_file"
    fi
    return 0
  fi

  nohup "$@" </dev/null >"$log_file" 2>&1 &
  echo $! >"$pid_file"
}

wait_for_http() {
  local url="$1"
  local timeout="${2:-120}"
  local i
  for ((i=0; i<timeout; i++)); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

wait_for_tcp() {
  local host="$1"
  local port="$2"
  local timeout="${3:-120}"
  "$HEALTH_PYTHON_CMD" - "$host" "$port" "$timeout" <<'PY'
import socket, sys, time
host = sys.argv[1]
port = int(sys.argv[2])
timeout = int(sys.argv[3])
end = time.time() + timeout
while time.time() < end:
    s = socket.socket()
    s.settimeout(1)
    try:
        s.connect((host, port))
        print("ok")
        sys.exit(0)
    except OSError:
        time.sleep(1)
    finally:
        s.close()
sys.exit(1)
PY
}

start_backend() {
  if is_running "$backend_pid_file" "backend"; then
    echo "backend already running pid=$(current_pid "$backend_pid_file" "backend")"
    return 0
  fi
  local log_file="$LOG_ROOT/backend.log"
  start_with_pid_file "$backend_pid_file" "$log_file" "backend" \
    env SERVER_PORT="$BACKEND_HTTP_PORT" NETTY_SERVER_PORT="$BACKEND_TCP_PORT" "$BACKEND_RUNNER"
  wait_for_http "http://127.0.0.1:${BACKEND_HTTP_PORT}/health/actuator/health" 180
  wait_for_tcp 127.0.0.1 "$BACKEND_TCP_PORT" 120 >/dev/null
  echo "backend started http=$BACKEND_HTTP_PORT tcp=$BACKEND_TCP_PORT pid=$(current_pid "$backend_pid_file" "backend")"
}

stop_backend() {
  stop_service "$backend_pid_file" "backend" "backend"
}

restart_backend() {
  stop_backend
  start_backend
}

backend_status() {
  if is_running "$backend_pid_file" "backend"; then
    echo "backend running pid=$(current_pid "$backend_pid_file" "backend") http=$BACKEND_HTTP_PORT tcp=$BACKEND_TCP_PORT"
  else
    echo "backend stopped"
  fi
}

start_frontend() {
  if is_running "$frontend_pid_file" "frontend"; then
    echo "frontend already running pid=$(current_pid "$frontend_pid_file" "frontend")"
    return 0
  fi
  local log_file="$LOG_ROOT/frontend.log"
  start_with_pid_file "$frontend_pid_file" "$log_file" "frontend" \
    env VITE_TARGET="http://localhost:${BACKEND_HTTP_PORT}" FRONTEND_PORT="$FRONTEND_PORT" "$FRONTEND_RUNNER"
  wait_for_http "http://127.0.0.1:${FRONTEND_PORT}/" 120
  echo "frontend started port=$FRONTEND_PORT pid=$(current_pid "$frontend_pid_file" "frontend")"
}

stop_frontend() {
  stop_service "$frontend_pid_file" "frontend" "frontend"
}

restart_frontend() {
  stop_frontend
  start_frontend
}

frontend_status() {
  if is_running "$frontend_pid_file" "frontend"; then
    echo "frontend running pid=$(current_pid "$frontend_pid_file" "frontend") port=$FRONTEND_PORT"
  else
    echo "frontend stopped"
  fi
}

start_simulator() {
  if is_running "$sim_pid_file" "simulator"; then
    echo "simulator already running pid=$(current_pid "$sim_pid_file" "simulator")"
    return 0
  fi
  local log_file="$LOG_ROOT/simulator.log"
  start_with_pid_file "$sim_pid_file" "$log_file" "simulator" \
    env \
      HEALTH_SIM_SERVER_HOST="$SIM_SERVER_HOST" \
      HEALTH_SIM_SERVER_PORT="$SIM_SERVER_PORT" \
      HEALTH_SIM_START_ID="$SIM_START_ID" \
      HEALTH_SIM_TOTAL_WATCHES="$SIM_TOTAL_WATCHES" \
      "$SIMULATOR_RUNNER"
  sleep 2
  echo "simulator started pid=$(current_pid "$sim_pid_file" "simulator") host=$SIM_SERVER_HOST port=$SIM_SERVER_PORT watches=$SIM_TOTAL_WATCHES"
}

stop_simulator() {
  stop_service "$sim_pid_file" "simulator" "simulator"
}

restart_simulator() {
  stop_simulator
  start_simulator
}

simulator_status() {
  if is_running "$sim_pid_file" "simulator"; then
    echo "simulator running pid=$(current_pid "$sim_pid_file" "simulator") host=$SIM_SERVER_HOST port=$SIM_SERVER_PORT watches=$SIM_TOTAL_WATCHES"
  else
    echo "simulator stopped"
  fi
}

start_all() {
  start_backend
  start_frontend
  start_simulator
}

stop_all() {
  stop_simulator
  stop_frontend
  stop_backend
}

restart_all() {
  stop_all
  start_all
}

all_status() {
  backend_status
  frontend_status
  simulator_status
}

case "${1:-status}" in
  status)
    all_status
    ;;
  backend)
    case "${2:-status}" in
      start) start_backend ;;
      stop) stop_backend ;;
      restart) restart_backend ;;
      status) backend_status ;;
      *) usage; exit 1 ;;
    esac
    ;;
  frontend)
    case "${2:-status}" in
      start) start_frontend ;;
      stop) stop_frontend ;;
      restart) restart_frontend ;;
      status) frontend_status ;;
      *) usage; exit 1 ;;
    esac
    ;;
  simulator)
    case "${2:-status}" in
      start) start_simulator ;;
      stop) stop_simulator ;;
      restart) restart_simulator ;;
      status) simulator_status ;;
      *) usage; exit 1 ;;
    esac
    ;;
  all)
    case "${2:-status}" in
      start) start_all ;;
      stop) stop_all ;;
      restart) restart_all ;;
      status) all_status ;;
      *) usage; exit 1 ;;
    esac
    ;;
  *)
    usage
    exit 1
    ;;
esac
