#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

TYPE="app-image"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --type)
      TYPE="${2:-}"
      shift 2
      ;;
    --type=*)
      TYPE="${1#--type=}"
      shift
      ;;
    -h|--help)
      echo "Usage: $0 --type app-image"
      echo "P0 supports app-image on the current OS/arch. Installers (dmg/msi/deb) are P7."
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

if [[ "$TYPE" != "app-image" ]]; then
  echo "P0 packaging only implements app-image. Received: $TYPE" >&2
  exit 2
fi

JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVA_BIN="${JAVA_BIN:-$(command -v java)}"
if [[ -z "$JAVA_BIN" ]]; then
  echo "A JDK 25 runtime is required to package." >&2
  exit 1
fi
JAVA_HOME="${JAVA_HOME:-$(cd "$(dirname "$JAVA_BIN")/.." && pwd)}"
JLINK="$JAVA_HOME/bin/jlink"
JPACKAGE="$JAVA_HOME/bin/jpackage"
if [[ ! -x "$JLINK" || ! -x "$JPACKAGE" ]]; then
  echo "jlink/jpackage not found in $JAVA_HOME" >&2
  exit 1
fi

OS="$(uname -s)"
ARCH="$(uname -m)"
case "$OS" in
  Darwin)
    PKG_OS="mac"
    JFX_OS="osx"
    ;;
  Linux)
    PKG_OS="linux"
    JFX_OS="linux"
    ;;
  *)
    echo "Unsupported OS: $OS" >&2
    exit 1
    ;;
esac
case "$ARCH" in
  arm64|aarch64)
    PKG_ARCH="arm64"
    JFX_ARCH="aarch64"
    JFX_PLATFORM="mac-aarch64"
    ;;
  x86_64|amd64)
    PKG_ARCH="x64"
    JFX_ARCH="x64"
    JFX_PLATFORM="mac"
    ;;
  *)
    echo "Unsupported arch: $ARCH" >&2
    exit 1
    ;;
esac
if [[ "$PKG_OS" == "linux" && "$PKG_ARCH" == "x64" ]]; then
  JFX_PLATFORM="linux"
fi
if [[ "$PKG_OS" == "linux" && "$PKG_ARCH" == "arm64" ]]; then
  JFX_PLATFORM="linux-aarch64"
fi
if [[ "$PKG_OS" == "mac" && "$PKG_ARCH" == "x64" ]]; then
  JFX_PLATFORM="mac"
fi

VERSION="$("$ROOT/mvnw" -q -DforceStdout help:evaluate -Dexpression=project.version | tail -n 1)"
JAVAFX_VERSION="$("$ROOT/mvnw" -q -DforceStdout help:evaluate -Dexpression=javafx.version | tail -n 1)"
# jpackage/macOS rejects an app-version whose first integer is 0 (JDK 25).
# Product version remains 0.1.0-SNAPSHOT inside the jar; installer numeric version is mapped.
APP_VERSION="1.0.0"
DIST="$ROOT/dist"
RUNTIME_CACHE="$ROOT/.runtime"
JMODS_DIR="$RUNTIME_CACHE/javafx-jmods-$JAVAFX_VERSION-$PKG_OS-$PKG_ARCH"
JMODS_ZIP="$RUNTIME_CACHE/openjfx-${JAVAFX_VERSION}_${JFX_OS}-${JFX_ARCH}_bin-jmods.zip"
JMODS_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_${JFX_OS}-${JFX_ARCH}_bin-jmods.zip"

mkdir -p "$DIST" "$RUNTIME_CACHE"
"$ROOT/mvnw" -q -DskipTests package

if [[ ! -d "$JMODS_DIR" ]]; then
  echo "Downloading JavaFX jmods $JAVAFX_VERSION for $JFX_OS-$JFX_ARCH"
  curl -fL --retry 3 -o "$JMODS_ZIP" "$JMODS_URL"
  mkdir -p "$JMODS_DIR"
  unzip -qo "$JMODS_ZIP" -d "$RUNTIME_CACHE/jmods-unpack"
  FOUND="$(find "$RUNTIME_CACHE/jmods-unpack" -name 'javafx.base.jmod' | head -n 1)"
  if [[ -z "$FOUND" ]]; then
    echo "JavaFX jmods archive did not contain javafx.base.jmod" >&2
    exit 1
  fi
  cp "$(dirname "$FOUND")"/*.jmod "$JMODS_DIR/"
fi

INPUT="$ROOT/target/app-input"
rm -rf "$INPUT"
mkdir -p "$INPUT/lib"
cp "$ROOT/target/mootool-next-fx-${VERSION}.jar" "$INPUT/mootool-next-fx.jar"
# Runtime classpath libraries excluding JavaFX platform jars (they come from jlink).
"$ROOT/mvnw" -q -DincludeScope=runtime dependency:copy-dependencies -DoutputDirectory="$INPUT/lib"
find "$INPUT/lib" -name 'javafx-*.jar' -delete

RUNTIME_IMAGE="$ROOT/target/runtime"
rm -rf "$RUNTIME_IMAGE"
"$JLINK" \
  --module-path "$JAVA_HOME/jmods:$JMODS_DIR" \
  --add-modules java.base,java.desktop,java.logging,java.sql,java.xml,java.naming,java.management,java.prefs,java.net.http,java.scripting,java.datatransfer,jdk.unsupported,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.charsets,jdk.localedata,jdk.zipfs,javafx.base,javafx.graphics,javafx.controls \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress zip-6 \
  --output "$RUNTIME_IMAGE"

APP_NAME="MooTool Next FX"
DEST="$DIST"
rm -rf "$DEST/${APP_NAME}.app" "$DEST/$APP_NAME"

JPACKAGE_ARGS=(
  --type app-image
  --name "$APP_NAME"
  --app-version "$APP_VERSION"
  --vendor "rememberber"
  --description "MooTool Next FX"
  --dest "$DEST"
  --input "$INPUT"
  --main-jar mootool-next-fx.jar
  --main-class com.rememberber.mootool.nextfx.Launcher
  --runtime-image "$RUNTIME_IMAGE"
  --java-options "-Dmootool.profile=release"
)
if [[ "$PKG_OS" == "mac" ]]; then
  JPACKAGE_ARGS+=(--mac-package-identifier com.rememberber.mootool.next.fx)
  if [[ -f "$ROOT/packaging/icon.icns" ]]; then
    JPACKAGE_ARGS+=(--icon "$ROOT/packaging/icon.icns")
  fi
fi
"$JPACKAGE" "${JPACKAGE_ARGS[@]}"
APP_IMAGE="$DEST/${APP_NAME}.app"
if [[ ! -d "$APP_IMAGE" && ! -d "$DEST/$APP_NAME" ]]; then
  echo "jpackage did not produce an app-image under $DEST" >&2
  exit 1
fi
echo "App image created under $DEST"
