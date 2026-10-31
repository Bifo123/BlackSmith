#!/usr/bin/env python3
"""Gera os assets e dados repetitivos das 10 estacoes (Fase 3).

Saida: blockstates, modelos de bloco/item, texturas PROVISORIAS, loot tables, receitas de
crafting e tags de mineracao. Rode novamente apos mudar a tabela STATIONS.
Uso: python3 tools/gen_station_assets.py
"""
import json
import random
from pathlib import Path

from gen_textures import ROOT as TEXTURES, shade, write_png, SIZE

RES = Path(__file__).resolve().parent.parent / "src/main/resources"
ASSETS = RES / "assets/forjasuprema"
DATA = RES / "data"
MOD = "forjasuprema"

# id -> (tipo de material, cor base, cor de destaque)
STATIONS = {
    "apprentice_forge": ("stone", (110, 100, 95), (200, 110, 40)),
    "blacksmith_forge": ("stone", (120, 70, 60), (255, 140, 40)),
    "advanced_forge": ("stone", (95, 90, 100), (255, 150, 50)),
    "master_forge": ("stone", (80, 80, 90), (255, 190, 70)),
    "legendary_forge": ("stone", (60, 50, 70), (255, 230, 120)),
    "blacksmith_anvil": ("iron", (70, 72, 78), (150, 155, 165)),
    "shaping_bench": ("wood", (140, 100, 60), (90, 90, 95)),
    "quench_tank": ("iron", (75, 80, 90), (50, 110, 200)),
    "heat_treatment_furnace": ("stone", (100, 95, 95), (230, 90, 30)),
    "polishing_station": ("wood", (150, 110, 70), (200, 200, 210)),
    "sharpening_stone": ("stone", (130, 130, 135), (80, 80, 85)),
    "engraving_bench": ("wood", (125, 90, 55), (210, 175, 70)),
    "alloy_station": ("stone", (95, 90, 100), (200, 140, 60)),
    "finishing_station": ("wood", (135, 95, 65), (160, 60, 50)),
}

# Forjas: tem estado aceso/apagado (modelo e textura de topo proprios para "lit").
FORGES = {"apprentice_forge", "blacksmith_forge", "advanced_forge", "master_forge", "legendary_forge"}

# id -> (padrao, chave de ingredientes). Ingredientes: item ("minecraft:x") ou tag ("#c:x").
# Receitas PROVISORIAS. Atencao: o aco so e produzido na forja de Mestre, entao as receitas das
# forjas avancada e de mestre nao podem exigir aco (usam ferro e bronze).
RECIPES = {
    "apprentice_forge": (["CCC", "CFC", "CCC"], {"C": "minecraft:cobblestone", "F": "minecraft:furnace"}),
    "blacksmith_forge": (["BBB", "BFB", "CCC"], {"B": "minecraft:brick", "F": "minecraft:furnace", "C": "minecraft:cobblestone"}),
    "advanced_forge": (["ZZZ", "ZFZ", "III"], {"Z": "#c:ingots/bronze", "F": "minecraft:blast_furnace", "I": "#c:ingots/iron"}),
    "master_forge": (["RIR", "ZFZ", "III"], {"R": "minecraft:blaze_rod", "I": "#c:ingots/iron", "Z": "#c:ingots/bronze", "F": "minecraft:blast_furnace"}),
    "legendary_forge": ([" S ", "NFN", "DDD"], {"S": "minecraft:nether_star", "N": "minecraft:netherite_ingot", "D": "minecraft:diamond_block", "F": "minecraft:blast_furnace"}),
    "blacksmith_anvil": (["III", " X ", "III"], {"I": "#c:ingots/iron", "X": "minecraft:iron_block"}),
    "shaping_bench": (["PPP", "L L", "L L"], {"P": "#minecraft:planks", "L": "#minecraft:logs"}),
    "quench_tank": (["I I", "IWI", "III"], {"I": "#c:ingots/iron", "W": "minecraft:water_bucket"}),
    "heat_treatment_furnace": (["BBB", "BXB", "III"], {"B": "minecraft:brick", "X": "minecraft:blast_furnace", "I": "#c:ingots/iron"}),
    "polishing_station": (["FFF", "PPP", "P P"], {"F": "minecraft:flint", "P": "#minecraft:planks"}),
    "engraving_bench": ([" I ", "PPP", "P P"], {"I": "#c:ingots/iron", "P": "#minecraft:planks"}),
    "alloy_station": (["BBB", "BKB", "III"], {"B": "minecraft:brick", "K": "minecraft:cauldron", "I": "#c:ingots/iron"}),
    "finishing_station": (["LLL", "PPP", "P P"], {"L": "minecraft:leather", "P": "#minecraft:planks"}),
}
SHAPELESS = {
    "sharpening_stone": ["minecraft:grindstone", "#c:ingots/iron"],
}


def ingredient(spec):
    return {"tag": spec[1:]} if spec.startswith("#") else {"item": spec}


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def texture(base, accent, seed, top):
    rnd = random.Random(seed)
    px = [[shade(base, rnd.uniform(0.88, 1.08)) for _ in range(SIZE)] for _ in range(SIZE)]
    for i in range(SIZE):  # borda escura
        for j in (0, SIZE - 1):
            px[i][j] = shade(base, 0.6)
            px[j][i] = shade(base, 0.6)
    if top:  # retangulo central de destaque
        for y in range(4, 12):
            for x in range(4, 12):
                px[y][x] = shade(accent, rnd.uniform(0.85, 1.1))
    else:  # faixa horizontal de destaque
        for y in (7, 8):
            for x in range(2, 14):
                px[y][x] = shade(accent, rnd.uniform(0.85, 1.1))
    return px


def main():
    for index, (name, (kind, base, accent)) in enumerate(STATIONS.items()):
        write_png(TEXTURES / f"block/{name}_top.png", texture(base, accent, index * 2 + 1, True))
        write_png(TEXTURES / f"block/{name}_side.png", texture(base, accent, index * 2 + 2, False))

        dump(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
        dump(ASSETS / f"models/block/{name}.json", {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {
                "top": f"{MOD}:block/{name}_top",
                "bottom": f"{MOD}:block/{name}_top",
                "side": f"{MOD}:block/{name}_side",
            },
        })
        dump(ASSETS / f"models/item/{name}.json", {"parent": f"{MOD}:block/{name}"})
        dump(DATA / f"{MOD}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{
                "bonus_rolls": 0.0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                "rolls": 1.0,
            }],
            "random_sequence": f"{MOD}:blocks/{name}",
        })

        if name in FORGES:
            # Variante acesa: topo mais brilhante, modelo e blockstate com a propriedade "lit".
            bright = tuple(min(255, int(c * 1.3)) for c in accent)
            write_png(TEXTURES / f"block/{name}_top_lit.png", texture(base, bright, index * 2 + 101, True))
            dump(ASSETS / f"models/block/{name}_lit.json", {
                "parent": "minecraft:block/cube_bottom_top",
                "textures": {
                    "top": f"{MOD}:block/{name}_top_lit",
                    "bottom": f"{MOD}:block/{name}_top",
                    "side": f"{MOD}:block/{name}_side",
                },
            })
            dump(ASSETS / f"blockstates/{name}.json", {"variants": {
                "lit=false": {"model": f"{MOD}:block/{name}"},
                "lit=true": {"model": f"{MOD}:block/{name}_lit"},
            }})

    for name, (pattern, key) in RECIPES.items():
        dump(DATA / f"{MOD}/recipe/stations/{name}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "key": {k: ingredient(v) for k, v in key.items()},
            "pattern": pattern,
            "result": {"id": f"{MOD}:{name}", "count": 1},
        })
    for name, ingredients in SHAPELESS.items():
        dump(DATA / f"{MOD}/recipe/stations/{name}.json", {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [ingredient(i) for i in ingredients],
            "result": {"id": f"{MOD}:{name}", "count": 1},
        })

    pickaxe = [f"{MOD}:tin_ore"] + [f"{MOD}:{n}" for n, (k, _, _) in STATIONS.items() if k in ("stone", "iron")]
    axe = [f"{MOD}:{n}" for n, (k, _, _) in STATIONS.items() if k == "wood"]
    dump(DATA / "minecraft/tags/block/mineable/pickaxe.json", {"values": pickaxe})
    dump(DATA / "minecraft/tags/block/mineable/axe.json", {"values": axe})
    print("assets das estacoes gerados")


if __name__ == "__main__":
    main()
