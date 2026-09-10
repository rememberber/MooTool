#!/bin/bash
set -euo pipefail
python3 - <<'PY'
import re, subprocess, sys
sdk = subprocess.check_output(['xcrun', '--sdk', 'macosx', '--show-sdk-version'], text=True).strip()
swift = subprocess.check_output(['swift', '--version'], text=True)
match = re.search(r'Swift version (\d+)\.(\d+)', swift)
if int(sdk.split('.')[0]) < 26 or not match or tuple(map(int, match.groups())) < (6, 2):
    sys.exit('Building MooTool Next Native requires macOS SDK 26 and Swift 6.2+. Select Xcode 26 or Command Line Tools 26. The built app still supports macOS 14+.')
PY
