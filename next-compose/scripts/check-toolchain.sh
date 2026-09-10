#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -z "${JAVA_HOME:-}" ]]; then
  if /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
  fi
fi
echo "JAVA_HOME=${JAVA_HOME:-unset}"
java -version
cd "$ROOT"
./gradlew --version
./gradlew :composeApp:printTooling
