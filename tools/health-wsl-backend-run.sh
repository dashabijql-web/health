#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/health-wsl-env.sh"

cd "$HEALTH_HOME/HealthData"
export SERVER_PORT="${SERVER_PORT:-8080}"
export NETTY_SERVER_PORT="${NETTY_SERVER_PORT:-9000}"

CLASSPATH_FILE="${HEALTH_BACKEND_CLASSPATH_FILE:-$HEALTH_HOME/HealthData/target/runtime-classpath.txt}"
MAIN_CLASS="${HEALTH_BACKEND_MAIN_CLASS:-com.xzkj.health.HealthApplication}"

if [[ -s "$CLASSPATH_FILE" ]]; then
  RUNTIME_CP="$(cat "$CLASSPATH_FILE")"
  exec "$JAVA_HOME/bin/java" -cp "$HEALTH_HOME/HealthData/target/classes:$RUNTIME_CP" "$MAIN_CLASS"
fi

exec "$HEALTH_MAVEN_CMD" -Dmaven.repo.local="$HEALTH_MAVEN_REPO_LOCAL" spring-boot:run
