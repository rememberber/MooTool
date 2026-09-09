#!/usr/bin/env python3
"""Validate next-flutter-v{version} against pubspec and release notes."""

from __future__ import annotations

import argparse
import pathlib
import re
import sys


def pubspec_version(root: pathlib.Path) -> str:
    text = (root / "pubspec.yaml").read_text()
    match = re.search(r"^version:\s*([0-9][^\s+]+)", text, re.M)
    if not match:
        raise SystemExit("pubspec.yaml is missing a version")
    return match.group(1)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".")
    parser.add_argument("--tag", default="")
    args = parser.parse_args()
    root = pathlib.Path(args.root).resolve()
    version = pubspec_version(root)
    notes = root / "release-notes" / f"{version}.md"
    if not notes.is_file():
        print(f"missing release notes: {notes}", file=sys.stderr)
        return 1
    heading = notes.read_text().splitlines()[0]
    if version not in heading or "MooTool Next Flutter" not in heading:
        print(f"release notes heading must include product and version: {heading}", file=sys.stderr)
        return 1
    if args.tag:
        expected = f"next-flutter-v{version}"
        if args.tag != expected:
            print(f"tag {args.tag} does not match {expected}", file=sys.stderr)
            return 1
    print(f"ok version={version} notes={notes.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
