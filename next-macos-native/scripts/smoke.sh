#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/check-toolchain.sh
for native_option in "$@"; do
  case "$native_option" in --window-capture|--notes-only|--note-layouts-only) ;; *) echo 'Usage: scripts/smoke.sh [--window-capture] [--notes-only|--note-layouts-only]' >&2; exit 2 ;; esac
done
swift build
native_bin=$(swift build --show-bin-path)
native_test_data=$(mktemp -d /tmp/mootool-native-test.XXXXXX)
trap 'rm -rf "$native_test_data"' EXIT
export MOOTOOL_NATIVE_TEST_DATA="$native_test_data"
export MOOTOOL_NATIVE_SCREENSHOTS="$PWD/dist/acceptance"
"$native_bin/MooToolNextNative" --smoke-test "$@"
"$native_bin/MooToolNextNative" --verify-workspace
