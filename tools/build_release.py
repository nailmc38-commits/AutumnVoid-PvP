#!/usr/bin/env python3
"""Build the install-ready AutumnVoid PvP resource-pack ZIP."""

from __future__ import annotations

import argparse
import zipfile
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--version", default="1.0.0")
    args = parser.parse_args()

    root = args.root.resolve()
    release_dir = root / "release"
    release_dir.mkdir(parents=True, exist_ok=True)
    destination = release_dir / f"AutumnVoid-PvP-{args.version}.zip"

    required = (root / "pack.mcmeta", root / "pack.png", root / "assets")
    missing = [str(path) for path in required if not path.exists()]
    if missing:
        raise SystemExit("Missing required pack files: " + ", ".join(missing))

    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in (root / "pack.mcmeta", root / "pack.png"):
            archive.write(path, path.relative_to(root))
        for path in sorted((root / "assets").rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(root))

    print(destination)


if __name__ == "__main__":
    main()
