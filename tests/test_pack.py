"""Regression tests for the release ZIP validator."""

import contextlib
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from tools.verify_pack import verify_archive


ROOT = Path(__file__).resolve().parents[1]
RELEASE = ROOT / "release/AutumnVoid-PvP-1.0.0.zip"


class PackTests(unittest.TestCase):
    def changed_archive(self, folder, *, omit=None, missing_format=None, corrupt=None):
        destination = Path(folder) / "test-pack.zip"
        with zipfile.ZipFile(RELEASE) as source, zipfile.ZipFile(destination, "w") as result:
            for name in source.namelist():
                if name == omit:
                    continue
                data = source.read(name)
                if name == "pack.mcmeta" and missing_format:
                    metadata = json.loads(data)
                    del metadata["pack"][missing_format]
                    data = json.dumps(metadata).encode()
                if name == corrupt:
                    data = b"not a PNG"
                result.writestr(name, data)
        return destination

    def test_release_is_valid(self):
        with contextlib.redirect_stdout(io.StringIO()):
            verify_archive(RELEASE)

    def test_new_format_fields_are_required(self):
        for field in ("min_format", "max_format"):
            with self.subTest(field=field), tempfile.TemporaryDirectory() as folder:
                path = self.changed_archive(folder, missing_format=field)
                with self.assertRaisesRegex(SystemExit, field):
                    verify_archive(path)

    def test_required_texture_cannot_be_missing(self):
        with tempfile.TemporaryDirectory() as folder:
            path = self.changed_archive(folder, omit="assets/minecraft/textures/item/ender_pearl.png")
            with self.assertRaisesRegex(SystemExit, "missing required texture"):
                verify_archive(path)

    def test_corrupt_png_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            path = self.changed_archive(folder, corrupt="pack.png")
            with self.assertRaisesRegex(SystemExit, "invalid PNG"):
                verify_archive(path)

    def test_root_metadata_is_required(self):
        with tempfile.TemporaryDirectory() as folder:
            path = self.changed_archive(folder, omit="pack.mcmeta")
            with self.assertRaisesRegex(SystemExit, "missing ZIP-root file"):
                verify_archive(path)


if __name__ == "__main__":
    unittest.main()
