from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from scripts.manage_releases import (
    ELECTRON_TARGETS,
    compare_semver,
    electron_release_info,
    java_release_info,
    release_outputs,
    stage_electron_assets,
    update_electron_manifest,
)


class ManageReleasesTests(unittest.TestCase):
    def test_semver_comparison_handles_prereleases(self) -> None:
        self.assertLess(compare_semver("2.0.0-beta.2", "2.0.0"), 0)
        self.assertGreater(compare_semver("2.0.0-beta.10", "2.0.0-beta.2"), 0)
        self.assertGreater(compare_semver("1.10.0", "1.9.9"), 0)
        with self.assertRaisesRegex(ValueError, "Invalid semantic version"):
            compare_semver("2.0.0-beta.01", "2.0.0-beta.1")

    def test_electron_release_requires_package_tag_and_notes_to_match(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self.write_electron_project(root, "1.2.0-beta.1")
            info = electron_release_info(root, "next-electron-v1.2.0-beta.1")
            self.assertEqual(info.title, "MooTool Next Electron 1.2.0-beta.1")
            self.assertTrue(info.prerelease)
            self.assertEqual(release_outputs(info)["notes_path"], "next/release-notes/1.2.0-beta.1.md")
            with self.assertRaisesRegex(ValueError, "Electron tag must be"):
                electron_release_info(root, "v1.2.0-beta.1")

    def test_staging_renames_architecture_specific_update_metadata(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            source = root / "dist"
            output = root / "release"
            source.mkdir()
            version = "1.0.0"
            target = ELECTRON_TARGETS["mac-apple-silicon"]
            for template in target.asset_templates:
                (source / template.format(version=version)).write_text("asset", encoding="utf-8")
            payload = target.metadata_payload_template.format(version=version)
            (source / target.metadata_source).write_text(
                f"version: {version}\nfiles:\n  - url: {payload}\n    sha512: checksum\n",
                encoding="utf-8",
            )

            staged = stage_electron_assets(source, output, "mac-apple-silicon", version)
            self.assertIn(output / "arm64-mac.yml", staged)
            self.assertFalse((output / "latest-mac.yml").exists())

    def test_manifest_update_preserves_products_and_replaces_same_version(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self.write_electron_project(root, "1.0.0")
            assets = root / "assets"
            assets.mkdir()
            self.write_complete_electron_assets(assets, "1.0.0")
            manifest_path = root / "update-manifest.json"
            manifest_path.write_text(json.dumps({
                "schemaVersion": 1,
                "products": {
                    "java": {"displayName": "Java", "status": "legacy"},
                    "next-electron": {
                        "displayName": "Electron",
                        "status": "active",
                        "releases": [{"version": "1.0.0", "notes": "old"}],
                    },
                },
            }), encoding="utf-8")

            release = update_electron_manifest(
                root,
                manifest_path,
                assets,
                "next-electron-v1.0.0",
            )
            updated = json.loads(manifest_path.read_text(encoding="utf-8"))
            self.assertFalse(release["prerelease"])
            mac_zip = next(asset for asset in release["assets"] if asset["packageType"] == "zip")
            self.assertEqual(mac_zip["priority"], 10)
            self.assertEqual(updated["products"]["java"]["status"], "legacy")
            self.assertEqual(len(updated["products"]["next-electron"]["releases"]), 1)
            self.assertIn("独立更新", updated["products"]["next-electron"]["releases"][0]["notes"])

    def test_manifest_update_rejects_malformed_existing_release(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self.write_electron_project(root, "1.0.0")
            assets = root / "assets"
            assets.mkdir()
            self.write_complete_electron_assets(assets, "1.0.0")
            manifest_path = root / "update-manifest.json"
            manifest_path.write_text(json.dumps({
                "schemaVersion": 1,
                "products": {
                    "next-electron": {
                        "displayName": "Electron",
                        "status": "active",
                        "releases": [{"notes": "missing version"}],
                    },
                },
            }), encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "string version"):
                update_electron_manifest(root, manifest_path, assets, "next-electron-v1.0.0")

    def test_java_release_uses_all_three_version_sources_and_renders_notes(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self.write_java_project(root, "1.10.0")
            info = java_release_info(root, "v1.10.0")
            self.assertEqual(info.title, "MooTool Java 1.10.0")
            self.assertIn("- 修复版本比较", info.notes)
            with self.assertRaisesRegex(ValueError, "Java tag must be"):
                java_release_info(root, "v1.9.0")

    @staticmethod
    def write_electron_project(root: Path, version: str) -> None:
        (root / "next/release-notes").mkdir(parents=True)
        (root / "next/package.json").write_text(json.dumps({"version": version}), encoding="utf-8")
        title = f"MooTool Next Electron {version}"
        (root / f"next/release-notes/{version}.md").write_text(
            f"# {title}\n\n> 产品线：MooTool Next Electron\n\n## 更新内容\n\n- 独立更新。\n",
            encoding="utf-8",
        )

    @staticmethod
    def write_complete_electron_assets(root: Path, version: str) -> None:
        for target in ELECTRON_TARGETS.values():
            for template in target.asset_templates:
                (root / template.format(version=version)).write_text("asset", encoding="utf-8")
            payload = target.metadata_payload_template.format(version=version)
            (root / target.metadata_destination).write_text(
                f"version: {version}\nfiles:\n  - url: {payload}\n    sha512: checksum\n",
                encoding="utf-8",
            )

    @staticmethod
    def write_java_project(root: Path, version: str) -> None:
        (root / "src/main/java/com/luoboduner/moo/tool/ui").mkdir(parents=True)
        (root / "src/main/resources").mkdir(parents=True)
        (root / "pom.xml").write_text(
            "<?xml version=\"1.0\"?><project xmlns=\"http://maven.apache.org/POM/4.0.0\">"
            f"<modelVersion>4.0.0</modelVersion><version>{version}</version></project>",
            encoding="utf-8",
        )
        tag = f"v{version}"
        (root / "src/main/java/com/luoboduner/moo/tool/ui/UiConsts.java").write_text(
            f'public class UiConsts {{ public static final String APP_VERSION = "{tag}"; }}',
            encoding="utf-8",
        )
        (root / "src/main/resources/version_summary.json").write_text(json.dumps({
            "currentVersion": tag,
            "versionIndex": {tag: "1"},
            "versionDetailList": [{
                "version": tag,
                "title": "维护版本",
                "log": "● 修复版本比较\n● 更新发布说明\n",
            }],
        }), encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
