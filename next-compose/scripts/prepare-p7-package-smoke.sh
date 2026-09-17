#!/usr/bin/env bash
# P7 打包前本机烟雾检查（不代替三平台安装/升级/卸载验收）。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "# next-compose P7 package smoke ($(date -u +%Y-%m-%dT%H:%MZ))"
echo "# Run from: $ROOT"
echo ""

if [[ -x "$ROOT/scripts/check-toolchain.sh" ]]; then
  "$ROOT/scripts/check-toolchain.sh"
else
  echo "WARN: scripts/check-toolchain.sh missing"
fi

export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null || true)}"
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "ERROR: set JAVA_HOME to JDK 21"
  exit 1
fi

echo ""
echo "## Gradle tooling"
./gradlew :composeApp:printTooling --offline

echo ""
echo "## Unit/desktop tests (offline)"
./gradlew :composeApp:desktopTest --offline

echo ""
echo "## Current OS distributable (optional; may take several minutes)"
echo "# Uncomment to build install tree on this machine:"
echo "# ./gradlew :composeApp:packageDistributionForCurrentOS --offline"
echo "# ./scripts/rename-dist-artifacts.sh <semver>"
echo ""
echo "OK: smoke checks finished. Record results in docs/acceptance.md; P7 install/sign/notarize still manual."
