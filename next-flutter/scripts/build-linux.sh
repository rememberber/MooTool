#!/usr/bin/env bash
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
flutter build linux --release

BUNDLE=""
for candidate in \
  "build/linux/x64/release/bundle" \
  "build/linux/release/bundle"; do
  if [[ -d "$candidate" ]]; then
    BUNDLE="$candidate"
    break
  fi
done
if [[ -z "$BUNDLE" ]]; then
  echo "Linux bundle not found. This script must run on Linux." >&2
  exit 1
fi
if [[ ! -x "$BUNDLE/mootool-next-flutter" ]]; then
  echo "expected linux executable mootool-next-flutter" >&2
  exit 1
fi

DIST="$ROOT/dist"
mkdir -p "$DIST"

DEB_ROOT="$DIST/deb"
rm -rf "$DEB_ROOT"
mkdir -p "$DEB_ROOT/DEBIAN" \
  "$DEB_ROOT/usr/lib/mootool-next-flutter" \
  "$DEB_ROOT/usr/share/applications" \
  "$DEB_ROOT/usr/bin"
cp -a "$BUNDLE/." "$DEB_ROOT/usr/lib/mootool-next-flutter/"
cat > "$DEB_ROOT/usr/bin/mootool-next-flutter" <<'EOF'
#!/bin/sh
exec /usr/lib/mootool-next-flutter/mootool-next-flutter "$@"
EOF
chmod 755 "$DEB_ROOT/usr/bin/mootool-next-flutter"
cp "$ROOT/linux/packaging/com.rememberber.mootool.next.flutter.desktop" \
  "$DEB_ROOT/usr/share/applications/"
SIZE_KB="$(du -sk "$DEB_ROOT/usr" | awk '{print $1}')"
cat > "$DEB_ROOT/DEBIAN/control" <<EOF
Package: mootool-next-flutter
Version: $VERSION
Section: utils
Priority: optional
Architecture: amd64
Installed-Size: $SIZE_KB
Maintainer: MooTool Next Flutter <noreply@rememberber.com>
Description: Independent MooTool Next Flutter desktop toolbox
EOF
DEB_OUT="$DIST/MooTool-Next-Flutter-${VERSION}-linux-x64.deb"
dpkg-deb --build --root-owner-group "$DEB_ROOT" "$DEB_OUT"

APPIMAGE_OUT="$DIST/MooTool-Next-Flutter-${VERSION}-linux-x64.AppImage"
if command -v appimagetool >/dev/null; then
  APPDIR="$DIST/AppDir"
  rm -rf "$APPDIR"
  mkdir -p "$APPDIR/usr/bin" "$APPDIR/usr/lib" "$APPDIR/usr/share/applications"
  cp -a "$BUNDLE/." "$APPDIR/usr/lib/mootool-next-flutter"
  ln -sf usr/lib/mootool-next-flutter/mootool-next-flutter "$APPDIR/AppRun"
  cp "$ROOT/linux/packaging/com.rememberber.mootool.next.flutter.desktop" \
    "$APPDIR/usr/share/applications/"
  appimagetool "$APPDIR" "$APPIMAGE_OUT"
else
  echo "appimagetool not found; wrote .deb only. AppImage is skipped on this host." >&2
fi

python3 "$ROOT/scripts/verify-package.py" --root "$ROOT" "$DEB_OUT"
if [[ -f "$APPIMAGE_OUT" ]]; then
  python3 "$ROOT/scripts/verify-package.py" --root "$ROOT" "$APPIMAGE_OUT"
fi
echo "unsigned linux packages under $DIST"
