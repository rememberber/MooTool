from __future__ import annotations

import tarfile
import tempfile
import unittest
from pathlib import Path

from scripts.prepare_jdks import (
    TARGETS,
    format_size,
    install_from_existing_java_home,
    install_jmods_from_archive,
    locate_java_home,
    locate_jmods_dir,
    parse_targets,
    prepare_target,
)


class PrepareJdksTests(unittest.TestCase):
    def test_cached_jdk_requires_requested_version(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            home = root / "jdks" / "mac" / "arm64" / "home"
            (home / "bin").mkdir(parents=True)
            (home / "bin" / "java").touch()
            for release in (None, 'JAVA_VERSION="21.0.8"\n'):
                if release is not None:
                    (home / "release").write_text(release)
                with self.assertRaisesRegex(RuntimeError, "required Java 25"):
                    prepare_target(root, "25", TARGETS["mac-arm64"], False, False, None)
            (home / "release").write_text('JAVA_VERSION="25.0.1"\n')
            (home / "jmods").mkdir()
            (home / "jmods" / "java.base.jmod").write_bytes(b"jmod")
            self.assertEqual(prepare_target(root, "25", TARGETS["mac-arm64"], False, False, None), home)

    def test_wrong_java_home_does_not_replace_cache(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            source = root / "source"
            source.mkdir()
            (source / "release").write_text('JAVA_VERSION="21.0.8"\n')
            home = root / "jdks" / "mac" / "arm64" / "home"
            home.mkdir(parents=True)
            marker = home / "keep"
            marker.touch()
            with self.assertRaisesRegex(RuntimeError, "required Java 25"):
                prepare_target(root, "25", TARGETS["mac-arm64"], True, False, source)
            self.assertTrue(marker.exists())

    def test_format_size_for_bytes(self) -> None:
        self.assertEqual(format_size(512), "512 B")

    def test_format_size_for_mebibytes(self) -> None:
        self.assertEqual(format_size(5 * 1024 * 1024), "5.0 MiB")

    def test_parse_targets_accepts_all(self) -> None:
        targets = parse_targets("all")
        self.assertEqual({target.key for target in targets}, set(TARGETS))

    def test_parse_targets_rejects_unknown_target(self) -> None:
        with self.assertRaises(ValueError):
            parse_targets("mac-x64,unknown")

    def test_locate_java_home_for_standard_layout(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            home = root / "jdk-25"
            (home / "bin").mkdir(parents=True)
            (home / "bin" / "java").write_text("", encoding="utf-8")
            self.assertEqual(locate_java_home(root), home)

    def test_locate_java_home_for_macos_layout(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            home = root / "temurin-25.jdk" / "Contents" / "Home"
            (home / "bin").mkdir(parents=True)
            (home / "bin" / "java").write_text("", encoding="utf-8")
            self.assertEqual(locate_java_home(root), home)

    def test_locate_jmods_dir_for_temurin_layout(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            jmods = root / "jdk-25.0.4.1+1-jmods"
            jmods.mkdir(parents=True)
            (jmods / "java.base.jmod").write_bytes(b"jmod")
            self.assertEqual(locate_jmods_dir(root), jmods)

    def test_locate_jmods_dir_for_nested_layout(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            jmods = root / "jdk-25-jmods" / "jmods"
            jmods.mkdir(parents=True)
            (jmods / "java.base.jmod").write_bytes(b"jmod")
            self.assertEqual(locate_jmods_dir(root), jmods)

    def test_install_jmods_from_archive(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            payload = root / "stage" / "jdk-25.0.4.1+1-jmods"
            payload.mkdir(parents=True)
            (payload / "java.base.jmod").write_bytes(b"jmod")
            archive = root / "jmods.tar.gz"
            with tarfile.open(archive, "w:gz") as tar:
                tar.add(payload, arcname="jdk-25.0.4.1+1-jmods")
            home = root / "home"
            home.mkdir()

            install_jmods_from_archive(archive, TARGETS["linux-x64"], home)

            self.assertEqual((home / "jmods" / "java.base.jmod").read_bytes(), b"jmod")

    def test_install_from_existing_java_home(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            source_home = root / "source-home"
            (source_home / "bin").mkdir(parents=True)
            (source_home / "bin" / "java").write_text("", encoding="utf-8")
            destination_home = root / "jdks" / "mac" / "x64" / "home"

            install_from_existing_java_home(destination_home, source_home)

            self.assertTrue((destination_home / "bin" / "java").exists())


if __name__ == "__main__":
    unittest.main()
