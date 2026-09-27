#!/usr/bin/env python3
"""
generate_mining_drops.py

Regenerates MiningDrops.java (and the "no loot table" list for ItemAttainability)
straight from Minecraft's own generated loot table data, for any Minecraft version.

WHY THIS EXISTS
----------------
Hand-writing "what does breaking block X give you" for every block in the game is slow
and error-prone. Mojang already publishes this data generator output. This script reads
it directly instead of guessing.

HOW TO USE IT
-------------
1. Pick the Minecraft version you want (e.g. "1.20.1", "1.21.4", ...).
2. Clone the two branches of misode/mcmeta that carry that version's generated data:

     git clone --depth 1 --branch <version>-data       https://github.com/misode/mcmeta.git mcmeta-data
     git clone --depth 1 --branch <version>-summary    https://github.com/misode/mcmeta.git mcmeta-summary

   (misode/mcmeta mirrors Mojang's own `runData` output as git branches, one pair per
   released version — check https://github.com/misode/mcmeta/branches for the exact
   branch names if a version isn't there yet.)

3. Run this script, pointing it at both:

     python3 generate_mining_drops.py --data mcmeta-data --summary mcmeta-summary

4. It prints:
     - The full body of MiningDrops.java's static block (paste between the { } already
       there, or redirect with `> mining_drops_body.txt` and splice it in).
     - The list of blocks with NO loot table at all (candidates for ItemAttainability's
       UNOBTAINABLE entries) and blocks whose loot table shape this script doesn't
       recognise (it prints these separately so you can eyeball them by hand — leaves,
       crops, and a few other special cases are already handled via MANUAL_OVERRIDES
       below; add more there the same way if new ones show up in a later version).

WHAT IT UNDERSTANDS
--------------------
- A block that always drops the same single item regardless of tool.
- A block that only drops with Silk Touch, and gives a different item (or nothing)
  without it.
- A block gated by "alternatives": Silk Touch gives one item, everything else (including
  a plain hand or a shears-only case) gives another, or nothing.
- A "minecraft:set_count" function with a uniform min/max range on the non-Silk-Touch
  drop (glowstone, melon, sea lantern, ...) — averaged into a flat count. Fortune's
  extra rolls ("apply_bonus") are NOT modelled; this only captures the un-enchanted
  baseline, which is still far more accurate than assuming 1.

WHAT IT DOESN'T UNDERSTAND (and will list under "unrecognized" instead of guessing)
------------------------------------------------------------------------------------
- Loot tables with more than one pool, or more than one top-level entry in a pool
  (crops with age-based alternatives, gravel/nether gold ore's bonus-item pools, potted
  plants' two-pool structure, composter/decorated pot, ...).
- Anything using "minecraft:sequence" or other exotic entry types.
These are rare enough among plain blocks that hand-checking the couple dozen of them
(printed at the end) is faster and safer than trying to generalise further.
"""

import argparse
import json
import os


def strip_ns(name):
    return name.replace("minecraft:", "") if name else name


def is_silk_touch_condition(cond):
    if cond.get("condition") != "minecraft:match_tool":
        return False
    for ench in cond.get("predicate", {}).get("enchantments", []):
        if ench.get("enchantment") == "minecraft:silk_touch":
            return True
    return False


def is_shears_condition(cond):
    if cond.get("condition") != "minecraft:match_tool":
        return False
    items = cond.get("predicate", {}).get("items")
    return items in ("minecraft:shears", ["minecraft:shears"])


def has_condition(conditions, checker):
    return any(checker(c) for c in (conditions or []))


def extract_average_count(entry):
    """Reads an UNCONDITIONAL 'minecraft:set_count' function's uniform min/max off an
    entry, if present, and returns the rounded average. Returns 1 otherwise -- including
    when the only set_count present has its own 'conditions' (a block-state check, like
    a slab's 'type: double', a candle's 'candles: 4', or a bed's head/foot half). This
    schematic-parsing pipeline only knows a block's *name*, not its full state, so it
    can't tell which state a given block is in -- skipping the conditional case and
    defaulting to 1 matches the common single-item state, rather than silently assuming
    the doubled/maxed-out one every time."""
    for fn in entry.get("functions", []):
        if fn.get("function") == "minecraft:set_count" and "conditions" not in fn:
            count = fn.get("count")
            if isinstance(count, dict) and count.get("type") == "minecraft:uniform":
                lo, hi = count.get("min", 1), count.get("max", 1)
                return max(1, round((lo + hi) / 2))
            if isinstance(count, (int, float)):
                return max(1, round(count))
    return 1


def parse_loot_table(table):
    """Returns a dict: {normal_drop, normal_count, silk_drop, silk_count, shears_also_work}
    or None if the shape isn't one this script recognises."""
    pools = table.get("pools", [])
    if not pools:
        return {"normal_drop": None, "normal_count": 1, "silk_drop": None, "silk_count": 1,
                "shears_also_work": False}
    if len(pools) != 1:
        return None

    entries = pools[0].get("entries", [])
    if len(entries) != 1:
        return None
    entry = entries[0]

    if entry["type"] == "minecraft:item":
        name = strip_ns(entry["name"])
        count = extract_average_count(entry)
        pool_silk_only = has_condition(pools[0].get("conditions", []), is_silk_touch_condition)
        if pool_silk_only:
            return {"normal_drop": None, "normal_count": 1, "silk_drop": name,
                    "silk_count": count, "shears_also_work": False}
        return {"normal_drop": name, "normal_count": count, "silk_drop": name,
                "silk_count": count, "shears_also_work": False}

    if entry["type"] == "minecraft:alternatives":
        silk_drop = normal_drop = None
        silk_count = normal_count = 1
        shears_also_work = False

        for child in entry.get("children", []):
            if child.get("type") != "minecraft:item":
                return None
            name = strip_ns(child["name"])
            count = extract_average_count(child)
            conds = child.get("conditions", [])

            if has_condition(conds, is_silk_touch_condition):
                silk_drop, silk_count = name, count
            elif has_condition(conds, is_shears_condition):
                if normal_drop is None:
                    shears_also_work = True
                    silk_drop, silk_count = name, count  # shears gives the same result as Silk Touch
            elif normal_drop is None:
                normal_drop, normal_count = name, count

        if silk_drop is None and normal_drop is not None:
            silk_drop, silk_count = normal_drop, normal_count

        return {"normal_drop": normal_drop, "normal_count": normal_count,
                "silk_drop": silk_drop, "silk_count": silk_count,
                "shears_also_work": shears_also_work}

    return None


def find_loot_file(loot_dir, block):
    """Direct match, or the handful of naming patterns vanilla uses when a block shares
    another block's loot table (wall variants, attached stems, potted-style suffixes)."""
    direct = os.path.join(loot_dir, block + ".json")
    if os.path.isfile(direct):
        return direct

    candidates = []
    if "_wall_" in block:
        candidates.append(block.replace("_wall_", "_", 1))
    if block.startswith("wall_"):
        candidates.append(block[len("wall_"):])
    if block.startswith("attached_"):
        candidates.append(block[len("attached_"):])
    for suffix, replacement in [("_wall_sign", "_sign"), ("_wall_banner", "_banner"),
                                 ("_wall_head", "_head"), ("_wall_skull", "_skull"),
                                 ("_plant", "")]:
        if block.endswith(suffix):
            candidates.append(block[:-len(suffix)] + replacement)

    for candidate in candidates:
        path = os.path.join(loot_dir, candidate + ".json")
        if os.path.isfile(path):
            return path
    return None


# Loot tables this script can't generically parse (see the module docstring). Extend this
# for whatever a new Minecraft version's "unrecognized" list turns up.
MANUAL_OVERRIDES = {
    "tall_grass": {"normal_drop": None, "normal_count": 1, "silk_drop": "grass", "silk_count": 2, "shears_also_work": True},
    "large_fern": {"normal_drop": None, "normal_count": 1, "silk_drop": "fern", "silk_count": 2, "shears_also_work": True},
    "snow": {"normal_drop": "snowball", "normal_count": 4, "silk_drop": "snow", "silk_count": 1, "shears_also_work": False},
    "carrots": {"normal_drop": "carrot", "normal_count": 1, "silk_drop": "carrot", "silk_count": 1, "shears_also_work": False},
    "potatoes": {"normal_drop": "potato", "normal_count": 1, "silk_drop": "potato", "silk_count": 1, "shears_also_work": False},
    "beetroots": {"normal_drop": "beetroot", "normal_count": 1, "silk_drop": "beetroot", "silk_count": 1, "shears_also_work": False},
    "sweet_berry_bush": {"normal_drop": "sweet_berries", "normal_count": 2, "silk_drop": "sweet_berries", "silk_count": 2, "shears_also_work": False},
    "amethyst_cluster": {"normal_drop": "amethyst_shard", "normal_count": 4, "silk_drop": "amethyst_cluster", "silk_count": 1, "shears_also_work": False},
}
for _leaf in ["acacia_leaves", "azalea_leaves", "birch_leaves", "cherry_leaves", "dark_oak_leaves",
              "flowering_azalea_leaves", "jungle_leaves", "oak_leaves", "spruce_leaves", "mangrove_leaves"]:
    MANUAL_OVERRIDES[_leaf] = {"normal_drop": None, "normal_count": 1, "silk_drop": _leaf,
                               "silk_count": 1, "shears_also_work": True}

# Every potted_X block: breaking it gives BOTH a flower_pot AND the plant it held.
# (block name -> plain item id of the plant; almost always the block name minus "potted_")
POTTED_PLANTS = {
    "potted_acacia_sapling": "acacia_sapling", "potted_allium": "allium",
    "potted_azalea_bush": "azalea", "potted_azure_bluet": "azure_bluet",
    "potted_bamboo": "bamboo", "potted_birch_sapling": "birch_sapling",
    "potted_blue_orchid": "blue_orchid", "potted_brown_mushroom": "brown_mushroom",
    "potted_cactus": "cactus", "potted_cherry_sapling": "cherry_sapling",
    "potted_cornflower": "cornflower", "potted_crimson_fungus": "crimson_fungus",
    "potted_crimson_roots": "crimson_roots", "potted_dandelion": "dandelion",
    "potted_dark_oak_sapling": "dark_oak_sapling", "potted_dead_bush": "dead_bush",
    "potted_fern": "fern", "potted_flowering_azalea_bush": "flowering_azalea",
    "potted_jungle_sapling": "jungle_sapling", "potted_lily_of_the_valley": "lily_of_the_valley",
    "potted_mangrove_propagule": "mangrove_propagule", "potted_oak_sapling": "oak_sapling",
    "potted_orange_tulip": "orange_tulip", "potted_oxeye_daisy": "oxeye_daisy",
    "potted_pink_tulip": "pink_tulip", "potted_poppy": "poppy",
    "potted_red_mushroom": "red_mushroom", "potted_red_tulip": "red_tulip",
    "potted_spruce_sapling": "spruce_sapling", "potted_torchflower": "torchflower",
    "potted_warped_fungus": "warped_fungus", "potted_warped_roots": "warped_roots",
    "potted_white_tulip": "white_tulip", "potted_wither_rose": "wither_rose",
}


def java_literal(value):
    return "null" if value is None else '"%s"' % value


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--data", required=True, help="Path to the cloned <version>-data branch")
    parser.add_argument("--summary", required=True, help="Path to the cloned <version>-summary branch")
    args = parser.parse_args()

    loot_dir = os.path.join(args.data, "data", "minecraft", "loot_tables", "blocks")
    registry_file = os.path.join(args.summary, "registries", "data.json")

    blocks = sorted(json.load(open(registry_file))["block"])

    parsed, no_table, unrecognized = {}, [], []
    for block in blocks:
        if block in POTTED_PLANTS:
            continue  # handled separately, below
        path = find_loot_file(loot_dir, block)
        if path is None:
            no_table.append(block)
            continue
        result = parse_loot_table(json.load(open(path)))
        if result is None:
            unrecognized.append(block)
            continue
        parsed[block] = result

    unhandled_unrecognized = [b for b in unrecognized if b not in MANUAL_OVERRIDES]
    parsed.update(MANUAL_OVERRIDES)

    # Only emit an entry where the result actually differs from "drops itself, count 1,
    # either way" -- that's already the default when a block has no MiningDrops entry.
    needed = {
        b: r for b, r in parsed.items()
        if r["normal_drop"] != b or r["silk_drop"] != b
        or r["normal_count"] != 1 or r["silk_count"] != 1
    }

    print("=" * 70)
    print(f"{len(blocks)} blocks total, {len(needed)} need a MiningDrops entry,")
    print(f"{len(no_table)} have no loot table at all, {len(unhandled_unrecognized)} unrecognized.")
    print("=" * 70)
    print()
    print("---- MiningDrops.java static block (paste the lines you want) ----")
    print()
    for block in sorted(needed):
        r = needed[block]
        print(f'        drops.put("{block}", new MiningDrop('
              f'{java_literal(r["normal_drop"])}, {r["normal_count"]}, '
              f'{java_literal(r["silk_drop"])}, {r["silk_count"]}, '
              f'{str(r["shears_also_work"]).lower()}));')

    print()
    print("---- Potted plants (drop BOTH flower_pot and the plant \u2014 handle in MiningResolver, not MiningDrops) ----")
    for block, plant in sorted(POTTED_PLANTS.items()):
        print(f'        POTTED_PLANTS.put("{block}", "{plant}");')

    print()
    print("---- No loot table at all (candidates for ItemAttainability.UNOBTAINABLE) ----")
    print(sorted(no_table))

    print()
    print("---- Unrecognized shape, not already covered by MANUAL_OVERRIDES (check by hand) ----")
    print(sorted(unhandled_unrecognized))


if __name__ == "__main__":
    main()