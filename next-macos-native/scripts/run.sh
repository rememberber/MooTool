#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/build-app.sh --debug
open "dist/$(uname -m)/MooTool Next Native.app"
