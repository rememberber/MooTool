from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from scripts.collect_release_assets import collect_assets, extra_label, normalized_name, parse_project_info


class CollectReleaseAssetsTests(unittest.TestCase):
    def setUp(self) -> None:
        self.project_root = Path(__file__).resolve().parents[1]
        self.project = parse_project_info(self.project_root / "pom.xml")

    def test_normalized_name_for_macos_installer(self) -> None:
        actual = normalized_name(f"MooTool_{self.project.version}.pkg", self.project, "mac-apple-silicon")
        self.assertEqual(actual, f"MooTool-{self.project.version}-mac-apple-silicon.pkg")

    def test_normalized_name_preserves_runnable_extra_label(self) -> None:
        actual = normalized_name(f"MooTool-{self.project.version}-runnable.jar", self.project, "mac-intel")
        self.assertEqual(actual, f"MooTool-{self.project.version}-mac-intel-runnable.jar")

    def test_extra_label_removes_platform_noise(self) -> None:
        actual = extra_label(f"MooTool-{self.project.version}-mac.tar.gz", self.project)
        self.assertEqual(actual, "")

    def test_collect_assets_renames_supported_files(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            source = root / "source"
            output = root / "out"
            source.mkdir()
            (source / f"MooTool_{self.project.version}.dmg").write_text("dmg", encoding="utf-8")
            (source / f"MooTool-{self.project.version}-runnable.jar").write_text("jar", encoding="utf-8")
            collected = collect_assets(self.project_root, source, output, "mac-intel")
            self.assertCountEqual([item.destination.name for item in collected], [
                f"MooTool-{self.project.version}-mac-intel.dmg",
                f"MooTool-{self.project.version}-mac-intel-runnable.jar",
            ])


if __name__ == "__main__":
    unittest.main()



