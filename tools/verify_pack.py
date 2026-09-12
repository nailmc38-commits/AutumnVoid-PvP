#!/usr/bin/env python3
"""Validate metadata, PNG files, and archive layout for the resource pack."""

from __future__ import annotations

import io
import json
import sys
import zipfile
from pathlib import Path

from PIL import Image


def required_textures() -> set[str]:
    paths = set()
    for material in ("leather", "chainmail", "iron", "golden", "copper", "diamond", "netherite"):
        for piece in ("helmet", "chestplate", "leggings", "boots"):
            paths.add(f"assets/minecraft/textures/item/{material}_{piece}.png")
        worn = "gold" if material == "golden" else material
        for folder in ("humanoid", "humanoid_leggings"):
            paths.add(f"assets/minecraft/textures/entity/equipment/{folder}/{worn}.png")
    for name in ("ender_pearl", "end_crystal", "totem_of_undying", "elytra", "elytra_broken", "crossbow_standby", "crossbow_arrow", "crossbow_firework"):
        paths.add(f"assets/minecraft/textures/item/{name}.png")
    for frame in range(3):
        paths.add(f"assets/minecraft/textures/item/crossbow_pulling_{frame}.png")
    for name in ("obsidian", "crying_obsidian", "glowstone", "respawn_anchor_bottom", "respawn_anchor_top", "respawn_anchor_top_off"):
        paths.add(f"assets/minecraft/textures/block/{name}.png")
    for charge in range(5):
        paths.add(f"assets/minecraft/textures/block/respawn_anchor_side{charge}.png")
    paths.update({
        "assets/minecraft/textures/entity/shield_base.png",
        "assets/minecraft/textures/entity/shield_base_nopattern.png",
        "assets/minecraft/textures/entity/equipment/wings/elytra.png",
        "assets/minecraft/textures/entity/end_crystal/end_crystal.png",
    })
    return paths


def verify_archive(path: Path) -> None:
    errors: list[str] = []
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
        if len(names) != len(archive.namelist()):
            errors.append("duplicate archive entries")
        for missing in sorted(required_textures() - names):
            errors.append(f"missing required texture: {missing}")
        for required in ("pack.mcmeta", "pack.png"):
            if required not in names:
                errors.append(f"missing ZIP-root file: {required}")

        try:
            metadata = json.loads(archive.read("pack.mcmeta"))
            if metadata.get("pack", {}).get("pack_format") != 75:
                errors.append("pack_format must be 75 for Java 1.21.11")
            for field in ("min_format", "max_format"):
                if metadata.get("pack", {}).get(field) != 75:
                    errors.append(f"{field} must be 75 for the targeted resource format")
        except Exception as exc:  # validation should report every problem together
            errors.append(f"invalid pack.mcmeta: {exc}")

        png_names = [name for name in names if name.endswith(".png")]
        if len(png_names) < 70:
            errors.append(f"expected at least 70 PNGs, found {len(png_names)}")
        for name in png_names:
            try:
                with Image.open(io.BytesIO(archive.read(name))) as image:
                    image.verify()
                with Image.open(io.BytesIO(archive.read(name))) as image:
                    image.load()
            except Exception as exc:
                errors.append(f"invalid PNG {name}: {exc}")

        for name in sorted(n for n in names if n.endswith(".mcmeta")):
            try:
                json.loads(archive.read(name))
            except Exception as exc:
                errors.append(f"invalid JSON {name}: {exc}")

    if errors:
        raise SystemExit("Pack verification failed:\n- " + "\n- ".join(errors))
    print(f"Verified {path}: {len(png_names)} valid PNG files, correct ZIP root, pack format 75")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: verify_pack.py <resource-pack.zip>")
    verify_archive(Path(sys.argv[1]))
