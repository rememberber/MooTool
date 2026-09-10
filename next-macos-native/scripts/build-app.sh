#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/check-toolchain.sh
native_configuration=release
native_arch=$(uname -m)
native_dmg=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --debug) native_configuration=debug; shift ;;
    --arch) native_arch="$2"; shift 2 ;;
    --dmg) native_dmg=true; shift ;;
    *) echo "Usage: $0 [--debug] [--arch arm64|x86_64|universal] [--dmg]" >&2; exit 2 ;;
  esac
done
case "$native_arch" in arm64|x86_64|universal) ;; *) echo 'Invalid architecture' >&2; exit 2 ;; esac
native_version=$(tr -d '[:space:]' < VERSION)
if [[ ! "$native_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then echo 'VERSION must be major.minor.patch' >&2; exit 2; fi
native_output="$PWD/dist/$native_arch"
native_app="$native_output/MooTool Next Native.app"
mkdir -p "$native_output"
rm -rf "$native_app"
mkdir -p "$native_app/Contents/MacOS" "$native_app/Contents/Resources"
if [[ "$native_arch" == universal ]]; then native_arches=(arm64 x86_64); else native_arches=("$native_arch"); fi
native_binaries=()
native_workers=()
for native_target in "${native_arches[@]}"; do
  swift build -c "$native_configuration" --arch "$native_target"
  native_bin=$(swift build -c "$native_configuration" --arch "$native_target" --show-bin-path)
  native_binaries+=("$native_bin/MooToolNextNative")
  native_workers+=("$native_bin/MooToolJSONWorker")
done
if [[ "$native_arch" == universal ]]; then
  lipo -create "${native_binaries[@]}" -output "$native_app/Contents/MacOS/MooToolNextNative"
  lipo -create "${native_workers[@]}" -output "$native_app/Contents/MacOS/MooToolJSONWorker"
else
  cp "${native_binaries[0]}" "$native_app/Contents/MacOS/MooToolNextNative"
  cp "${native_workers[0]}" "$native_app/Contents/MacOS/MooToolJSONWorker"
fi
cp -R "$native_bin/MooToolNextNative_MooToolNextApp.bundle" "$native_app/Contents/Resources/"
cp -R "$native_bin/MooToolNextNative_MooToolNextCore.bundle" "$native_app/Contents/Resources/"
cp Sources/MooToolNextApp/Resources/AppIcon.icns "$native_app/Contents/Resources/AppIcon.icns"
cp LICENSE.txt "$native_app/Contents/Resources/MooTool-LICENSE.txt"
cp .build/checkouts/Yams/LICENSE "$native_app/Contents/Resources/Yams-LICENSE.txt"
python3 - "$native_app" "$native_version" <<'PY'
import plistlib, sys
from pathlib import Path
app, version = Path(sys.argv[1]), sys.argv[2]
info = {
    'CFBundleIdentifier': 'com.rememberber.mootool.next.macos-native',
    'CFBundleName': 'MooTool Next Native',
    'CFBundleDisplayName': 'MooTool Next Native',
    'CFBundleExecutable': 'MooToolNextNative',
    'CFBundlePackageType': 'APPL',
    'CFBundleShortVersionString': version,
    'CFBundleVersion': version,
    'CFBundleIconFile': 'AppIcon',
    'LSMinimumSystemVersion': '14.0',
    'LSApplicationCategoryType': 'public.app-category.developer-tools',
    'NSPrincipalClass': 'NSApplication',
    'NSHighResolutionCapable': True,
    'NSHumanReadableCopyright': 'Copyright © Zhou Bo. MIT License.',
    'NSAppTransportSecurity': {'NSAllowsArbitraryLoads': True},
}
with (app/'Contents/Info.plist').open('wb') as f: plistlib.dump(info, f)
PY
if [[ -n "${NATIVE_SIGN_IDENTITY:-}" ]]; then
  codesign --force --options runtime --timestamp --sign "$NATIVE_SIGN_IDENTITY" "$native_app/Contents/MacOS/MooToolJSONWorker"
  codesign --force --options runtime --timestamp --sign "$NATIVE_SIGN_IDENTITY" "$native_app"
else
  codesign --force --sign - "$native_app/Contents/MacOS/MooToolJSONWorker"
  codesign --force --sign - "$native_app"
fi
codesign --verify --deep --strict "$native_app"
plutil -lint "$native_app/Contents/Info.plist"
if [[ "$native_dmg" == true ]]; then
  native_stage=$(mktemp -d /tmp/mootool-native-dmg.XXXXXX)
  trap 'rm -rf "$native_stage"' EXIT
  cp -R "$native_app" "$native_stage/"
  ln -s /Applications "$native_stage/Applications"
  hdiutil create -volname 'MooTool Next Native' -srcfolder "$native_stage" -ov -format UDZO \
    "$native_output/MooTool-Next-macOS-Native-$native_version-mac-$native_arch.dmg"
fi
python3 - "$native_output" "$native_version" "$native_arch" "$native_configuration" "$native_dmg" <<'PY'
import datetime, hashlib, json, pathlib, subprocess, sys
output, version, arch, configuration, has_dmg = sys.argv[1:]
directory = pathlib.Path(output)
executable = directory / 'MooTool Next Native.app/Contents/MacOS/MooToolNextNative'
artifacts = [{'path': str(executable.relative_to(directory)), 'sha256': hashlib.sha256(executable.read_bytes()).hexdigest()}]
worker = executable.with_name('MooToolJSONWorker')
assert set(subprocess.check_output(['lipo', '-archs', str(worker)], text=True).split()) == set(subprocess.check_output(['lipo', '-archs', str(executable)], text=True).split())
artifacts.append({'path': str(worker.relative_to(directory)), 'sha256': hashlib.sha256(worker.read_bytes()).hexdigest()})
if has_dmg == 'true':
    dmg = directory / f'MooTool-Next-macOS-Native-{version}-mac-{arch}.dmg'
    artifacts.append({'path': dmg.name, 'bytes': dmg.stat().st_size, 'sha256': hashlib.sha256(dmg.read_bytes()).hexdigest()})
info = {'product': 'next-macos-native', 'bundleID': 'com.rememberber.mootool.next.macos-native',
        'version': version, 'configuration': configuration, 'minimumMacOS': '14.0',
        'architectures': subprocess.check_output(['lipo', '-archs', str(executable)], text=True).split(),
        'builtAt': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'artifacts': artifacts}
(directory / 'build-info.json').write_text(json.dumps(info, indent=2) + '\n')
PY
printf 'App: %s\n' "$native_app"
