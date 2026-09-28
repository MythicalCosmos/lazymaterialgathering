#!/usr/bin/env python3
"""
generate_biome_blocks.py

Builds the "which blocks generate in which biomes" dictionary (BiomeBlocks.java) straight
from Minecraft's own generated worldgen data, for any Minecraft version -- the same
approach as generate_mining_drops.py, and it reads the same misode/mcmeta data branch:

    git clone --depth 1 --branch <version>-data  https://github.com/misode/mcmeta.git mcmeta-data
    python3 generate_biome_blocks.py --data mcmeta-data --out BiomeBlocks.java

WHAT IT READS
-------------
1. Every biome's list of features (data/minecraft/worldgen/biome/*.json). Each feature is
   followed through its placed_feature and configured_feature files (including features
   nested inside other features, like a flower patch that places a flower) to collect every
   block it can place: ore veins, trees, flowers, disks of sand/clay/gravel, and so on.
2. The surface rules (worldgen/noise_settings/overworld|nether|end.json), which is where
   "what's the ground made of" lives (sand in deserts, mycelium in mushroom fields, ice in
   ice spikes, ...). Blocks under a "biome is X" condition are credited to those biomes;
   blocks not under any biome condition are credited to every biome in that dimension.

WHAT COMES OUT
--------------
- A block that shows up in nearly every biome of a dimension (iron ore, diamond ore, stone,
  dirt ...) is "universal" there: it doesn't matter which biome you're in, so there's
  nothing for biome-based pathing to do. This is the honest answer for most ores -- in
  vanilla, ore placement depends on height, not biome.
- Every other block gets the list of biomes it appears in (emerald ore: only mountain-type
  biomes; sand: deserts and beaches; and so on) -- these are the ones worth routing to.

KNOWN GAPS (it will not pretend otherwise)
------------------------------------------
- Structures (villages, mineshafts, strongholds, ...) aren't biome features; not covered.
- Features that place blocks from a structure template (fossils, ...) aren't followed.
- Surface rules also depend on depth, height and noise, not just biome, so "appears in this
  biome" is a guarantee that the block *can* generate there, not that it's common.
"""

import argparse
import json
import os
import sys
from collections import defaultdict

UNIVERSAL_THRESHOLD = 0.85   # present in this share of a dimension's biomes => "universal"


def strip(name):
    return name.split(":", 1)[1] if isinstance(name, str) and ":" in name else name


class Data:
    def __init__(self, root):
        self.base = os.path.join(root, "data", "minecraft")
        self.biomes = sorted(f[:-5] for f in os.listdir(os.path.join(self.base, "worldgen", "biome")) if f.endswith(".json"))
        self._cache = {}

    def load(self, *parts):
        path = os.path.join(self.base, *parts)
        if path not in self._cache:
            self._cache[path] = json.load(open(path)) if os.path.isfile(path) else None
        return self._cache[path]

    def placed(self, name):
        return self.load("worldgen", "placed_feature", strip(name) + ".json")

    def configured(self, name):
        return self.load("worldgen", "configured_feature", strip(name) + ".json")

    def tag_biomes(self, tag, seen=None):
        """A biome tag's biomes, following nested '#tag' entries."""
        seen = seen if seen is not None else set()
        if tag in seen:
            return set()
        seen.add(tag)
        data = self.load("tags", "worldgen", "biome", strip(tag) + ".json")
        result = set()
        for value in (data or {}).get("values", []):
            if isinstance(value, dict):
                value = value.get("id", "")
            if value.startswith("#"):
                result |= self.tag_biomes(value[1:], seen)
            else:
                result.add(strip(value))
        return result


# Keys inside a configured feature whose value is a *placed* feature (a string id, an inline
# {"feature": ..., "placement": ...} object, or a list of those). Everything else nested in a
# configured feature is just more configuration.
PLACED_KEYS = {"feature", "features", "default", "feature_true", "feature_false",
               "vegetation_feature", "ground_feature"}


class Collector:
    """Blocks found, split by how much a player could rely on them.

    primary:    placed by real features -- ore veins, trees, flowers, and so on.
    incidental: placed only by 'disk' features, the small patches of sand, clay and gravel
                that turn up underwater in almost every biome. They technically exist
                everywhere, but nobody goes to a forest to find sand, so counting them would
                make sand and clay look "the same everywhere" and hide where they're actually
                plentiful. They're only used for a block that has no primary source at all
                (clay, which only ever comes from disks).
    """
    def __init__(self):
        self.primary = set()
        self.incidental = set()


# NOTE on the three walk_* functions: a *placed* feature and the *configured* feature it points
# at very often share a name (placed_feature/ore_emerald -> configured_feature/ore_emerald).
# They live in different folders, so they're tracked separately in `seen`; treating them as one
# thing makes the second lookup look like a repeat and silently drops the block.

def walk_placed(data, node, col, seen, incidental=False):
    """node: a placed feature id, an inline placed feature, or a list of them."""
    if isinstance(node, str):
        key = ("placed", node)
        if key in seen:
            return
        seen.add(key)
        placed = data.placed(node)
        if placed is not None:
            walk_configured(data, placed.get("feature"), col, seen, incidental)
    elif isinstance(node, list):
        for item in node:
            walk_placed(data, item, col, seen, incidental)
    elif isinstance(node, dict):
        if "feature" in node and "placement" in node:
            walk_configured(data, node["feature"], col, seen, incidental)   # an inline placed feature
        else:
            walk_generic(data, node, col, seen, incidental)                 # e.g. {"chance": .., "feature": ..}


def walk_configured(data, node, col, seen, incidental=False):
    """node: a configured feature id, or an inline {"type": .., "config": ..}."""
    if isinstance(node, str):
        key = ("configured", node)
        if key in seen:
            return
        seen.add(key)
        configured = data.configured(node)
        if configured is not None:
            walk_generic(data, configured, col, seen, incidental)
    elif node is not None:
        walk_generic(data, node, col, seen, incidental)


def walk_generic(data, node, col, seen, incidental=False):
    if isinstance(node, dict):
        if node.get("type") == "minecraft:disk":
            incidental = True
        if isinstance(node.get("Name"), str):          # a block state
            (col.incidental if incidental else col.primary).add(strip(node["Name"]))
        for key, value in node.items():
            if key in PLACED_KEYS:
                walk_placed(data, value, col, seen, incidental)
            elif isinstance(value, (dict, list)):
                walk_generic(data, value, col, seen, incidental)
    elif isinstance(node, list):
        for item in node:
            walk_generic(data, item, col, seen, incidental)


def biome_feature_blocks(data):
    """biome -> Collector of the blocks its features can place"""
    result = {}
    for biome in data.biomes:
        col = Collector()
        for step in data.load("worldgen", "biome", biome + ".json").get("features", []):
            for feature in step:
                walk_placed(data, feature, col, set())
        result[biome] = col
    return result


def walk_surface(rule, biome_context, out, everywhere):
    """Collect (block -> biomes) from a surface rule tree. biome_context is the set of
    biomes from the innermost enclosing 'biome is' condition, or None if there isn't one."""
    kind = rule.get("type", "")
    if kind == "minecraft:sequence":
        for sub in rule["sequence"]:
            walk_surface(sub, biome_context, out, everywhere)
    elif kind == "minecraft:condition":
        context = biome_context
        cond = rule.get("if_true", {})
        if cond.get("type") == "minecraft:biome":
            context = {strip(b) for b in cond.get("biome_is", [])}
        walk_surface(rule["then_run"], context, out, everywhere)
    elif kind == "minecraft:block":
        name = strip(rule["result_state"]["Name"])
        if biome_context is None:
            everywhere.add(name)
        else:
            out[name] |= biome_context
    elif kind == "minecraft:bandlands":
        for name in BADLANDS_BANDS:
            if biome_context is None:
                everywhere.add(name)
            else:
                out[name] |= biome_context


BADLANDS_BANDS = ["terracotta", "white_terracotta", "orange_terracotta", "yellow_terracotta",
                  "brown_terracotta", "red_terracotta", "light_gray_terracotta"]


def java_str(s):
    return '"%s"' % s


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--data", required=True, help="Path to the cloned <version>-data branch")
    parser.add_argument("--out", default="BiomeBlocks.java", help="Where to write the Java file")
    args = parser.parse_args()

    data = Data(args.data)

    dims = {
        "overworld": data.tag_biomes("minecraft:is_overworld"),
        "the_nether": data.tag_biomes("minecraft:is_nether"),
        "the_end": data.tag_biomes("minecraft:is_end"),
    }
    biome_dim = {}
    for dim, biomes in dims.items():
        for biome in biomes:
            biome_dim[biome] = dim
    unassigned = [b for b in data.biomes if b not in biome_dim]   # e.g. the_void
    print("biomes: %d  (%s)  unassigned: %s" % (
        len(data.biomes), ", ".join("%s=%d" % (d, len(b)) for d, b in dims.items()), unassigned), file=sys.stderr)

    # blocks -> biomes, from features. Disk-only ("incidental") sources are set aside and only
    # used further down for a block that has nothing else.
    block_biomes = defaultdict(set)
    incidental_biomes = defaultdict(set)
    for biome, col in biome_feature_blocks(data).items():
        for block in col.primary:
            block_biomes[block].add(biome)
        for block in col.incidental:
            incidental_biomes[block].add(biome)

    # blocks -> biomes, from surface rules (one noise_settings file per dimension)
    for dim, filename in (("overworld", "overworld"), ("the_nether", "nether"), ("the_end", "end")):
        settings = data.load("worldgen", "noise_settings", filename + ".json")
        if settings is None:
            continue
        specific = defaultdict(set)
        everywhere = set()
        walk_surface(settings["surface_rule"], None, specific, everywhere)
        for block, biomes in specific.items():
            block_biomes[block] |= biomes
        for block in everywhere:
            block_biomes[block] |= dims[dim]

    # A block whose ONLY source is disks (nothing else) is credited to the biomes those
    # disks are in. A block that has a real source AND also shows up in small disks nearly
    # everywhere (sand, clay, gravel) keeps its real biomes as "where it's plentiful", and is
    # separately flagged "also found in small amounts almost anywhere" -- both are true, and
    # a router needs to know both (a stack of sand is fine anywhere; a chest full needs a desert).
    also_everywhere = {}
    for block, biomes in incidental_biomes.items():
        if block not in block_biomes:
            block_biomes[block] = set(biomes)
            continue
        for dim, members in dims.items():
            if members and len(biomes & members) / len(members) >= UNIVERSAL_THRESHOLD:
                also_everywhere[block] = dim
                break

    # split into universal-in-a-dimension and biome-specific
    universal = {}
    specific = {}
    for block, biomes in block_biomes.items():
        placed_dim = None
        for dim, members in dims.items():
            if members and len(biomes & members) / len(members) >= UNIVERSAL_THRESHOLD:
                placed_dim = dim
                break
        if placed_dim:
            universal[block] = placed_dim
        else:
            specific[block] = sorted(biomes)

    # Identical biome lists are shared as one constant, to keep the file (and the class's
    # static initializer, which Java caps at 64KB) small.
    set_ids = {}
    for block, biomes in specific.items():
        set_ids.setdefault(tuple(biomes), len(set_ids))

    lines = []
    w = lines.append
    w("package net.lucy.data;")
    w("")
    w("import java.util.HashMap;")
    w("import java.util.HashSet;")
    w("import java.util.List;")
    w("import java.util.Map;")
    w("import java.util.Set;")
    w("")
    w("/**")
    w(" * Which blocks generate in which biomes. GENERATED by generate_biome_blocks.py from")
    w(" * Minecraft's own worldgen data -- edit the script or re-run it for another version,")
    w(" * don't edit this by hand. See that script for exactly what it does and doesn't cover.")
    w(" *")
    w(" * Most blocks (iron, diamond, coal, stone, dirt, ...) generate in nearly every biome of")
    w(" * their dimension, so which biome you're in makes no difference to finding them; those")
    w(" * are 'universal'. Only the rest have a list of biomes worth travelling to.")
    w(" */")
    w("public class BiomeBlocks {")
    w("    /** Blocks found in (nearly) every biome of a dimension: block -> dimension. */")
    w("    private static final Map<String, String> UNIVERSAL = new HashMap<>();")
    w("    /** Blocks that only generate in certain biomes: block -> those biomes. */")
    w("    private static final Map<String, List<String>> SPECIFIC = new HashMap<>();")
    w("    /** Blocks with a plentiful home in specific biomes that ALSO turn up in small patches almost")
    w("     *  everywhere in their dimension (sand, clay, gravel): block -> dimension. */")
    w("    private static final Map<String, String> ALSO_EVERYWHERE = new HashMap<>();")
    w("    /** Biome -> dimension (overworld, the_nether, the_end). */")
    w("    private static final Map<String, String> BIOME_DIMENSION = new HashMap<>();")
    w("")
    for biomes, i in sorted(set_ids.items(), key=lambda kv: kv[1]):
        w("    private static final List<String> SET_%d = List.of(%s);" % (i, ", ".join(java_str(b) for b in biomes)))
    w("")
    w("    static {")
    for block in sorted(universal):
        w("        UNIVERSAL.put(%s, %s);" % (java_str(block), java_str(universal[block])))
    w("")
    for block in sorted(specific):
        w("        SPECIFIC.put(%s, SET_%d);" % (java_str(block), set_ids[tuple(specific[block])]))
    w("")
    for block in sorted(also_everywhere):
        w("        ALSO_EVERYWHERE.put(%s, %s);" % (java_str(block), java_str(also_everywhere[block])))
    w("")
    for biome in sorted(biome_dim):
        w("        BIOME_DIMENSION.put(%s, %s);" % (java_str(biome), java_str(biome_dim[biome])))
    w("    }")
    w("")
    w("    /** Does it generate in (nearly) every biome of its dimension, so biome doesn't matter? */")
    w("    public static boolean isUniversal(String block) { return UNIVERSAL.containsKey(block); }")
    w("")
    w("    /** The biomes a block is limited to. Empty if it's universal, or not a generated block. */")
    w("    public static Set<String> biomesFor(String block) {")
    w("        List<String> biomes = SPECIFIC.get(block);")
    w("        return biomes == null ? Set.of() : new HashSet<>(biomes);")
    w("    }")
    w("")
    w("    /** Does this block, though limited to certain biomes for real quantities, also turn up in")
    w("     *  small patches nearly everywhere? Then a small amount doesn't need a trip. */")
    w("    public static boolean isAlsoFoundEverywhere(String block) { return ALSO_EVERYWHERE.containsKey(block); }")
    w("")
    w("    /** True if this dictionary knows anything about the block at all. */")
    w("    public static boolean isKnown(String block) { return UNIVERSAL.containsKey(block) || SPECIFIC.containsKey(block); }")
    w("")
    w("    /** The dimension a biome belongs to, or null for one this doesn't classify (the_void). */")
    w("    public static String dimensionOf(String biome) { return BIOME_DIMENSION.get(biome); }")
    w("}")

    with open(args.out, "w") as f:
        f.write("\n".join(lines) + "\n")

    print("universal blocks: %d   biome-specific blocks: %d   distinct biome lists: %d   also-found-everywhere: %s" %
          (len(universal), len(specific), len(set_ids), sorted(also_everywhere)), file=sys.stderr)
    for check in ("iron_ore", "diamond_ore", "emerald_ore", "sand", "red_sand", "mycelium", "podzol", "ancient_debris", "clay", "packed_ice"):
        if check in universal:
            print("  %-16s universal (%s)" % (check, universal[check]), file=sys.stderr)
        elif check in specific:
            print("  %-16s %d biomes: %s" % (check, len(specific[check]), ", ".join(specific[check][:8]) + (" ..." if len(specific[check]) > 8 else "")), file=sys.stderr)
        else:
            print("  %-16s not found" % check, file=sys.stderr)


if __name__ == "__main__":
    main()