#!/usr/bin/env bash
set -euo pipefail

HEALTH_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

resolve_java_home() {
  local preferred="${HEALTH_JAVA_HOME:-${JAVA_HOME:-$HOME/.local/opt/jdk-17.0.19+10}}"
  local java_bin
  if [[ -x "$preferred/bin/java" ]]; then
    printf '%s\n' "$preferred"
    return 0
  fi

  java_bin="$(command -v java || true)"
  if [[ -n "$java_bin" ]]; then
    java_bin="$(readlink -f "$java_bin")"
    printf '%s\n' "$(cd "$(dirname "$java_bin")/.." && pwd)"
    return 0
  fi

  return 1
}

resolve_maven_home() {
  local preferred="${HEALTH_MAVEN_HOME:-${MAVEN_HOME:-$HOME/.local/opt/apache-maven-3.9.16}}"
  local mvn_bin
  if [[ -x "$preferred/bin/mvn" ]]; then
    printf '%s\n' "$preferred"
    return 0
  fi

  mvn_bin="$(command -v mvn || true)"
  if [[ -n "$mvn_bin" ]]; then
    mvn_bin="$(readlink -f "$mvn_bin")"
    printf '%s\n' "$(cd "$(dirname "$mvn_bin")/.." && pwd)"
    return 0
  fi

  return 1
}

require_cmd_path() {
  local description="$1"
  local path_value="$2"
  if [[ -z "$path_value" || ! -x "$path_value" ]]; then
    echo "Missing ${description}: ${path_value:-<empty>}" >&2
    exit 1
  fi
}

JAVA_HOME="$(resolve_java_home || true)"
MAVEN_HOME="$(resolve_maven_home || true)"
HEALTH_PYTHON_CMD="${HEALTH_PYTHON_CMD:-$(command -v python3 || true)}"
HEALTH_NODE_CMD="${HEALTH_NODE_CMD:-$(command -v node || true)}"
HEALTH_NPM_CMD="${HEALTH_NPM_CMD:-$(command -v npm || true)}"

require_cmd_path "Java runtime" "${JAVA_HOME:+$JAVA_HOME/bin/java}"
require_cmd_path "Maven runtime" "${MAVEN_HOME:+$MAVEN_HOME/bin/mvn}"
require_cmd_path "Python runtime" "$HEALTH_PYTHON_CMD"
require_cmd_path "Node runtime" "$HEALTH_NODE_CMD"
require_cmd_path "npm runtime" "$HEALTH_NPM_CMD"

if [[ -d "/mnt/c/Users/j/.m2/repository" ]]; then
  HEALTH_MAVEN_REPO_LOCAL="${HEALTH_MAVEN_REPO_LOCAL:-/mnt/c/Users/j/.m2/repository}"
else
  HEALTH_MAVEN_REPO_LOCAL="${HEALTH_MAVEN_REPO_LOCAL:-$HOME/.m2/repository}"
fi

export HEALTH_HOME
export JAVA_HOME
export MAVEN_HOME
export HEALTH_MAVEN_CMD="$MAVEN_HOME/bin/mvn"
export HEALTH_MAVEN_REPO_LOCAL
export HEALTH_PYTHON_CMD
export HEALTH_NODE_CMD
export HEALTH_NPM_CMD
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
