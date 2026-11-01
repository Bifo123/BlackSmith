#!/usr/bin/env python3
"""Gera texturas PROVISORIAS da Fase 5 (chapa, barra, pregos em tons de cinza para tingimento; foles).

Uso: python3 tools/gen_part_assets.py   (substitua por arte final quando houver)
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_textures import ROOT, blank, shade, write_png  # noqa: E402

GRAY = (235, 235, 235)


def plate():
    px = blank()
    for y in range(4, 12):
        for x in range(2, 14):
            edge = y in (4, 11) or x in (2, 13)
            px[y][x] = shade(GRAY, 0.6 if edge else (1.05 if y < 6 else 0.9))
    return px


def rod():
    px = blank()
    for i in range(2, 14):
        for dx in (0, 1):
            x, y = i + dx, 15 - i
            if 0 <= x < 16:
                px[y][x] = shade(GRAY, 0.6 if dx == 1 else 1.1)
    return px


def nails():
    px = blank()
    for cx, top in ((4, 3), (8, 5), (11, 4)):
        for y in range(top, top + 8):
            px[y][cx] = shade(GRAY, 0.9)
        px[top][cx - 1] = shade(GRAY, 1.1)
        px[top][cx] = shade(GRAY, 1.1)
        px[top][cx + 1] = shade(GRAY, 1.1)
    return px


def bellows():
    px = blank()
    wood, leather = (120, 84, 48), (150, 96, 56)
    for y in range(4, 12):
        for x in range(3, 11):
            edge = y in (4, 11) or x in (3, 10)
            px[y][x] = shade(leather, 0.6 if edge else 1.0)
    for y in range(6, 10):
        for x in range(10, 15):
            px[y][x] = shade(wood, 0.7 if y in (6, 9) else 1.0)
    for y in range(3, 13):
        px[y][2] = shade(wood, 0.8)
    return px


def cloth():
    px = blank()
    for y in range(3, 13):
        for x in range(3, 13):
            px[y][x] = shade((225, 215, 190), 0.85 if (x + y) % 4 == 0 else 1.0)
    for x in range(3, 13):
        px[3][x] = shade((225, 215, 190), 0.6)
        px[12][x] = shade((225, 215, 190), 0.6)
    return px


def metal_file():
    px = blank()
    for i in range(2, 11):
        for dx in (0, 1, 2):
            x, y = i + dx, 12 - i
            if 0 <= x < 16 and 0 <= y < 16:
                px[y][x] = shade((170, 175, 185), 0.7 + 0.15 * ((i + dx) % 2))
    for i in range(10, 14):
        px[15 - i + 1][i] = shade((120, 84, 48), 1.0)
        px[15 - i + 1][i + 1] = shade((120, 84, 48), 0.8)
    return px


def chisel():
    px = blank()
    for i in range(3, 12):
        px[15 - i][i] = shade((190, 195, 205), 1.0)
        px[15 - i][i + 1] = shade((190, 195, 205), 0.7)
    for i in range(10, 14):
        px[15 - i + 1][i] = shade((120, 84, 48), 1.0)
    px[13][2] = shade((230, 235, 240), 1.2)
    return px


def rune_glyph():
    px = blank()
    for y in range(2, 14):
        for x in range(3, 13):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 31:
                px[y][x] = shade((235, 235, 235), 0.55)
    for y in range(4, 12):
        px[y][7] = shade((255, 255, 255), 1.0)
    for x in range(5, 11):
        px[6][x] = shade((255, 255, 255), 1.0)
    px[9][5] = px[9][9] = shade((255, 255, 255), 1.0)
    return px


def main():
    write_png(ROOT / "item/polishing_cloth.png", cloth())
    write_png(ROOT / "item/metal_file.png", metal_file())
    write_png(ROOT / "item/engraving_chisel.png", chisel())
    write_png(ROOT / "item/rune.png", rune_glyph())
    write_png(ROOT / "item/metal_plate.png", plate())
    write_png(ROOT / "item/metal_rod.png", rod())
    write_png(ROOT / "item/metal_nails.png", nails())
    write_png(ROOT / "item/bellows.png", bellows())
    print("texturas da fase 5 geradas")


if __name__ == "__main__":
    main()
