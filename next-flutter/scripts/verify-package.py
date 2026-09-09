#!/usr/bin/env python3
"""Validate MooTool Next Flutter package names against pubspec version."""

from __future__ import annotations

import argparse
import hashlib
import pathlib
import re
import sys

NAME = re.compile(
    r"^MooTool-Next-Flutter-(?P<version>\d+\.\d+\.\d+(?:-[0-9A-Za-z.]+)?)-"
    r"(?:mac-(?P<mac>arm64|x64)\.dmg|"
    r"win-x64-(?P<win>setup\.exe|portable\.zip)|"
    r"linux-x64\.(?P<linux>AppImage|deb))$"
)


def pubspec_version(root: pathlib.Path) -> str:
    text = (root / "pubspec.yaml").read_text()
    match = re.search(r"^version:\s*([0-9][^\s+]+)", text, re.M)
    if not match:
        raise SystemExit("pubspec.yaml is missing a version")
    return match.group(1)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="+")
    parser.add_argument("--root", default=".")
    args = parser.parse_args()
    root = pathlib.Path(args.root).resolve()
    version = pubspec_version(root)
    failed = 0
    for raw in args.paths:
        path = pathlib.Path(raw)
        if not path.is_file():
            print(f"missing: {path}", file=sys.stderr)
            failed += 1
            continue
        match = NAME.match(path.name)
        if not match:
            print(f"invalid name: {path.name}", file=sys.stderr)
            failed += 1
            continue
        if match.group("version") != version:
            print(
                f"version mismatch: {path.name} vs pubspec {version}",
                file=sys.stderr,
            )
            failed += 1
            continue
        digest = hashlib.sha512(path.read_bytes()).digest()
        print(f"ok {path.name} sha512-b64={__import__('base64').b64encode(digest).decode()} size={path.stat().st_size}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
