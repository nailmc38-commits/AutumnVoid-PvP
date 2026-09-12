#!/usr/bin/env python3
"""Generate AutumnVoid PvP textures from an official Minecraft 1.21.11 client jar.

The vanilla files are used only as shape/UV masks so every generated PNG stays
pixel-perfect and lands at the exact resource path expected by the game.
"""

from __future__ import annotations

import argparse
import io
import math
import shutil
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


PACK_FORMAT = 75
ARMOR_MATERIALS = (
    "leather",
    "chainmail",
    "iron",
    "golden",
    "copper",
    "diamond",
    "netherite",
)
ARMOR_PIECES = ("helmet", "chestplate", "leggings", "boots")
TOOL_MATERIALS = ("wooden", "stone", "iron", "golden", "copper", "diamond", "netherite")
TOOLS = ("sword", "axe", "pickaxe")


PALETTES: dict[str, tuple[str, ...]] = {
    "leather": ("#24130e", "#54271a", "#8a4523", "#c06a2d", "#e7a54c"),
    "chainmail": ("#241815", "#56352c", "#92533a", "#cf7a42", "#f0ad62"),
    "iron": ("#202124", "#4b4c4a", "#77766d", "#a45e35", "#e1914b"),
    "gold": ("#51200f", "#9a4618", "#d47b20", "#f2ad32", "#ffe072"),
    "copper": ("#382018", "#7e3822", "#bb6032", "#9a8241", "#8fad70"),
    "diamond": ("#3a0c0b", "#7d1c12", "#be4318", "#ed7b22", "#ffd25d"),
    "netherite": ("#09070e", "#1a1024", "#351747", "#672978", "#b252c2"),
    "turtle": ("#172014", "#344222", "#65723a", "#a08038", "#d2aa55"),
    "wood": ("#25140d", "#5a2d17", "#8f4a20", "#c4722b", "#e9a94a"),
    "stone": ("#211d1b", "#51443e", "#75645a", "#996b46", "#d49a58"),
    "autumn": ("#260c0b", "#6e1c11", "#ad3516", "#e8731f", "#ffd05b"),
    "void": ("#08060d", "#21102b", "#48195c", "#7f2f91", "#c55ad2"),
    "ember": ("#18090a", "#5f1710", "#a93614", "#ed7420", "#ffd466"),
    "olive": ("#17160c", "#3f3e1a", "#6b6e2b", "#9e8a39", "#dab85c"),
    "warm_steel": ("#171719", "#3b3c3e", "#696a67", "#9b6545", "#d38a4d"),
}


def rgb(hex_color: str) -> tuple[int, int, int]:
    value = hex_color.lstrip("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def palette_color(palette_name: str, value: float) -> tuple[int, int, int]:
    colors = [rgb(color) for color in PALETTES[palette_name]]
    value = max(0.0, min(1.0, value))
    scaled = value * (len(colors) - 1)
    left = min(int(scaled), len(colors) - 2)
    return mix(colors[left], colors[left + 1], scaled - left)


def luminance(pixel: tuple[int, int, int, int]) -> float:
    r, g, b, _ = pixel
    return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0


def is_alpha_edge(alpha: Image.Image, x: int, y: int) -> bool:
    if alpha.getpixel((x, y)) == 0:
        return False
    width, height = alpha.size
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nx, ny = x + dx, y + dy
        if nx < 0 or ny < 0 or nx >= width or ny >= height or alpha.getpixel((nx, ny)) == 0:
            return True
    return False


def recolor(source: Image.Image, palette_name: str, style: str | None = None) -> Image.Image:
    image = source.convert("RGBA")
    alpha = image.getchannel("A")
    out = Image.new("RGBA", image.size)
    src = image.load()
    dst = out.load()
    width, height = image.size

    for y in range(height):
        for x in range(width):
            original = src[x, y]
            if original[3] == 0:
                continue
            value = luminance(original)
            # A little extra contrast keeps 16x textures crisp in inventory.
            value = 0.5 + (value - 0.5) * 1.24
            color = palette_color(palette_name, value)
            edge = is_alpha_edge(alpha, x, y)

            if edge:
                if style in {"iron", "leather", "chainmail"} and (x + y) % 4 == 0:
                    color = rgb("#c86e36")
                elif style == "diamond" and (x + 2 * y) % 5 == 0:
                    color = rgb("#ffbf45")
                elif style == "netherite" and (2 * x + y) % 5 == 0:
                    color = rgb("#8e3aa5")
                else:
                    color = mix(color, (8, 7, 10), 0.38)
            elif style == "diamond" and (x - y) % 11 == 0 and value > 0.42:
                color = mix(color, rgb("#ffd66c"), 0.58)
            elif style == "netherite" and (3 * x + 5 * y) % 23 == 0 and value > 0.28:
                color = rgb("#c96ee0")
            elif style == "copper" and (x + 3 * y) % 19 == 0:
                color = mix(color, rgb("#87a66a"), 0.58)
            elif style in {"leather", "chainmail"} and (x + y) % 17 == 0:
                color = mix(color, rgb("#e7a14e"), 0.44)

            dst[x, y] = (*color, original[3])
    return out


def recolor_tool(source: Image.Image, material: str) -> Image.Image:
    style = material.replace("golden", "gold")
    palette = {
        "wooden": "wood",
        "stone": "stone",
        "iron": "iron",
        "golden": "gold",
        "copper": "copper",
        "diamond": "diamond",
        "netherite": "netherite",
    }[material]
    image = source.convert("RGBA")
    alpha = image.getchannel("A")
    out = Image.new("RGBA", image.size)
    src = image.load()
    dst = out.load()

    for y in range(image.height):
        for x in range(image.width):
            original = src[x, y]
            if original[3] == 0:
                continue
            r, g, b, a = original
            value = max(0.0, min(1.0, 0.5 + (luminance(original) - 0.5) * 1.3))
            looks_like_handle = luminance(original) < 0.50 and r > b + 15 and r >= g
            color = palette_color("wood" if looks_like_handle else palette, value)
            if is_alpha_edge(alpha, x, y):
                color = mix(color, (8, 7, 9), 0.42)
            elif style == "diamond" and (x - y) % 7 == 0:
                color = mix(color, rgb("#ffe06b"), 0.62)
            elif style == "netherite" and (x + 2 * y) % 13 == 0:
                color = mix(color, rgb("#b957cc"), 0.72)
            dst[x, y] = (*color, a)
    return out


def transform_totem(source: Image.Image) -> Image.Image:
    image = recolor(source, "wood", "leather")
    src = source.convert("RGBA")
    dst = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = src.getpixel((x, y))
            if a and g > r * 1.08 and g > b * 1.05:
                value = luminance((r, g, b, a))
                dst[x, y] = (*palette_color("olive", value), a)
    return image


def material_for_path(path: str) -> tuple[str, str]:
    name = Path(path).stem
    if "netherite" in name:
        return "netherite", "netherite"
    if "diamond" in name:
        return "diamond", "diamond"
    if "copper" in name:
        return "copper", "copper"
    if "gold" in name:
        return "gold", "gold"
    if "iron" in name:
        return "iron", "iron"
    if "chainmail" in name:
        return "chainmail", "chainmail"
    if "leather" in name:
        return "leather", "leather"
    if "turtle" in name or "scute" in name:
        return "turtle", "turtle"
    return "autumn", "autumn"


def style_for_special(path: str) -> tuple[str, str]:
    if "ender_pearl" in path:
        return "autumn", "diamond"
    if "end_crystal" in path:
        return "ember", "diamond"
    if "obsidian" in path and "crying" not in path:
        return "void", "netherite"
    if "crying_obsidian" in path:
        return "ember", "netherite"
    if "respawn_anchor" in path:
        return "ember", "netherite"
    if "glowstone" in path:
        return "gold", "gold"
    if "elytra" in path:
        return "autumn", "diamond"
    if "shield" in path:
        return "wood", "leather"
    if "crossbow" in path or "/bow" in path:
        return "wood", "leather"
    if "mace" in path:
        return "warm_steel", "iron"
    if "glint" in path:
        return "gold", "diamond"
    if "golden_apple" in path:
        return "gold", "gold"
    if "experience_bottle" in path:
        return "olive", "copper"
    if "firework_rocket" in path or path.endswith("/arrow.png"):
        return "autumn", "diamond"
    return "autumn", "diamond"


def selected_assets(names: set[str]) -> list[str]:
    selected: set[str] = set()

    for material in ARMOR_MATERIALS:
        for piece in ARMOR_PIECES:
            path = f"assets/minecraft/textures/item/{material}_{piece}.png"
            if path in names:
                selected.add(path)
            overlay = f"assets/minecraft/textures/item/{material}_{piece}_overlay.png"
            if overlay in names:
                selected.add(overlay)

    turtle = "assets/minecraft/textures/item/turtle_helmet.png"
    if turtle in names:
        selected.add(turtle)

    for material in TOOL_MATERIALS:
        for tool in TOOLS:
            path = f"assets/minecraft/textures/item/{material}_{tool}.png"
            if path in names:
                selected.add(path)

    item_names = (
        "arrow",
        "bow",
        "bow_pulling_0",
        "bow_pulling_1",
        "bow_pulling_2",
        "crossbow_arrow",
        "crossbow_firework",
        "crossbow_pulling_0",
        "crossbow_pulling_1",
        "crossbow_pulling_2",
        "crossbow_standby",
        "elytra",
        "elytra_broken",
        "end_crystal",
        "ender_pearl",
        "experience_bottle",
        "firework_rocket",
        "golden_apple",
        "mace",
        "totem_of_undying",
    )
    for name in item_names:
        path = f"assets/minecraft/textures/item/{name}.png"
        if path in names:
            selected.add(path)

    block_names = (
        "obsidian",
        "crying_obsidian",
        "glowstone",
        "respawn_anchor_bottom",
        "respawn_anchor_side0",
        "respawn_anchor_side1",
        "respawn_anchor_side2",
        "respawn_anchor_side3",
        "respawn_anchor_side4",
        "respawn_anchor_top",
        "respawn_anchor_top_off",
    )
    for name in block_names:
        selected.add(f"assets/minecraft/textures/block/{name}.png")

    entity_paths = (
        "assets/minecraft/textures/entity/end_crystal/end_crystal.png",
        "assets/minecraft/textures/entity/end_crystal/end_crystal_beam.png",
        "assets/minecraft/textures/entity/shield_base.png",
        "assets/minecraft/textures/entity/shield_base_nopattern.png",
        "assets/minecraft/textures/entity/shield/base.png",
        "assets/minecraft/textures/entity/equipment/wings/elytra.png",
    )
    selected.update(path for path in entity_paths if path in names)

    worn_materials = ("leather", "chainmail", "iron", "gold", "copper", "diamond", "netherite")
    for folder in ("humanoid", "humanoid_leggings"):
        for material in worn_materials:
            selected.add(f"assets/minecraft/textures/entity/equipment/{folder}/{material}.png")
        selected.add(f"assets/minecraft/textures/entity/equipment/{folder}/leather_overlay.png")
    selected.add("assets/minecraft/textures/entity/equipment/humanoid/turtle_scute.png")

    selected.update(
        {
            "assets/minecraft/textures/misc/enchanted_glint_item.png",
            "assets/minecraft/textures/misc/enchanted_glint_armor.png",
        }
    )
    return sorted(path for path in selected if path in names)


def make_pack_icon(output_root: Path) -> None:
    size = 32
    icon = Image.new("RGB", (size, size), rgb("#100814"))
    draw = ImageDraw.Draw(icon)
    for y in range(size):
        t = y / (size - 1)
        draw.line((0, y, size, y), fill=mix(rgb("#431326"), rgb("#120819"), t))

    # Autumn moon.
    draw.ellipse((7, 4, 25, 22), fill=rgb("#d65b1d"), outline=rgb("#ffbd42"), width=1)
    draw.rectangle((8, 14, 24, 22), fill=rgb("#8a2816"))
    # Void obsidian pedestal.
    draw.polygon(((5, 26), (10, 22), (22, 22), (27, 26), (24, 30), (8, 30)), fill=rgb("#24102e"))
    draw.line(((7, 27), (13, 24), (18, 27), (24, 24)), fill=rgb("#8f3aa2"), width=1)
    # End crystal silhouette with ember core.
    draw.polygon(((16, 6), (22, 12), (16, 20), (10, 12)), fill=rgb("#f08a28"), outline=rgb("#ffd669"))
    draw.polygon(((16, 9), (19, 12), (16, 17), (13, 12)), fill=rgb("#fff0a1"))
    # Small falling leaves.
    for x, y, color in ((4, 8, "#c83a18"), (27, 9, "#ee8b27"), (5, 18, "#d9aa38"), (27, 19, "#8a6f2d")):
        draw.rectangle((x, y, x + 1, y + 1), fill=rgb(color))

    icon.resize((128, 128), Image.Resampling.NEAREST).save(output_root / "pack.png")


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    names = (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
    )
    for name in names:
        if Path(name).exists():
            return ImageFont.truetype(name, size)
    return ImageFont.load_default()


def paste_icon(canvas: Image.Image, path: Path, x: int, y: int, scale: int = 5) -> None:
    image = Image.open(path).convert("RGBA")
    image = image.resize((image.width * scale, image.height * scale), Image.Resampling.NEAREST)
    canvas.alpha_composite(image, (x, y))


def make_preview(output_root: Path, preview_path: Path) -> None:
    width, height = 1500, 940
    canvas = Image.new("RGBA", (width, height), rgb("#100d13") + (255,))
    draw = ImageDraw.Draw(canvas)
    title_font = load_font(52, bold=True)
    heading_font = load_font(27, bold=True)
    label_font = load_font(19, bold=True)
    small_font = load_font(15)
    muted = rgb("#c9b9aa")
    gold = rgb("#ffbd4a")

    draw.rectangle((0, 0, width, 105), fill=rgb("#271126"))
    draw.text((42, 22), "AUTUMNVOID PvP", font=title_font, fill=gold)
    draw.text((650, 42), "Java 1.21.11  •  16x  •  CPvP", font=heading_font, fill=rgb("#e5c7b1"))

    panels = ((30, 130, 910, 520), (935, 130, 1470, 520), (30, 545, 1470, 910))
    for box in panels:
        draw.rounded_rectangle(box, radius=18, fill=rgb("#1b171d"), outline=rgb("#49303e"), width=2)

    draw.text((55, 150), "Armor tiers — every set has its own identity", font=heading_font, fill=gold)
    armor_rows = (
        ("Leather", "leather"),
        ("Chainmail", "chainmail"),
        ("Iron", "iron"),
        ("Gold", "golden"),
        ("Copper", "copper"),
        ("Diamond — full autumn", "diamond"),
        ("Netherite — void purple", "netherite"),
    )
    for row, (label, material) in enumerate(armor_rows):
        y = 195 + row * 43
        draw.text((58, y + 8), label, font=label_font, fill=rgb("#efe0d3"))
        for col, piece in enumerate(ARMOR_PIECES):
            path = output_root / f"assets/minecraft/textures/item/{material}_{piece}.png"
            if path.exists():
                paste_icon(canvas, path, 340 + col * 92, y, 2)

    draw.text((958, 150), "Crystal PvP essentials", font=heading_font, fill=gold)
    essentials = (
        ("netherite_sword", "Void sword"),
        ("diamond_sword", "Autumn sword"),
        ("end_crystal", "Crystal"),
        ("ender_pearl", "Pearl"),
        ("totem_of_undying", "Totem"),
        ("golden_apple", "Gapple"),
        ("crossbow_standby", "Crossbow"),
        ("elytra", "Elytra"),
    )
    for index, (name, label) in enumerate(essentials):
        col, row = index % 4, index // 4
        x, y = 965 + col * 124, 207 + row * 145
        paste_icon(canvas, output_root / f"assets/minecraft/textures/item/{name}.png", x + 18, y, 5)
        tw = draw.textbbox((0, 0), label, font=small_font)[2]
        draw.text((x + 58 - tw / 2, y + 89), label, font=small_font, fill=muted)

    draw.text((55, 565), "World textures + combat gear", font=heading_font, fill=gold)
    blocks = (
        ("obsidian", "Obsidian"),
        ("crying_obsidian", "Crying obsidian"),
        ("respawn_anchor_side4", "Charged anchor"),
        ("respawn_anchor_top_off", "Anchor top"),
        ("glowstone", "Glowstone"),
    )
    for index, (name, label) in enumerate(blocks):
        x = 65 + index * 205
        y = 625
        paste_icon(canvas, output_root / f"assets/minecraft/textures/block/{name}.png", x, y, 8)
        draw.text((x, y + 139), label, font=small_font, fill=muted)

    weapon_names = ("netherite_axe", "netherite_pickaxe", "mace", "bow", "firework_rocket")
    for index, name in enumerate(weapon_names):
        x = 1080 + (index % 3) * 120
        y = 610 + (index // 3) * 135
        paste_icon(canvas, output_root / f"assets/minecraft/textures/item/{name}.png", x, y, 5)

    draw.text((55, 845), "Burnt orange • harvest gold • deep red • moss olive • void violet", font=label_font, fill=rgb("#d4a879"))
    preview_path.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(preview_path, quality=95)


def generate(client_jar: Path, output_root: Path) -> int:
    if not client_jar.exists():
        raise SystemExit(f"Client jar not found: {client_jar}")

    generated = 0
    with zipfile.ZipFile(client_jar) as jar:
        names = set(jar.namelist())
        for path in selected_assets(names):
            with jar.open(path) as file:
                source = Image.open(io.BytesIO(file.read())).convert("RGBA")

            filename = Path(path).stem
            if filename == "totem_of_undying":
                result = transform_totem(source)
            elif any(f"{material}_" in filename for material in TOOL_MATERIALS) and any(
                filename.endswith(f"_{tool}") for tool in TOOLS
            ):
                material = next(material for material in TOOL_MATERIALS if filename.startswith(f"{material}_"))
                result = recolor_tool(source, material)
            elif "/item/" in path and any(material in filename for material in ARMOR_MATERIALS):
                palette, style = material_for_path(path)
                result = recolor(source, palette, style)
            elif "/equipment/humanoid" in path:
                palette, style = material_for_path(path)
                result = recolor(source, palette, style)
            else:
                palette, style = style_for_special(path)
                result = recolor(source, palette, style)

            destination = output_root / path
            destination.parent.mkdir(parents=True, exist_ok=True)
            result.save(destination, optimize=True)
            generated += 1

        for metadata in (
            "assets/minecraft/textures/block/respawn_anchor_top.png.mcmeta",
            "assets/minecraft/textures/misc/enchanted_glint_item.png.mcmeta",
            "assets/minecraft/textures/misc/enchanted_glint_armor.png.mcmeta",
        ):
            if metadata in names:
                destination = output_root / metadata
                destination.parent.mkdir(parents=True, exist_ok=True)
                with jar.open(metadata) as src, destination.open("wb") as dst:
                    shutil.copyfileobj(src, dst)

    make_pack_icon(output_root)
    make_preview(output_root, output_root / "preview/pack-preview.png")
    return generated


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--client-jar", required=True, type=Path)
    parser.add_argument("--output", default=Path.cwd(), type=Path)
    args = parser.parse_args()
    count = generate(args.client_jar, args.output.resolve())
    print(f"Generated {count} texture PNGs in {args.output.resolve()}")


if __name__ == "__main__":
    main()
