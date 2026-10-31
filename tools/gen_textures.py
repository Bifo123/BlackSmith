#!/usr/bin/env python3
"""Gera texturas 16x16 PROVISORIAS (placeholders) para itens e blocos da Fase 2.

Sem dependencias externas. Substitua os PNGs gerados por arte final quando houver.
Uso: python3 tools/gen_textures.py
"""
import random
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/forjasuprema/textures"
SIZE = 16


def write_png(path, pixels):
    """pixels: lista de 16 linhas, cada uma com 16 tuplas RGBA."""
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in pixels)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def shade(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color) + (255,)


def blank():
    return [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]


def ingot(color):
    px = blank()
    for y in range(5, 12):
        start = 2 + (11 - y) // 2
        for x in range(start, start + 9):
            edge = y in (5, 11) or x in (start, start + 8)
            if edge:
                px[y][x] = shade(color, 0.55)
            elif y <= 6:
                px[y][x] = shade(color, 1.25)
            elif y >= 10:
                px[y][x] = shade(color, 0.8)
            else:
                px[y][x] = shade(color, 1.0)
    return px


def raw_chunk(color, seed):
    rnd = random.Random(seed)
    px = blank()
    cx, cy = 7.5, 8.0
    for y in range(SIZE):
        for x in range(SIZE):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d < 5.2 + rnd.uniform(-0.8, 0.8):
                px[y][x] = shade(color, rnd.uniform(0.75, 1.15))
    return px


def ore(speck, seed):
    rnd = random.Random(seed)
    px = []
    for _ in range(SIZE):
        px.append([shade((125, 125, 125), rnd.uniform(0.85, 1.1)) for _ in range(SIZE)])
    for _ in range(9):
        cx, cy = rnd.randint(1, 14), rnd.randint(1, 14)
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            x, y = min(cx + dx, 15), min(cy + dy, 15)
            px[y][x] = shade(speck, rnd.uniform(0.9, 1.15))
    return px


def main():
    write_png(ROOT / "item/tin_ingot.png", ingot((188, 200, 208)))
    write_png(ROOT / "item/bronze_ingot.png", ingot((192, 138, 62)))
    write_png(ROOT / "item/steel_ingot.png", ingot((125, 138, 150)))
    write_png(ROOT / "item/raw_tin.png", raw_chunk((150, 165, 175), 7))
    write_png(ROOT / "item/slag.png", raw_chunk((62, 56, 60), 3))
    write_png(ROOT / "block/tin_ore.png", ore((200, 212, 220), 11))
    print("texturas geradas em", ROOT)


if __name__ == "__main__":
    main()
