from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from scripts.collect_release_assets import collect_assets, extra_label, normalized_name, parse_project_info


class CollectReleaseAssetsTests(unittest.TestCase):
    def setUp(self) -> None:
        self.project_root = Path("/Users/zhoubo/IdeaProjectsCE/MooTool")
        self.project = parse_project_info(self.project_root / "pom.xml")

    def test_normalized_name_for_macos_installer(self) -> None:
        actual = normalized_name("MooTool_1.7.0.pkg", self.project, "mac-apple-silicon")
        self.assertEqual(actual, "MooTool-1.7.0-mac-apple-silicon.pkg")

    def test_normalized_name_preserves_runnable_extra_label(self) -> None:
        actual = normalized_name("MooTool-1.7.0-runnable.jar", self.project, "mac-intel")
        self.assertEqual(actual, "MooTool-1.7.0-mac-intel-runnable.jar")

    def test_extra_label_removes_platform_noise(self) -> None:
        actual = extra_label("MooTool-1.7.0-mac.tar.gz", self.project)
        self.assertEqual(actual, "")

    def test_collect_assets_renames_supported_files(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            source = root / "source"
            output = root / "out"
            source.mkdir()
            (source / "MooTool_1.7.0.dmg").write_text("dmg", encoding="utf-8")
            (source / "MooTool-1.7.0-runnable.jar").write_text("jar", encoding="utf-8")
            collected = collect_assets(self.project_root, source, output, "mac-intel")
            self.assertCountEqual([item.destination.name for item in collected], [
                "MooTool-1.7.0-mac-intel.dmg",
                "MooTool-1.7.0-mac-intel-runnable.jar",
            ])


if __name__ == "__main__":
    unittest.main()




