#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:-}"
case "$TARGET" in
  macos) exec "$ROOT/scripts/build-macos.sh" ;;
  windows) exec "$ROOT/scripts/build-windows.sh" ;;
  linux) exec "$ROOT/scripts/build-linux.sh" ;;
  *)
    echo "usage: $0 macos|windows|linux" >&2
    echo "Packages are unsigned. Missing platform toolchains fail instead of faking an installer." >&2
    exit 1
    ;;
esac
