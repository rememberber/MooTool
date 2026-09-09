#!/usr/bin/env bash
# Windows packaging. Run from Git Bash or a Windows CI runner with Flutter.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="${FLUTTER_SDK:-$HOME/sdk/flutter}/bin:$PATH"
cd "$ROOT"

if ! command -v flutter >/dev/null; then
  echo "flutter not found." >&2
  exit 1
fi

VERSION="$(python3 - <<'PY'
import pathlib, re
text = pathlib.Path("pubspec.yaml").read_text()
print(re.search(r"^version:\s*([0-9][^\s+]+)", text, re.M).group(1))
PY
)"

flutter pub get
flutter build windows --release

RELEASE_DIR=""
for candidate in \
  "build/windows/x64/runner/Release" \
  "build/windows/runner/Release"; do
  if [[ -d "$candidate" ]]; then
    RELEASE_DIR="$candidate"
    break
  fi
done
if [[ -z "$RELEASE_DIR" ]]; then
  echo "Windows Release directory not found. This script must run on Windows." >&2
  exit 1
fi
if [[ ! -f "$RELEASE_DIR/MooToolNextFlutter.exe" ]]; then
  echo "expected MooToolNextFlutter.exe in $RELEASE_DIR" >&2
  exit 1
fi

DIST="$ROOT/dist"
mkdir -p "$DIST"
PORTABLE="$DIST/MooTool-Next-Flutter-${VERSION}-win-x64-portable.zip"
rm -f "$PORTABLE"
python3 - <<PY
import pathlib, zipfile
root = pathlib.Path("$RELEASE_DIR")
out = pathlib.Path("$PORTABLE")
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zf:
    for path in root.rglob("*"):
        if path.is_file():
            zf.write(path, pathlib.Path("MooToolNextFlutter") / path.relative_to(root))
print("wrote", out)
PY

SETUP_OUT="$DIST/MooTool-Next-Flutter-${VERSION}-win-x64-setup.exe"
if command -v iscc >/dev/null; then
  iscc /DMyAppVersion="$VERSION" /O"$DIST" "$ROOT/windows/packaging/setup.iss"
else
  echo "Inno Setup (iscc) not found; portable zip was written. setup.exe is skipped." >&2
fi

python3 "$ROOT/scripts/verify-package.py" --root "$ROOT" "$PORTABLE"
if [[ -f "$SETUP_OUT" ]]; then
  python3 "$ROOT/scripts/verify-package.py" --root "$ROOT" "$SETUP_OUT"
fi
echo "unsigned windows packages under $DIST"
