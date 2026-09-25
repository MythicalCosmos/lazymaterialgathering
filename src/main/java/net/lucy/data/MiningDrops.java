package net.lucy.data;

import net.lucy.model.MiningDrop;
import java.util.HashMap;
import java.util.Map;

/**
 * What you actually get from breaking a block: without any special tool, and with Silk
 * Touch (and, for some plants, shears).
 *
 * These 248 entries are generated from Minecraft 1.20.1's own loot table data (see
 * generate_mining_drops.py), not hand-guessed, and now include:
 *  - Ore drops, Silk-Touch-only blocks, and id mismatches (a wall torch is the block
 *    "wall_torch" but the item "torch"), same as before.
 *  - Realistic quantities where the game always gives more than one (glowstone dust,
 *    melon slices, amethyst shards, ...), read straight from the loot table's own count
 *    range. Fortune's extra rolls are NOT included, only the un-enchanted baseline.
 *  - Shears as a second way to get the Silk-Touch-equivalent result, for the blocks
 *    where that's actually true (leaves, vines, tall grass, ferns).
 * A block with no entry here simply drops itself, count 1, either way — correct for
 * most blocks.
 *
 * Still not modelled, because it would need to know a block's exact state (age, "type:
 * double", how many candles are stacked, ...), which this mod only tracks by block name:
 *  - Slabs, candles, beds, and doors always count as their single/default state (a slab
 *    is always 1, never 2, since most slabs placed in a build are the single kind).
 *  - Crops are read as their mature drop; an immature wheat block still counts as wheat,
 *    not seeds.
 *  - A flower pot with a plant in it (potted_*) drops the flower pot AND the plant it
 *    held — handled in MiningResolver via POTTED_PLANTS, not here, since it's two items.
 *
 * Regenerate or extend this with generate_mining_drops.py for whichever Minecraft
 * version you target (see that script for how to fetch the version data it reads).
 */
public class MiningDrops {
    public static Map<String, MiningDrop> drops = new HashMap<>();

    static {
        drops.put("acacia_leaves", new MiningDrop(null, 1, "acacia_leaves", 1, true));
        drops.put("acacia_wall_hanging_sign", new MiningDrop("acacia_hanging_sign", 1, "acacia_hanging_sign", 1, false));
        drops.put("acacia_wall_sign", new MiningDrop("acacia_sign", 1, "acacia_sign", 1, false));
        drops.put("amethyst_cluster", new MiningDrop("amethyst_shard", 4, "amethyst_cluster", 1, false));
        drops.put("attached_melon_stem", new MiningDrop("melon_seeds", 1, "melon_seeds", 1, false));
        drops.put("attached_pumpkin_stem", new MiningDrop("pumpkin_seeds", 1, "pumpkin_seeds", 1, false));
        drops.put("azalea_leaves", new MiningDrop(null, 1, "azalea_leaves", 1, true));
        drops.put("bamboo_sapling", new MiningDrop("bamboo", 1, "bamboo", 1, false));
        drops.put("bamboo_wall_hanging_sign", new MiningDrop("bamboo_hanging_sign", 1, "bamboo_hanging_sign", 1, false));
        drops.put("bamboo_wall_sign", new MiningDrop("bamboo_sign", 1, "bamboo_sign", 1, false));
        drops.put("bee_nest", new MiningDrop(null, 1, "bee_nest", 1, false));
        drops.put("beetroots", new MiningDrop("beetroot", 1, "beetroot", 1, false));
        drops.put("big_dripleaf_stem", new MiningDrop("big_dripleaf", 1, "big_dripleaf", 1, false));
        drops.put("birch_leaves", new MiningDrop(null, 1, "birch_leaves", 1, true));
        drops.put("birch_wall_hanging_sign", new MiningDrop("birch_hanging_sign", 1, "birch_hanging_sign", 1, false));
        drops.put("birch_wall_sign", new MiningDrop("birch_sign", 1, "birch_sign", 1, false));
        drops.put("black_candle_cake", new MiningDrop("black_candle", 1, "black_candle", 1, false));
        drops.put("black_stained_glass", new MiningDrop(null, 1, "black_stained_glass", 1, false));
        drops.put("black_stained_glass_pane", new MiningDrop(null, 1, "black_stained_glass_pane", 1, false));
        drops.put("black_wall_banner", new MiningDrop("black_banner", 1, "black_banner", 1, false));
        drops.put("blue_candle_cake", new MiningDrop("blue_candle", 1, "blue_candle", 1, false));
        drops.put("blue_ice", new MiningDrop(null, 1, "blue_ice", 1, false));
        drops.put("blue_stained_glass", new MiningDrop(null, 1, "blue_stained_glass", 1, false));
        drops.put("blue_stained_glass_pane", new MiningDrop(null, 1, "blue_stained_glass_pane", 1, false));
        drops.put("blue_wall_banner", new MiningDrop("blue_banner", 1, "blue_banner", 1, false));
        drops.put("bookshelf", new MiningDrop("book", 3, "bookshelf", 1, false));
        drops.put("brain_coral", new MiningDrop(null, 1, "brain_coral", 1, false));
        drops.put("brain_coral_block", new MiningDrop("dead_brain_coral_block", 1, "brain_coral_block", 1, false));
        drops.put("brain_coral_fan", new MiningDrop(null, 1, "brain_coral_fan", 1, false));
        drops.put("brain_coral_wall_fan", new MiningDrop(null, 1, "brain_coral_fan", 1, false));
        drops.put("brown_candle_cake", new MiningDrop("brown_candle", 1, "brown_candle", 1, false));
        drops.put("brown_mushroom_block", new MiningDrop("brown_mushroom", 1, "brown_mushroom_block", 1, false));
        drops.put("brown_stained_glass", new MiningDrop(null, 1, "brown_stained_glass", 1, false));
        drops.put("brown_stained_glass_pane", new MiningDrop(null, 1, "brown_stained_glass_pane", 1, false));
        drops.put("brown_wall_banner", new MiningDrop("brown_banner", 1, "brown_banner", 1, false));
        drops.put("bubble_coral", new MiningDrop(null, 1, "bubble_coral", 1, false));
        drops.put("bubble_coral_block", new MiningDrop("dead_bubble_coral_block", 1, "bubble_coral_block", 1, false));
        drops.put("bubble_coral_fan", new MiningDrop(null, 1, "bubble_coral_fan", 1, false));
        drops.put("bubble_coral_wall_fan", new MiningDrop(null, 1, "bubble_coral_fan", 1, false));
        drops.put("budding_amethyst", new MiningDrop(null, 1, null, 1, false));
        drops.put("cake", new MiningDrop(null, 1, null, 1, false));
        drops.put("calibrated_sculk_sensor", new MiningDrop(null, 1, "calibrated_sculk_sensor", 1, false));
        drops.put("campfire", new MiningDrop("charcoal", 2, "campfire", 1, false));
        drops.put("candle_cake", new MiningDrop("candle", 1, "candle", 1, false));
        drops.put("carrots", new MiningDrop("carrot", 1, "carrot", 1, false));
        drops.put("cave_vines", new MiningDrop("glow_berries", 1, "glow_berries", 1, false));
        drops.put("cave_vines_plant", new MiningDrop("glow_berries", 1, "glow_berries", 1, false));
        drops.put("cherry_leaves", new MiningDrop(null, 1, "cherry_leaves", 1, true));
        drops.put("cherry_wall_hanging_sign", new MiningDrop("cherry_hanging_sign", 1, "cherry_hanging_sign", 1, false));
        drops.put("cherry_wall_sign", new MiningDrop("cherry_sign", 1, "cherry_sign", 1, false));
        drops.put("chiseled_bookshelf", new MiningDrop(null, 1, "chiseled_bookshelf", 1, false));
        drops.put("chorus_plant", new MiningDrop("chorus_fruit", 1, "chorus_fruit", 1, false));
        drops.put("clay", new MiningDrop("clay_ball", 4, "clay", 1, false));
        drops.put("coal_ore", new MiningDrop("coal", 1, "coal_ore", 1, false));
        drops.put("cocoa", new MiningDrop("cocoa_beans", 1, "cocoa_beans", 1, false));
        drops.put("copper_ore", new MiningDrop("raw_copper", 4, "copper_ore", 1, false));
        drops.put("creeper_wall_head", new MiningDrop("creeper_head", 1, "creeper_head", 1, false));
        drops.put("crimson_nylium", new MiningDrop("netherrack", 1, "crimson_nylium", 1, false));
        drops.put("crimson_wall_hanging_sign", new MiningDrop("crimson_hanging_sign", 1, "crimson_hanging_sign", 1, false));
        drops.put("crimson_wall_sign", new MiningDrop("crimson_sign", 1, "crimson_sign", 1, false));
        drops.put("cyan_candle_cake", new MiningDrop("cyan_candle", 1, "cyan_candle", 1, false));
        drops.put("cyan_stained_glass", new MiningDrop(null, 1, "cyan_stained_glass", 1, false));
        drops.put("cyan_stained_glass_pane", new MiningDrop(null, 1, "cyan_stained_glass_pane", 1, false));
        drops.put("cyan_wall_banner", new MiningDrop("cyan_banner", 1, "cyan_banner", 1, false));
        drops.put("dark_oak_leaves", new MiningDrop(null, 1, "dark_oak_leaves", 1, true));
        drops.put("dark_oak_wall_hanging_sign", new MiningDrop("dark_oak_hanging_sign", 1, "dark_oak_hanging_sign", 1, false));
        drops.put("dark_oak_wall_sign", new MiningDrop("dark_oak_sign", 1, "dark_oak_sign", 1, false));
        drops.put("dead_brain_coral", new MiningDrop(null, 1, "dead_brain_coral", 1, false));
        drops.put("dead_brain_coral_fan", new MiningDrop(null, 1, "dead_brain_coral_fan", 1, false));
        drops.put("dead_brain_coral_wall_fan", new MiningDrop(null, 1, "dead_brain_coral_fan", 1, false));
        drops.put("dead_bubble_coral", new MiningDrop(null, 1, "dead_bubble_coral", 1, false));
        drops.put("dead_bubble_coral_fan", new MiningDrop(null, 1, "dead_bubble_coral_fan", 1, false));
        drops.put("dead_bubble_coral_wall_fan", new MiningDrop(null, 1, "dead_bubble_coral_fan", 1, false));
        drops.put("dead_bush", new MiningDrop("stick", 1, "dead_bush", 1, true));
        drops.put("dead_fire_coral", new MiningDrop(null, 1, "dead_fire_coral", 1, false));
        drops.put("dead_fire_coral_fan", new MiningDrop(null, 1, "dead_fire_coral_fan", 1, false));
        drops.put("dead_fire_coral_wall_fan", new MiningDrop(null, 1, "dead_fire_coral_fan", 1, false));
        drops.put("dead_horn_coral", new MiningDrop(null, 1, "dead_horn_coral", 1, false));
        drops.put("dead_horn_coral_fan", new MiningDrop(null, 1, "dead_horn_coral_fan", 1, false));
        drops.put("dead_horn_coral_wall_fan", new MiningDrop(null, 1, "dead_horn_coral_fan", 1, false));
        drops.put("dead_tube_coral", new MiningDrop(null, 1, "dead_tube_coral", 1, false));
        drops.put("dead_tube_coral_fan", new MiningDrop(null, 1, "dead_tube_coral_fan", 1, false));
        drops.put("dead_tube_coral_wall_fan", new MiningDrop(null, 1, "dead_tube_coral_fan", 1, false));
        drops.put("deepslate", new MiningDrop("cobbled_deepslate", 1, "deepslate", 1, false));
        drops.put("deepslate_coal_ore", new MiningDrop("coal", 1, "deepslate_coal_ore", 1, false));
        drops.put("deepslate_copper_ore", new MiningDrop("raw_copper", 4, "deepslate_copper_ore", 1, false));
        drops.put("deepslate_diamond_ore", new MiningDrop("diamond", 1, "deepslate_diamond_ore", 1, false));
        drops.put("deepslate_emerald_ore", new MiningDrop("emerald", 1, "deepslate_emerald_ore", 1, false));
        drops.put("deepslate_gold_ore", new MiningDrop("raw_gold", 1, "deepslate_gold_ore", 1, false));
        drops.put("deepslate_iron_ore", new MiningDrop("raw_iron", 1, "deepslate_iron_ore", 1, false));
        drops.put("deepslate_lapis_ore", new MiningDrop("lapis_lazuli", 6, "deepslate_lapis_ore", 1, false));
        drops.put("deepslate_redstone_ore", new MiningDrop("redstone", 4, "deepslate_redstone_ore", 1, false));
        drops.put("diamond_ore", new MiningDrop("diamond", 1, "diamond_ore", 1, false));
        drops.put("dirt_path", new MiningDrop("dirt", 1, "dirt", 1, false));
        drops.put("dragon_wall_head", new MiningDrop("dragon_head", 1, "dragon_head", 1, false));
        drops.put("emerald_ore", new MiningDrop("emerald", 1, "emerald_ore", 1, false));
        drops.put("ender_chest", new MiningDrop("obsidian", 8, "ender_chest", 1, false));
        drops.put("farmland", new MiningDrop("dirt", 1, "dirt", 1, false));
        drops.put("fern", new MiningDrop("wheat_seeds", 1, "fern", 1, true));
        drops.put("fire", new MiningDrop(null, 1, null, 1, false));
        drops.put("fire_coral", new MiningDrop(null, 1, "fire_coral", 1, false));
        drops.put("fire_coral_block", new MiningDrop("dead_fire_coral_block", 1, "fire_coral_block", 1, false));
        drops.put("fire_coral_fan", new MiningDrop(null, 1, "fire_coral_fan", 1, false));
        drops.put("fire_coral_wall_fan", new MiningDrop(null, 1, "fire_coral_fan", 1, false));
        drops.put("flowering_azalea_leaves", new MiningDrop(null, 1, "flowering_azalea_leaves", 1, true));
        drops.put("frogspawn", new MiningDrop(null, 1, null, 1, false));
        drops.put("frosted_ice", new MiningDrop(null, 1, null, 1, false));
        drops.put("glass", new MiningDrop(null, 1, "glass", 1, false));
        drops.put("glass_pane", new MiningDrop(null, 1, "glass_pane", 1, false));
        drops.put("glowstone", new MiningDrop("glowstone_dust", 3, "glowstone", 1, false));
        drops.put("gold_ore", new MiningDrop("raw_gold", 1, "gold_ore", 1, false));
        drops.put("grass", new MiningDrop("wheat_seeds", 1, "grass", 1, true));
        drops.put("grass_block", new MiningDrop("dirt", 1, "grass_block", 1, false));
        drops.put("gray_candle_cake", new MiningDrop("gray_candle", 1, "gray_candle", 1, false));
        drops.put("gray_stained_glass", new MiningDrop(null, 1, "gray_stained_glass", 1, false));
        drops.put("gray_stained_glass_pane", new MiningDrop(null, 1, "gray_stained_glass_pane", 1, false));
        drops.put("gray_wall_banner", new MiningDrop("gray_banner", 1, "gray_banner", 1, false));
        drops.put("green_candle_cake", new MiningDrop("green_candle", 1, "green_candle", 1, false));
        drops.put("green_stained_glass", new MiningDrop(null, 1, "green_stained_glass", 1, false));
        drops.put("green_stained_glass_pane", new MiningDrop(null, 1, "green_stained_glass_pane", 1, false));
        drops.put("green_wall_banner", new MiningDrop("green_banner", 1, "green_banner", 1, false));
        drops.put("horn_coral", new MiningDrop(null, 1, "horn_coral", 1, false));
        drops.put("horn_coral_block", new MiningDrop("dead_horn_coral_block", 1, "horn_coral_block", 1, false));
        drops.put("horn_coral_fan", new MiningDrop(null, 1, "horn_coral_fan", 1, false));
        drops.put("horn_coral_wall_fan", new MiningDrop(null, 1, "horn_coral_fan", 1, false));
        drops.put("ice", new MiningDrop(null, 1, "ice", 1, false));
        drops.put("infested_chiseled_stone_bricks", new MiningDrop(null, 1, "chiseled_stone_bricks", 1, false));
        drops.put("infested_cobblestone", new MiningDrop(null, 1, "cobblestone", 1, false));
        drops.put("infested_cracked_stone_bricks", new MiningDrop(null, 1, "cracked_stone_bricks", 1, false));
        drops.put("infested_deepslate", new MiningDrop(null, 1, "deepslate", 1, false));
        drops.put("infested_mossy_stone_bricks", new MiningDrop(null, 1, "mossy_stone_bricks", 1, false));
        drops.put("infested_stone", new MiningDrop(null, 1, "stone", 1, false));
        drops.put("infested_stone_bricks", new MiningDrop(null, 1, "stone_bricks", 1, false));
        drops.put("iron_ore", new MiningDrop("raw_iron", 1, "iron_ore", 1, false));
        drops.put("jungle_leaves", new MiningDrop(null, 1, "jungle_leaves", 1, true));
        drops.put("jungle_wall_hanging_sign", new MiningDrop("jungle_hanging_sign", 1, "jungle_hanging_sign", 1, false));
        drops.put("jungle_wall_sign", new MiningDrop("jungle_sign", 1, "jungle_sign", 1, false));
        drops.put("kelp_plant", new MiningDrop("kelp", 1, "kelp", 1, false));
        drops.put("lapis_ore", new MiningDrop("lapis_lazuli", 6, "lapis_ore", 1, false));
        drops.put("large_amethyst_bud", new MiningDrop(null, 1, "large_amethyst_bud", 1, false));
        drops.put("large_fern", new MiningDrop(null, 1, "fern", 2, true));
        drops.put("lava_cauldron", new MiningDrop("cauldron", 1, "cauldron", 1, false));
        drops.put("light_blue_candle_cake", new MiningDrop("light_blue_candle", 1, "light_blue_candle", 1, false));
        drops.put("light_blue_stained_glass", new MiningDrop(null, 1, "light_blue_stained_glass", 1, false));
        drops.put("light_blue_stained_glass_pane", new MiningDrop(null, 1, "light_blue_stained_glass_pane", 1, false));
        drops.put("light_blue_wall_banner", new MiningDrop("light_blue_banner", 1, "light_blue_banner", 1, false));
        drops.put("light_gray_candle_cake", new MiningDrop("light_gray_candle", 1, "light_gray_candle", 1, false));
        drops.put("light_gray_stained_glass", new MiningDrop(null, 1, "light_gray_stained_glass", 1, false));
        drops.put("light_gray_stained_glass_pane", new MiningDrop(null, 1, "light_gray_stained_glass_pane", 1, false));
        drops.put("light_gray_wall_banner", new MiningDrop("light_gray_banner", 1, "light_gray_banner", 1, false));
        drops.put("lime_candle_cake", new MiningDrop("lime_candle", 1, "lime_candle", 1, false));
        drops.put("lime_stained_glass", new MiningDrop(null, 1, "lime_stained_glass", 1, false));
        drops.put("lime_stained_glass_pane", new MiningDrop(null, 1, "lime_stained_glass_pane", 1, false));
        drops.put("lime_wall_banner", new MiningDrop("lime_banner", 1, "lime_banner", 1, false));
        drops.put("magenta_candle_cake", new MiningDrop("magenta_candle", 1, "magenta_candle", 1, false));
        drops.put("magenta_stained_glass", new MiningDrop(null, 1, "magenta_stained_glass", 1, false));
        drops.put("magenta_stained_glass_pane", new MiningDrop(null, 1, "magenta_stained_glass_pane", 1, false));
        drops.put("magenta_wall_banner", new MiningDrop("magenta_banner", 1, "magenta_banner", 1, false));
        drops.put("mangrove_leaves", new MiningDrop(null, 1, "mangrove_leaves", 1, true));
        drops.put("mangrove_wall_hanging_sign", new MiningDrop("mangrove_hanging_sign", 1, "mangrove_hanging_sign", 1, false));
        drops.put("mangrove_wall_sign", new MiningDrop("mangrove_sign", 1, "mangrove_sign", 1, false));
        drops.put("medium_amethyst_bud", new MiningDrop(null, 1, "medium_amethyst_bud", 1, false));
        drops.put("melon", new MiningDrop("melon_slice", 5, "melon", 1, false));
        drops.put("melon_stem", new MiningDrop("melon_seeds", 1, "melon_seeds", 1, false));
        drops.put("mushroom_stem", new MiningDrop(null, 1, "mushroom_stem", 1, false));
        drops.put("mycelium", new MiningDrop("dirt", 1, "mycelium", 1, false));
        drops.put("nether_gold_ore", new MiningDrop("gold_nugget", 4, "nether_gold_ore", 1, false));
        drops.put("nether_portal", new MiningDrop(null, 1, null, 1, false));
        drops.put("nether_quartz_ore", new MiningDrop("quartz", 1, "nether_quartz_ore", 1, false));
        drops.put("oak_leaves", new MiningDrop(null, 1, "oak_leaves", 1, true));
        drops.put("oak_wall_hanging_sign", new MiningDrop("oak_hanging_sign", 1, "oak_hanging_sign", 1, false));
        drops.put("oak_wall_sign", new MiningDrop("oak_sign", 1, "oak_sign", 1, false));
        drops.put("orange_candle_cake", new MiningDrop("orange_candle", 1, "orange_candle", 1, false));
        drops.put("orange_stained_glass", new MiningDrop(null, 1, "orange_stained_glass", 1, false));
        drops.put("orange_stained_glass_pane", new MiningDrop(null, 1, "orange_stained_glass_pane", 1, false));
        drops.put("orange_wall_banner", new MiningDrop("orange_banner", 1, "orange_banner", 1, false));
        drops.put("packed_ice", new MiningDrop(null, 1, "packed_ice", 1, false));
        drops.put("piglin_wall_head", new MiningDrop("piglin_head", 1, "piglin_head", 1, false));
        drops.put("pink_candle_cake", new MiningDrop("pink_candle", 1, "pink_candle", 1, false));
        drops.put("pink_stained_glass", new MiningDrop(null, 1, "pink_stained_glass", 1, false));
        drops.put("pink_stained_glass_pane", new MiningDrop(null, 1, "pink_stained_glass_pane", 1, false));
        drops.put("pink_wall_banner", new MiningDrop("pink_banner", 1, "pink_banner", 1, false));
        drops.put("pitcher_crop", new MiningDrop("pitcher_pod", 1, "pitcher_pod", 1, false));
        drops.put("player_wall_head", new MiningDrop("player_head", 1, "player_head", 1, false));
        drops.put("podzol", new MiningDrop("dirt", 1, "podzol", 1, false));
        drops.put("potatoes", new MiningDrop("potato", 1, "potato", 1, false));
        drops.put("powder_snow", new MiningDrop(null, 1, null, 1, false));
        drops.put("powder_snow_cauldron", new MiningDrop("cauldron", 1, "cauldron", 1, false));
        drops.put("pumpkin_stem", new MiningDrop("pumpkin_seeds", 1, "pumpkin_seeds", 1, false));
        drops.put("purple_candle_cake", new MiningDrop("purple_candle", 1, "purple_candle", 1, false));
        drops.put("purple_stained_glass", new MiningDrop(null, 1, "purple_stained_glass", 1, false));
        drops.put("purple_stained_glass_pane", new MiningDrop(null, 1, "purple_stained_glass_pane", 1, false));
        drops.put("purple_wall_banner", new MiningDrop("purple_banner", 1, "purple_banner", 1, false));
        drops.put("red_candle_cake", new MiningDrop("red_candle", 1, "red_candle", 1, false));
        drops.put("red_mushroom_block", new MiningDrop("red_mushroom", 1, "red_mushroom_block", 1, false));
        drops.put("red_stained_glass", new MiningDrop(null, 1, "red_stained_glass", 1, false));
        drops.put("red_stained_glass_pane", new MiningDrop(null, 1, "red_stained_glass_pane", 1, false));
        drops.put("red_wall_banner", new MiningDrop("red_banner", 1, "red_banner", 1, false));
        drops.put("redstone_ore", new MiningDrop("redstone", 4, "redstone_ore", 1, false));
        drops.put("redstone_wall_torch", new MiningDrop("redstone_torch", 1, "redstone_torch", 1, false));
        drops.put("redstone_wire", new MiningDrop("redstone", 1, "redstone", 1, false));
        drops.put("reinforced_deepslate", new MiningDrop(null, 1, null, 1, false));
        drops.put("sculk", new MiningDrop(null, 1, "sculk", 1, false));
        drops.put("sculk_catalyst", new MiningDrop(null, 1, "sculk_catalyst", 1, false));
        drops.put("sculk_sensor", new MiningDrop(null, 1, "sculk_sensor", 1, false));
        drops.put("sculk_shrieker", new MiningDrop(null, 1, "sculk_shrieker", 1, false));
        drops.put("sea_lantern", new MiningDrop("prismarine_crystals", 2, "sea_lantern", 1, false));
        drops.put("skeleton_wall_skull", new MiningDrop("skeleton_skull", 1, "skeleton_skull", 1, false));
        drops.put("small_amethyst_bud", new MiningDrop(null, 1, "small_amethyst_bud", 1, false));
        drops.put("snow", new MiningDrop("snowball", 4, "snow", 1, false));
        drops.put("snow_block", new MiningDrop("snowball", 4, "snow_block", 1, false));
        drops.put("soul_campfire", new MiningDrop("soul_soil", 1, "soul_campfire", 1, false));
        drops.put("soul_fire", new MiningDrop(null, 1, null, 1, false));
        drops.put("soul_wall_torch", new MiningDrop("soul_torch", 1, "soul_torch", 1, false));
        drops.put("spawner", new MiningDrop(null, 1, null, 1, false));
        drops.put("spruce_leaves", new MiningDrop(null, 1, "spruce_leaves", 1, true));
        drops.put("spruce_wall_hanging_sign", new MiningDrop("spruce_hanging_sign", 1, "spruce_hanging_sign", 1, false));
        drops.put("spruce_wall_sign", new MiningDrop("spruce_sign", 1, "spruce_sign", 1, false));
        drops.put("stone", new MiningDrop("cobblestone", 1, "stone", 1, false));
        drops.put("suspicious_gravel", new MiningDrop(null, 1, null, 1, false));
        drops.put("suspicious_sand", new MiningDrop(null, 1, null, 1, false));
        drops.put("sweet_berry_bush", new MiningDrop("sweet_berries", 2, "sweet_berries", 2, false));
        drops.put("tall_grass", new MiningDrop(null, 1, "grass", 2, true));
        drops.put("tall_seagrass", new MiningDrop("seagrass", 2, "seagrass", 2, false));
        drops.put("torchflower_crop", new MiningDrop("torchflower_seeds", 1, "torchflower_seeds", 1, false));
        drops.put("tripwire", new MiningDrop("string", 1, "string", 1, false));
        drops.put("tube_coral", new MiningDrop(null, 1, "tube_coral", 1, false));
        drops.put("tube_coral_block", new MiningDrop("dead_tube_coral_block", 1, "tube_coral_block", 1, false));
        drops.put("tube_coral_fan", new MiningDrop(null, 1, "tube_coral_fan", 1, false));
        drops.put("tube_coral_wall_fan", new MiningDrop(null, 1, "tube_coral_fan", 1, false));
        drops.put("turtle_egg", new MiningDrop(null, 1, "turtle_egg", 1, false));
        drops.put("twisting_vines_plant", new MiningDrop("twisting_vines", 1, "twisting_vines", 1, false));
        drops.put("wall_torch", new MiningDrop("torch", 1, "torch", 1, false));
        drops.put("warped_nylium", new MiningDrop("netherrack", 1, "warped_nylium", 1, false));
        drops.put("warped_wall_hanging_sign", new MiningDrop("warped_hanging_sign", 1, "warped_hanging_sign", 1, false));
        drops.put("warped_wall_sign", new MiningDrop("warped_sign", 1, "warped_sign", 1, false));
        drops.put("water_cauldron", new MiningDrop("cauldron", 1, "cauldron", 1, false));
        drops.put("weeping_vines_plant", new MiningDrop("weeping_vines", 1, "weeping_vines", 1, false));
        drops.put("white_candle_cake", new MiningDrop("white_candle", 1, "white_candle", 1, false));
        drops.put("white_stained_glass", new MiningDrop(null, 1, "white_stained_glass", 1, false));
        drops.put("white_stained_glass_pane", new MiningDrop(null, 1, "white_stained_glass_pane", 1, false));
        drops.put("white_wall_banner", new MiningDrop("white_banner", 1, "white_banner", 1, false));
        drops.put("wither_skeleton_wall_skull", new MiningDrop("wither_skeleton_skull", 1, "wither_skeleton_skull", 1, false));
        drops.put("yellow_candle_cake", new MiningDrop("yellow_candle", 1, "yellow_candle", 1, false));
        drops.put("yellow_stained_glass", new MiningDrop(null, 1, "yellow_stained_glass", 1, false));
        drops.put("yellow_stained_glass_pane", new MiningDrop(null, 1, "yellow_stained_glass_pane", 1, false));
        drops.put("yellow_wall_banner", new MiningDrop("yellow_banner", 1, "yellow_banner", 1, false));
        drops.put("zombie_wall_head", new MiningDrop("zombie_head", 1, "zombie_head", 1, false));
    }
}