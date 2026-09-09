#!/usr/bin/env python3
"""Verify a built Universal package in an isolated directory and workspace."""
import hashlib
import json
import os
import pathlib
import plistlib
import shutil
import signal
import subprocess
import tempfile
import time

root = pathlib.Path(__file__).resolve().parent.parent
version = (root / 'VERSION').read_text().strip()
output = root / 'dist/universal'
app = output / 'MooTool Next Native.app'
report = []


def run(args, **kwargs):
    result = subprocess.run([str(a) for a in args], check=True, text=True,
                            capture_output=True, timeout=60, **kwargs)
    if result.stdout.strip():
        report.append(result.stdout.strip())
    return result.stdout


info = json.loads((output / 'build-info.json').read_text())
assert info['version'] == version and info['product'] == 'next-macos-native'
assert set(info['architectures']) == {'arm64', 'x86_64'}
for artifact in info['artifacts']:
    data = (output / artifact['path']).read_bytes()
    assert hashlib.sha256(data).hexdigest() == artifact['sha256']
    if 'bytes' in artifact:
        assert len(data) == artifact['bytes']
with (app / 'Contents/Info.plist').open('rb') as file:
    plist = plistlib.load(file)
assert plist['CFBundleIdentifier'] == 'com.rememberber.mootool.next.macos-native'
assert plist['CFBundleShortVersionString'] == version
for name in ['MooToolNextNative', 'MooToolJSONWorker']:
    executable = app / 'Contents/MacOS' / name
    assert set(run(['lipo', '-archs', executable]).split()) == {'arm64', 'x86_64'}
    dependencies = run(['otool', '-L', executable])
    for line in dependencies.splitlines()[1:]:
        if 'compatibility version' in line:
            assert not line.strip().startswith('/Users/'), line
run(['hdiutil', 'verify', output / f'MooTool-Next-macOS-Native-{version}-mac-universal.dmg'])
with tempfile.TemporaryDirectory(prefix='mootool-native-independent-') as tmp:
    directory = pathlib.Path(tmp)
    copied = directory / app.name
    shutil.copytree(app, copied, symlinks=True)
    run(['codesign', '--verify', '--deep', '--strict', copied])
    env = dict(os.environ, MOOTOOL_NATIVE_TEST_DATA=str(directory / 'workspace'))
    result = run([copied / 'Contents/MacOS/MooToolNextNative', '--verify-bundle'],
                 cwd=directory, env=env)
    assert 'JSON helper execution, note quick replacement, attachment backup/restore and image decoding' in result
    started = time.monotonic()
    with subprocess.Popen([str(copied / 'Contents/MacOS/MooToolJSONWorker')],
                          stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE, cwd=directory) as bounded:
        # Keep stdin open without data: only the helper's own timer can stop it.
        try:
            status = bounded.wait(timeout=6)
        except subprocess.TimeoutExpired:
            bounded.kill()
            bounded.wait()
            raise
        assert status == -signal.SIGALRM and time.monotonic() - started < 5.5, status
    report.append('PASS: standalone JSON helper terminates at its own four-second deadline while blocked on input, without a parent watchdog')
report.append('PASS: Universal app and helper architectures, artifact hashes, DMG integrity, independent bundle copy, signatures, embedded parser resources, helper execution and portable image backup/restore')
(root / f'dist/verify-package-{version}.log').write_text('\n'.join(report) + '\n')
print('\n'.join(report[-2:]))
