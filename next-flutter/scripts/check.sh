#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="${FLUTTER_SDK:-$HOME/sdk/flutter}/bin:$PATH"
cd "$ROOT"
if ! command -v flutter >/dev/null; then
  echo "flutter not found. Install the SDK or set FLUTTER_SDK." >&2
  exit 1
fi
flutter pub get
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test test/unit
