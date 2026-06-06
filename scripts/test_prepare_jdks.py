from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from scripts.prepare_jdks import TARGETS, locate_java_home, parse_targets


class PrepareJdksTests(unittest.TestCase):
    def test_parse_targets_accepts_all(self) -> None:
        targets = parse_targets("all")
        self.assertEqual({target.key for target in targets}, set(TARGETS))

    def test_parse_targets_rejects_unknown_target(self) -> None:
        with self.assertRaises(ValueError):
            parse_targets("mac-x64,unknown")

    def test_locate_java_home_for_standard_layout(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            home = root / "jdk-21"
            (home / "bin").mkdir(parents=True)
            (home / "bin" / "java").write_text("", encoding="utf-8")
            self.assertEqual(locate_java_home(root), home)

    def test_locate_java_home_for_macos_layout(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            home = root / "temurin-21.jdk" / "Contents" / "Home"
            (home / "bin").mkdir(parents=True)
            (home / "bin" / "java").write_text("", encoding="utf-8")
            self.assertEqual(locate_java_home(root), home)


if __name__ == "__main__":
    unittest.main()

