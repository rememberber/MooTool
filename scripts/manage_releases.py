#!/usr/bin/env python3
"""Validate MooTool releases, stage Electron assets, and update release manifests."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
import shutil
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from functools import cmp_to_key
from pathlib import Path
from typing import Iterable

DEFAULT_REPO = "rememberber/MooTool"
ELECTRON_PRODUCT_ID = "next-electron"
ELECTRON_TAG_PREFIX = "next-electron-v"
SEMVER_PATTERN = re.compile(
    r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"
    r"(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?"
    r"(?:\+([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?$"
)


@dataclass(frozen=True)
class SemVer:
    major: int
    minor: int
    patch: int
    prerelease: tuple[str, ...]


@dataclass(frozen=True)
class ReleaseInfo:
    version: str
    tag: str
    title: str
    notes: str
    notes_path: Path | None
    prerelease: bool


@dataclass(frozen=True)
class ElectronAssetRule:
    platform: str
    architecture: str
    package_type: str
    priority: int
    file_name_template: str

    def file_name(self, version: str) -> str:
        return self.file_name_template.format(version=version)


@dataclass(frozen=True)
class ElectronTargetRule:
    asset_templates: tuple[str, ...]
    metadata_source: str | None
    metadata_destination: str | None
    metadata_payload_template: str | None


ELECTRON_ASSETS = (
    ElectronAssetRule("darwin", "arm64", "dmg", 10, "MooTool-Next-Electron-{version}-mac-arm64.dmg"),
    ElectronAssetRule("darwin", "x64", "dmg", 10, "MooTool-Next-Electron-{version}-mac-x64.dmg"),
    ElectronAssetRule("win32", "x64", "nsis", 10, "MooTool-Next-Electron-{version}-win-x64-setup.exe"),
    ElectronAssetRule("win32", "x64", "portable", 20, "MooTool-Next-Electron-{version}-win-x64-portable.exe"),
    ElectronAssetRule("linux", "x64", "appimage", 10, "MooTool-Next-Electron-{version}-linux-x64.AppImage"),
    ElectronAssetRule("linux", "x64", "deb", 20, "MooTool-Next-Electron-{version}-linux-x64.deb"),
)

ELECTRON_TARGETS = {
    "mac-apple-silicon": ElectronTargetRule(
        ("MooTool-Next-Electron-{version}-mac-arm64.dmg",),
        None,
        None,
        None,
    ),
    "mac-intel": ElectronTargetRule(
        ("MooTool-Next-Electron-{version}-mac-x64.dmg",),
        None,
        None,
        None,
    ),
    "windows-x64": ElectronTargetRule(
        (
            "MooTool-Next-Electron-{version}-win-x64-setup.exe",
            "MooTool-Next-Electron-{version}-win-x64-portable.exe",
        ),
        "latest.yml",
        "x64.yml",
        "MooTool-Next-Electron-{version}-win-x64-setup.exe",
    ),
    "linux-x64": ElectronTargetRule(
        (
            "MooTool-Next-Electron-{version}-linux-x64.AppImage",
            "MooTool-Next-Electron-{version}-linux-x64.deb",
        ),
        "latest-linux.yml",
        "x64-linux.yml",
        "MooTool-Next-Electron-{version}-linux-x64.AppImage",
    ),
}


def parse_semver(value: str) -> SemVer:
    match = SEMVER_PATTERN.fullmatch(value.strip())
    if match is None:
        raise ValueError(f"Invalid semantic version: {value}")
    prerelease = tuple(match.group(4).split(".")) if match.group(4) else ()
    if any(part.isdigit() and len(part) > 1 and part.startswith("0") for part in prerelease):
        raise ValueError(f"Invalid semantic version: {value}")
    return SemVer(int(match.group(1)), int(match.group(2)), int(match.group(3)), prerelease)


def compare_semver(left: str, right: str) -> int:
    a = parse_semver(left)
    b = parse_semver(right)
    core_a = (a.major, a.minor, a.patch)
    core_b = (b.major, b.minor, b.patch)
    if core_a != core_b:
        return 1 if core_a > core_b else -1
    if not a.prerelease and not b.prerelease:
        return 0
    if not a.prerelease:
        return 1
    if not b.prerelease:
        return -1
    for left_part, right_part in zip(a.prerelease, b.prerelease):
        if left_part == right_part:
            continue
        left_numeric = left_part.isdigit()
        right_numeric = right_part.isdigit()
        if left_numeric and right_numeric:
            return 1 if int(left_part) > int(right_part) else -1
        if left_numeric != right_numeric:
            return -1 if left_numeric else 1
        return 1 if left_part > right_part else -1
    if len(a.prerelease) == len(b.prerelease):
        return 0
    return 1 if len(a.prerelease) > len(b.prerelease) else -1


def load_json(path: Path) -> object:
    return json.loads(path.read_text(encoding="utf-8"))


def parse_release_notes(path: Path, expected_title: str) -> tuple[str, str]:
    if not path.is_file():
        raise ValueError(f"Release notes file does not exist: {path}")
    content = path.read_text(encoding="utf-8").strip()
    lines = content.splitlines()
    if not lines or lines[0].strip() != f"# {expected_title}":
        raise ValueError(f"Release notes must start with '# {expected_title}'")
    notes = "\n".join(lines[1:]).strip()
    if not notes:
        raise ValueError(f"Release notes have no content: {path}")
    if len(notes) > 5000:
        raise ValueError("Release notes must not exceed 5000 characters")
    return content, notes


def validate_multilingual_release_notes(notes: str, path: Path) -> None:
    headings = ("## English", "## 中文", "## 日本語")
    positions = [notes.find(heading) for heading in headings]
    if any(position < 0 for position in positions) or positions != sorted(positions):
        raise ValueError(
            f"Release notes must contain English, 中文, and 日本語 sections in that order: {path}"
        )


def electron_release_info(project_root: Path, tag: str) -> ReleaseInfo:
    package_path = project_root / "next" / "package.json"
    package = load_json(package_path)
    if not isinstance(package, dict) or not isinstance(package.get("version"), str):
        raise ValueError(f"Invalid package version in {package_path}")
    version = package["version"]
    parsed = parse_semver(version)
    expected_tag = f"{ELECTRON_TAG_PREFIX}{version}"
    if tag != expected_tag:
        raise ValueError(f"Electron tag must be {expected_tag}, got {tag}")
    title = f"MooTool Next Electron {version}"
    notes_path = project_root / "next" / "release-notes" / f"{version}.md"
    _, notes = parse_release_notes(notes_path, title)
    validate_multilingual_release_notes(notes, notes_path)
    return ReleaseInfo(version, tag, title, notes, notes_path, bool(parsed.prerelease))


def pom_version(path: Path) -> str:
    root = ET.parse(path).getroot()
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    node = root.find("m:version", namespace)
    if node is None or node.text is None:
        raise ValueError(f"Missing project version in {path}")
    return node.text.strip()


def java_release_info(project_root: Path, tag: str) -> ReleaseInfo:
    version = pom_version(project_root / "pom.xml")
    parsed = parse_semver(version)
    if parsed.prerelease:
        raise ValueError("MooTool Java prereleases are not supported by the legacy stable update feed")
    expected_tag = f"v{version}"
    if tag != expected_tag:
        raise ValueError(f"Java tag must be {expected_tag}, got {tag}")

    ui_consts = (project_root / "src/main/java/com/luoboduner/moo/tool/ui/UiConsts.java").read_text(encoding="utf-8")
    ui_match = re.search(r'APP_VERSION\s*=\s*"([^"]+)"', ui_consts)
    if ui_match is None or ui_match.group(1) != tag:
        actual = ui_match.group(1) if ui_match else "missing"
        raise ValueError(f"UiConsts.APP_VERSION must be {tag}, got {actual}")

    summary_path = project_root / "src/main/resources/version_summary.json"
    summary = load_json(summary_path)
    if not isinstance(summary, dict) or summary.get("currentVersion") != tag:
        raise ValueError(f"version_summary.currentVersion must be {tag}")
    version_index = summary.get("versionIndex")
    details = summary.get("versionDetailList")
    if not isinstance(version_index, dict) or tag not in version_index:
        raise ValueError(f"version_summary.versionIndex is missing {tag}")
    if not isinstance(details, list):
        raise ValueError("version_summary.versionDetailList must be an array")
    detail = next((item for item in details if isinstance(item, dict) and item.get("version") == tag), None)
    localized_fields = ("title", "log", "titleEn", "logEn", "titleJa", "logJa")
    if detail is None or any(
        not isinstance(detail.get(field), str) or not detail[field].strip()
        for field in localized_fields
    ):
        raise ValueError(f"version_summary.versionDetailList is missing complete notes for {tag}")

    title = f"MooTool Java {version}"
    notes = java_release_notes(
        version,
        detail["title"], detail["log"],
        detail["titleEn"], detail["logEn"],
        detail["titleJa"], detail["logJa"],
    )
    return ReleaseInfo(version, tag, title, notes, None, False)


def release_changes(log: str) -> list[str]:
    changes: list[str] = []
    for raw_line in log.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        changes.append(f"- {line[1:].strip()}" if line.startswith("●") else f"- {line}")
    return changes


def java_release_notes(
    version: str,
    title_zh: str, log_zh: str,
    title_en: str, log_en: str,
    title_ja: str, log_ja: str,
) -> str:
    sections = (
        ("English", "Release notes", title_en, log_en,
         "This release updates MooTool Java only; it does not replace or upgrade MooTool Next Electron."),
        ("中文", "更新内容", title_zh, log_zh,
         "本版本只更新 MooTool Java，不会替换或升级 MooTool Next Electron。"),
        ("日本語", "更新内容", title_ja, log_ja,
         "このリリースは MooTool Java のみを更新し、MooTool Next Electron を置き換えたり更新したりしません。"),
    )
    rendered: list[str] = []
    for language, heading, title, log, notice in sections:
        changes = release_changes(log)
        if not changes:
            raise ValueError(f"MooTool Java {version} has no {language} release notes")
        rendered.extend([
            f"## {language}",
            "",
            f"> Product / 产品线 / 製品ライン: MooTool Java  ",
            f"> Version / 版本 / バージョン: {version}  ",
            f"> {notice}",
            "",
            f"### {heading}: {title}",
            "",
            *changes,
            "",
        ])
    return "\n".join([
        *rendered,
    ]).rstrip()


def validate_metadata(path: Path, version: str, expected_payload: str) -> None:
    if not path.is_file():
        raise ValueError(f"Missing Electron update metadata: {path}")
    content = path.read_text(encoding="utf-8")
    if re.search(rf"(?m)^version:\s*['\"]?{re.escape(version)}['\"]?\s*$", content) is None:
        raise ValueError(f"Update metadata {path.name} does not declare version {version}")
    if expected_payload not in content:
        raise ValueError(f"Update metadata {path.name} does not reference {expected_payload}")
    if re.search(r"(?m)^\s*sha512:\s*\S+\s*$", content) is None:
        raise ValueError(f"Update metadata {path.name} does not contain a SHA-512 checksum")


def stage_electron_assets(source_dir: Path, output_dir: Path, target: str, version: str) -> list[Path]:
    rule = ELECTRON_TARGETS.get(target)
    if rule is None:
        raise ValueError(f"Unsupported Electron target: {target}")
    parse_semver(version)
    output_dir.mkdir(parents=True, exist_ok=True)
    staged: list[Path] = []

    for template in rule.asset_templates:
        file_name = template.format(version=version)
        source = source_dir / file_name
        if not source.is_file():
            raise ValueError(f"Missing Electron release asset: {source}")
        destination = output_dir / file_name
        shutil.copy2(source, destination)
        staged.append(destination)
        blockmap = source_dir / f"{file_name}.blockmap"
        if blockmap.is_file():
            blockmap_destination = output_dir / blockmap.name
            shutil.copy2(blockmap, blockmap_destination)
            staged.append(blockmap_destination)

    if rule.metadata_source and rule.metadata_destination and rule.metadata_payload_template:
        metadata_source = source_dir / rule.metadata_source
        expected_payload = rule.metadata_payload_template.format(version=version)
        validate_metadata(metadata_source, version, expected_payload)
        metadata_destination = output_dir / rule.metadata_destination
        shutil.copy2(metadata_source, metadata_destination)
        staged.append(metadata_destination)
    return staged


def release_asset_url(repo: str, tag: str, file_name: str) -> str:
    return f"https://github.com/{repo}/releases/download/{tag}/{file_name}"


def file_sha512(path: Path) -> str:
    digest = hashlib.sha512()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return base64.b64encode(digest.digest()).decode("ascii")


def build_electron_manifest_release(info: ReleaseInfo, assets_dir: Path, repo: str) -> dict[str, object]:
    assets: list[dict[str, object]] = []
    for rule in ELECTRON_ASSETS:
        file_name = rule.file_name(info.version)
        asset_path = assets_dir / file_name
        if not asset_path.is_file():
            raise ValueError(f"Missing Electron release asset: {file_name}")
        assets.append({
            "platform": rule.platform,
            "architecture": rule.architecture,
            "packageType": rule.package_type,
            "priority": rule.priority,
            "fileName": file_name,
            "url": release_asset_url(repo, info.tag, file_name),
            "sha512": file_sha512(asset_path),
            "size": asset_path.stat().st_size,
        })

    for target_rule in ELECTRON_TARGETS.values():
        if target_rule.metadata_destination and target_rule.metadata_payload_template:
            payload = target_rule.metadata_payload_template.format(version=info.version)
            validate_metadata(assets_dir / target_rule.metadata_destination, info.version, payload)

    return {
        "version": info.version,
        "title": info.title,
        "notes": info.notes,
        "prerelease": info.prerelease,
        "releaseUrl": f"https://github.com/{repo}/releases/tag/{info.tag}",
        "assets": assets,
    }


def update_electron_manifest(
    project_root: Path,
    manifest_path: Path,
    assets_dir: Path,
    tag: str,
    repo: str = DEFAULT_REPO,
) -> dict[str, object]:
    info = electron_release_info(project_root, tag)
    manifest = load_json(manifest_path)
    if not isinstance(manifest, dict) or manifest.get("schemaVersion") != 1:
        raise ValueError("Unsupported update manifest schema")
    products = manifest.get("products")
    if not isinstance(products, dict):
        raise ValueError("Update manifest products must be an object")
    product = products.get(ELECTRON_PRODUCT_ID)
    if not isinstance(product, dict) or product.get("status") != "active":
        raise ValueError(f"Update product must be active: {ELECTRON_PRODUCT_ID}")
    releases = product.get("releases")
    if not isinstance(releases, list):
        raise ValueError("Electron releases must be an array")
    for release in releases:
        if not isinstance(release, dict) or not isinstance(release.get("version"), str):
            raise ValueError("Every Electron release must be an object with a string version")
        parse_semver(release["version"])

    new_release = build_electron_manifest_release(info, assets_dir, repo)
    retained = [item for item in releases if item.get("version") != info.version]
    retained.append(new_release)
    retained.sort(
        key=cmp_to_key(lambda left, right: compare_semver(left["version"], right["version"]))
    )
    product["releases"] = retained
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return new_release


def append_github_output(path: Path | None, values: dict[str, str]) -> None:
    if path is None:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as output:
        for key, value in values.items():
            if "\n" in value or "\r" in value:
                raise ValueError(f"GitHub output must be single-line: {key}")
            output.write(f"{key}={value}\n")


def write_java_release_body(path: Path, info: ReleaseInfo) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(f"# {info.title}\n\n{info.notes}\n", encoding="utf-8")


def release_outputs(info: ReleaseInfo) -> dict[str, str]:
    notes_path = f"next/release-notes/{info.version}.md" if info.notes_path else ""
    return {
        "version": info.version,
        "tag": info.tag,
        "title": info.title,
        "prerelease": str(info.prerelease).lower(),
        "make_latest": "false" if info.prerelease else "true",
        "notes_path": notes_path,
    }


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    default_root = Path(__file__).resolve().parents[1]

    validate_electron = subparsers.add_parser("validate-electron")
    validate_electron.add_argument("--project-root", type=Path, default=default_root)
    validate_electron.add_argument("--tag", required=True)
    validate_electron.add_argument("--github-output", type=Path)

    stage_electron = subparsers.add_parser("stage-electron")
    stage_electron.add_argument("--source-dir", required=True, type=Path)
    stage_electron.add_argument("--output-dir", required=True, type=Path)
    stage_electron.add_argument("--target", required=True, choices=sorted(ELECTRON_TARGETS))
    stage_electron.add_argument("--version", required=True)

    update_manifest = subparsers.add_parser("update-electron-manifest")
    update_manifest.add_argument("--project-root", type=Path, default=default_root)
    update_manifest.add_argument("--manifest", required=True, type=Path)
    update_manifest.add_argument("--assets-dir", required=True, type=Path)
    update_manifest.add_argument("--tag", required=True)
    update_manifest.add_argument("--repo", default=DEFAULT_REPO)

    validate_assets = subparsers.add_parser("validate-electron-assets")
    validate_assets.add_argument("--project-root", type=Path, default=default_root)
    validate_assets.add_argument("--assets-dir", required=True, type=Path)
    validate_assets.add_argument("--tag", required=True)

    validate_java = subparsers.add_parser("validate-java")
    validate_java.add_argument("--project-root", type=Path, default=default_root)
    validate_java.add_argument("--tag", required=True)
    validate_java.add_argument("--github-output", type=Path)
    validate_java.add_argument("--body-output", required=True, type=Path)
    return parser


def main(argv: Iterable[str] | None = None) -> int:
    args = build_parser().parse_args(list(argv) if argv is not None else None)
    if args.command == "validate-electron":
        info = electron_release_info(args.project_root.resolve(), args.tag)
        append_github_output(args.github_output, release_outputs(info))
        print(f"Validated {info.tag}: {info.title}")
    elif args.command == "stage-electron":
        staged = stage_electron_assets(args.source_dir.resolve(), args.output_dir.resolve(), args.target, args.version)
        for path in staged:
            print(path)
    elif args.command == "update-electron-manifest":
        release = update_electron_manifest(
            args.project_root.resolve(),
            args.manifest.resolve(),
            args.assets_dir.resolve(),
            args.tag,
            args.repo,
        )
        print(f"Updated Electron manifest for {release['version']}")
    elif args.command == "validate-electron-assets":
        info = electron_release_info(args.project_root.resolve(), args.tag)
        release = build_electron_manifest_release(info, args.assets_dir.resolve(), DEFAULT_REPO)
        print(f"Validated {len(release['assets'])} Electron assets for {info.tag}")
    elif args.command == "validate-java":
        info = java_release_info(args.project_root.resolve(), args.tag)
        write_java_release_body(args.body_output.resolve(), info)
        append_github_output(args.github_output, release_outputs(info))
        print(f"Validated {info.tag}: {info.title}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ValueError, OSError, json.JSONDecodeError, ET.ParseError) as error:
        raise SystemExit(str(error)) from error
