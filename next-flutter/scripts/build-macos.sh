#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="${FLUTTER_SDK:-$HOME/sdk/flutter}/bin:$PATH"
cd "$ROOT"

if ! command -v flutter >/dev/null; then
  echo "flutter not found. Install the SDK or set FLUTTER_SDK." >&2
  exit 1
fi

# Prefer a full Xcode.app even when xcode-select still points at Command Line Tools.
if [[ -z "${DEVELOPER_DIR:-}" ]]; then
  if [[ -d /Applications/Xcode.app/Contents/Developer ]]; then
    export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
  elif [[ -d "${HOME}/Applications/Xcode.app/Contents/Developer" ]]; then
    export DEVELOPER_DIR="${HOME}/Applications/Xcode.app/Contents/Developer"
  fi
fi
if [[ -n "${DEVELOPER_DIR:-}" ]]; then
  export PATH="${DEVELOPER_DIR}/usr/bin:$PATH"
fi

if ! command -v xcodebuild >/dev/null; then
  echo "xcodebuild not found." >&2
  exit 1
fi
if ! xcodebuild -version >/dev/null 2>&1; then
  echo "Xcode is installed but not ready. Agree to the license and finish first launch:" >&2
  echo "  sudo xcodebuild -license accept" >&2
  echo "  sudo xcodebuild -runFirstLaunch" >&2
  echo "Active developer dir: $(xcode-select -p 2>/dev/null || true)" >&2
  echo "DEVELOPER_DIR=${DEVELOPER_DIR:-unset}" >&2
  xcodebuild -version >&2 || true
  exit 1
fi

VERSION="$(python3 - <<'PY'
import pathlib, re
text = pathlib.Path("pubspec.yaml").read_text()
print(re.search(r"^version:\s*([0-9][^\s+]+)", text, re.M).group(1))
PY
)"
ARCH_RAW="$(uname -m)"
case "$ARCH_RAW" in
  arm64) ARCH=arm64 ;;
  x86_64) ARCH=x64 ;;
  *) echo "unsupported arch: $ARCH_RAW" >&2; exit 1 ;;
esac

# Unsigned on purpose. Do not discover Developer ID certificates.
export CODE_SIGNING_ALLOWED=NO
export CODE_SIGNING_REQUIRED=NO
export CODE_SIGN_IDENTITY=""
unset CSC_LINK CSC_NAME CSC_IDENTITY_AUTO_DISCOVERY || true
export CSC_IDENTITY_AUTO_DISCOVERY=false

flutter pub get
flutter build macos --release

APP="build/macos/Build/Products/Release/MooTool Next Flutter.app"
if [[ ! -d "$APP" ]]; then
  echo "expected app bundle missing: $APP" >&2
  exit 1
fi

DIST="$ROOT/dist"
STAGE="$DIST/dmg-stage"
rm -rf "$STAGE"
mkdir -p "$STAGE"
cp -R "$APP" "$STAGE/"
ln -sf /Applications "$STAGE/Applications"

OUT="$DIST/MooTool-Next-Flutter-${VERSION}-mac-${ARCH}.dmg"
rm -f "$OUT"
hdiutil create \
  -volname "MooTool Next Flutter" \
  -srcfolder "$STAGE" \
  -ov -format UDZO \
  "$OUT"

python3 "$ROOT/scripts/verify-package.py" --root "$ROOT" "$OUT"
echo "unsigned dmg: $OUT"
echo "Gatekeeper will warn; users must open the app from Finder. This product does not auto-install."
