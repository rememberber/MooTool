#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="${FLUTTER_SDK:-$HOME/sdk/flutter}/bin:$PATH"
cd "$ROOT"
DATA_DIR="${1:-}"
ARGS=()
if [[ -n "$DATA_DIR" ]]; then
  ARGS+=(--data-dir "$DATA_DIR")
fi
flutter run -d macos --dart-define=FLUTTER_APP_DATA="${DATA_DIR:-}" "${ARGS[@]}"
