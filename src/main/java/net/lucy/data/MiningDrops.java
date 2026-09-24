package net.lucy.data;

import net.lucy.model.MiningDrop;
import java.util.HashMap;
import java.util.Map;

/**
 * What you actually get from breaking a block: without Silk Touch, and with it.
 * A null drop means "nothing, without Silk Touch" (glass, ice, and similar blocks).
 *
 * This covers ores, the common Silk-Touch-only blocks, and blocks whose block id
 * doesn't match their item id (a wall torch is the block "wall_torch" but the item
 * "torch", for example) — those would otherwise show up under the wrong name, or
 * with no icon at all, on the Material List and Raw Materials screens.
 *
 * This isn't a complete copy of every vanilla loot table (drop quantities, fortune,
 * and shears aren't modelled), but it covers what actually shows up in most builds.
 * Add more entries here the same way if you run into a block that isn't listed.
 */
public class MiningDrops {
    public static Map<String, MiningDrop> drops = new HashMap<>();

    static {
        // ---- Overworld ores (normal drop, Silk Touch drop) ----
        drops.put("stone", new MiningDrop("cobblestone", "stone"));
        drops.put("deepslate", new MiningDrop("cobbled_deepslate", "deepslate"));
        drops.put("coal_ore", new MiningDrop("coal", "coal_ore"));
        drops.put("deepslate_coal_ore", new MiningDrop("coal", "deepslate_coal_ore"));
        drops.put("iron_ore", new MiningDrop("raw_iron", "iron_ore"));
        drops.put("deepslate_iron_ore", new MiningDrop("raw_iron", "deepslate_iron_ore"));
        drops.put("copper_ore", new MiningDrop("raw_copper", "copper_ore"));
        drops.put("deepslate_copper_ore", new MiningDrop("raw_copper", "deepslate_copper_ore"));
        drops.put("gold_ore", new MiningDrop("raw_gold", "gold_ore"));
        drops.put("deepslate_gold_ore", new MiningDrop("raw_gold", "deepslate_gold_ore"));
        drops.put("redstone_ore", new MiningDrop("redstone", "redstone_ore"));
        drops.put("deepslate_redstone_ore", new MiningDrop("redstone", "deepslate_redstone_ore"));
        drops.put("diamond_ore", new MiningDrop("diamond", "diamond_ore"));
        drops.put("deepslate_diamond_ore", new MiningDrop("diamond", "deepslate_diamond_ore"));
        drops.put("emerald_ore", new MiningDrop("emerald", "emerald_ore"));
        drops.put("deepslate_emerald_ore", new MiningDrop("emerald", "deepslate_emerald_ore"));
        drops.put("lapis_ore", new MiningDrop("lapis_lazuli", "lapis_ore"));
        drops.put("deepslate_lapis_ore", new MiningDrop("lapis_lazuli", "deepslate_lapis_ore"));

        // ---- Nether ores ----
        drops.put("nether_quartz_ore", new MiningDrop("quartz", "nether_quartz_ore"));
        drops.put("nether_gold_ore", new MiningDrop("gold_nugget", "nether_gold_ore"));

        // ---- Blocks that need Silk Touch to get the block itself; otherwise a different item ----
        drops.put("grass_block", new MiningDrop("dirt", "grass_block"));
        drops.put("podzol", new MiningDrop("dirt", "podzol"));
        drops.put("mycelium", new MiningDrop("dirt", "mycelium"));
        drops.put("glass", new MiningDrop(null, "glass"));
        drops.put("tinted_glass", new MiningDrop(null, "tinted_glass"));
        drops.put("ice", new MiningDrop(null, "ice"));
        drops.put("glowstone", new MiningDrop("glowstone_dust", "glowstone"));
        drops.put("sea_lantern", new MiningDrop("prismarine_crystals", "sea_lantern"));
        drops.put("melon", new MiningDrop("melon_slice", "melon"));
        drops.put("snow", new MiningDrop("snowball", "snow"));
        drops.put("turtle_egg", new MiningDrop(null, "turtle_egg"));
        drops.put("sculk_catalyst", new MiningDrop(null, "sculk_catalyst"));
        drops.put("sculk_sensor", new MiningDrop(null, "sculk_sensor"));
        drops.put("sculk_shrieker", new MiningDrop(null, "sculk_shrieker"));
        drops.put("amethyst_cluster", new MiningDrop("amethyst_shard", "amethyst_cluster"));

        // ---- Decorative plants: only worth collecting with Silk Touch here, since ----
        // ---- shears aren't modelled as a separate case ----
        drops.put("vine", new MiningDrop(null, "vine"));
        drops.put("glow_lichen", new MiningDrop(null, "glow_lichen"));
        drops.put("twisting_vines", new MiningDrop(null, "twisting_vines"));
        drops.put("twisting_vines_plant", new MiningDrop(null, "twisting_vines"));
        drops.put("weeping_vines", new MiningDrop(null, "weeping_vines"));
        drops.put("weeping_vines_plant", new MiningDrop(null, "weeping_vines"));
        drops.put("kelp_plant", new MiningDrop("kelp", "kelp"));
        drops.put("bamboo_sapling", new MiningDrop("bamboo", "bamboo"));
        drops.put("cave_vines", new MiningDrop("glow_berries", "cave_vines"));
        drops.put("cave_vines_plant", new MiningDrop("glow_berries", "cave_vines_plant"));

        // ---- Block id doesn't match the item id, regardless of Silk Touch ----
        same("wall_torch", "torch");
        same("soul_wall_torch", "soul_torch");
        same("redstone_wall_torch", "redstone_torch");
        same("redstone_wire", "redstone");
        same("tripwire", "string");
        same("cocoa", "cocoa_beans");
        same("pumpkin_stem", "pumpkin_seeds");
        same("attached_pumpkin_stem", "pumpkin_seeds");
        same("melon_stem", "melon_seeds");
        same("attached_melon_stem", "melon_seeds");
        same("big_dripleaf_stem", "big_dripleaf");
    }

    // Both with and without Silk Touch give the same item (just under a different name
    // than the block itself).
    private static void same(String blockName, String itemName) {
        drops.put(blockName, new MiningDrop(itemName, itemName));
    }
}