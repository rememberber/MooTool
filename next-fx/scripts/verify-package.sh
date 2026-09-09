#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <app-image-path>" >&2
  exit 2
fi

TARGET="$1"
if [[ ! -e "$TARGET" ]]; then
  echo "Missing package path: $TARGET" >&2
  exit 1
fi

echo "Verifying $TARGET"
if [[ -d "$TARGET" && -d "$TARGET/Contents" ]]; then
  INFO="$TARGET/Contents/Info.plist"
  grep -q "com.rememberber.mootool.next.fx" "$INFO"
  grep -q "MooTool Next FX" "$INFO"
  BINARY="$TARGET/Contents/MacOS/MooTool Next FX"
  [[ -x "$BINARY" ]]
  RUNTIME_JAVA="$TARGET/Contents/runtime/Contents/Home/bin/java"
  [[ -x "$RUNTIME_JAVA" ]]
  CFG="$TARGET/Contents/app/MooTool Next FX.cfg"
  [[ -f "$CFG" ]]
  grep -q "jackson-databind" "$CFG"
  grep -q "sqlite-jdbc" "$CFG"
  grep -q "richtextfx" "$CFG"
  APP_DIR="$TARGET/Contents/app"
  [[ -f "$APP_DIR/mootool-next-fx.jar" ]]
  ls "$APP_DIR/lib"/jackson-databind-*.jar >/dev/null
  ls "$APP_DIR/lib"/sqlite-jdbc-*.jar >/dev/null
  ls "$APP_DIR/lib"/richtextfx-*.jar >/dev/null
  "$RUNTIME_JAVA" -version
  echo "macOS app-image identity, launcher, bundled java, and classpath jars are present."
elif [[ -d "$TARGET" && -x "$TARGET/MooTool Next FX" ]]; then
  [[ -x "$TARGET/lib/runtime/bin/java" ]]
  echo "Linux app-image launcher and bundled java are present."
else
  echo "Unrecognized app-image layout." >&2
  exit 1
fi
