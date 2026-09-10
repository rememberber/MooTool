#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -z "${JAVA_HOME:-}" ]]; then
  if /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
  fi
fi
export MOOTOOL_COMPOSE_DATA_DIR="${MOOTOOL_COMPOSE_DATA_DIR:-$ROOT/local.properties.d/dev-profile}"
mkdir -p "$MOOTOOL_COMPOSE_DATA_DIR"
cd "$ROOT"
./gradlew :composeApp:run
