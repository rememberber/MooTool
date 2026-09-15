#!/usr/bin/env bash
# Rename jpackage output to the names in docs/data-platform-release.md.
# Run after packageDistributionForCurrentOS on that OS. Does not install or notarize.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${1:-${APP_VERSION:-0.1.0}}"
OS_NAME="$(uname -s | tr '[:upper:]' '[:lower:]')"
ARCH_RAW="$(uname -m)"
case "$ARCH_RAW" in
  arm64|aarch64) ARCH="arm64" ;;
  x86_64|amd64) ARCH="x64" ;;
  *) ARCH="$ARCH_RAW" ;;
esac
BIN="$ROOT/composeApp/build/compose/binaries/main"
DEST="$ROOT/composeApp/build/compose/binaries/named"
mkdir -p "$DEST"

copy_first() {
  local src_dir="$1"
  local dest_name="$2"
  local match
  match="$(find "$src_dir" -maxdepth 1 -type f \( -name '*.dmg' -o -name '*.pkg' -o -name '*.msi' -o -name '*.exe' -o -name '*.deb' -o -name '*.rpm' \) 2>/dev/null | head -n 1 || true)"
  if [[ -z "$match" ]]; then
    echo "no package in $src_dir (build on this OS first)"
    return 1
  fi
  cp "$match" "$DEST/$dest_name"
  echo "copied $(basename "$match") -> $DEST/$dest_name"
}

case "$OS_NAME" in
  darwin)
    copy_first "$BIN/dmg" "MooTool-Next-Compose-${VERSION}-mac-${ARCH}.dmg"
    ;;
  linux)
    if [[ -d "$BIN/deb" ]]; then
      copy_first "$BIN/deb" "MooTool-Next-Compose-${VERSION}-linux-${ARCH}.deb"
    fi
    if [[ -d "$BIN/rpm" ]]; then
      copy_first "$BIN/rpm" "MooTool-Next-Compose-${VERSION}-linux-${ARCH}.rpm" || true
    fi
    ;;
  mingw*|msys*|cygwin*|windows*)
    copy_first "$BIN/msi" "MooTool-Next-Compose-${VERSION}-win-${ARCH}-setup.msi"
    ;;
  *)
    echo "unsupported OS: $OS_NAME"
    exit 1
    ;;
esac
echo "unsigned/unnotarized artifacts only; do not mark P7 installation accepted"
