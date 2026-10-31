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


def main():
    write_png(ROOT / "item/metal_plate.png", plate())
    write_png(ROOT / "item/metal_rod.png", rod())
    write_png(ROOT / "item/metal_nails.png", nails())
    write_png(ROOT / "item/bellows.png", bellows())
    print("texturas da fase 5 geradas")


if __name__ == "__main__":
    main()
