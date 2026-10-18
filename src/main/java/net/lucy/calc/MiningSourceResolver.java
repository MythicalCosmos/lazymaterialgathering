package net.lucy.calc;

import net.lucy.config.Configs;

import java.util.List;

/**
 * Maps a desired inventory item to physical blocks that can
 * legitimately produce it with the configured tool/drop mode.
 *
 * This is intentionally an explicit allow-list.
 *
 * It does NOT assume that every block whose item name happens
 * to match the requested item is a valid mining source.
 *
 * It also does not depend on mining_drops.json or MiningData.
 */
public final class MiningSourceResolver {

    private MiningSourceResolver() {
    }

    public static List<String> getPossibleSources(
            String item
    ) {
        String normalized =
                normalize(item);

        if (normalized == null) {
            return List.of();
        }

        boolean silkTouch =
                Configs.Generic
                        .USE_SILK_TOUCH
                        .getBooleanValue();

        boolean shears =
                Configs.Generic
                        .HAS_SHEARS
                        .getBooleanValue();

        return switch (normalized) {

            /*
             * Stone:
             *
             * Silk Touch -> stone
             * Normal     -> cobblestone
             */
            case "minecraft:stone" ->
                    silkTouch
                            ? List.of(
                            "minecraft:stone"
                    )
                            : List.of();

            case "minecraft:cobblestone" ->
                    silkTouch
                            ? List.of()
                            : List.of(
                            "minecraft:stone"
                    );

            case "minecraft:deepslate" ->
                    List.of(
                            "minecraft:deepslate"
                    );

            /*
             * Dirt:
             *
             * Normal mining can produce dirt from dirt and
             * grass blocks.
             *
             * Silk Touch is required if the requested inventory
             * item itself is a grass block.
             */
            case "minecraft:dirt" ->
                    silkTouch
                            ? List.of(
                            "minecraft:dirt"
                    )
                            : List.of(
                            "minecraft:dirt",
                            "minecraft:grass_block"
                    );

            case "minecraft:grass_block" ->
                    silkTouch
                            ? List.of(
                            "minecraft:grass_block"
                    )
                            : List.of();

            case "minecraft:coarse_dirt" ->
                    List.of(
                            "minecraft:coarse_dirt"
                    );

            case "minecraft:sand" ->
                    List.of(
                            "minecraft:sand"
                    );

            case "minecraft:red_sand" ->
                    List.of(
                            "minecraft:red_sand"
                    );

            case "minecraft:gravel" ->
                    List.of(
                            "minecraft:gravel"
                    );

            case "minecraft:netherrack" ->
                    List.of(
                            "minecraft:netherrack"
                    );

            case "minecraft:end_stone" ->
                    List.of(
                            "minecraft:end_stone"
                    );

            case "minecraft:clay" ->
                    silkTouch
                            ? List.of(
                            "minecraft:clay"
                    )
                            : List.of();

            /*
             * Clay blocks drop four clay balls without Silk Touch.
             * Keep this mapping aligned with MiningResolver.getDrops().
             */
            case "minecraft:clay_ball" ->
                    silkTouch
                            ? List.of()
                            : List.of("minecraft:clay");

            case "minecraft:obsidian" ->
                    List.of(
                            "minecraft:obsidian"
                    );

            /*
             * Ore blocks themselves are valid desired inventory items
             * only with Silk Touch. Without it, these blocks yield their
             * normal drops and must not be treated as direct sources.
             */
            case "minecraft:coal_ore",
                 "minecraft:deepslate_coal_ore",
                 "minecraft:iron_ore",
                 "minecraft:deepslate_iron_ore",
                 "minecraft:gold_ore",
                 "minecraft:deepslate_gold_ore",
                 "minecraft:nether_gold_ore",
                 "minecraft:copper_ore",
                 "minecraft:deepslate_copper_ore",
                 "minecraft:diamond_ore",
                 "minecraft:deepslate_diamond_ore",
                 "minecraft:emerald_ore",
                 "minecraft:deepslate_emerald_ore",
                 "minecraft:lapis_ore",
                 "minecraft:deepslate_lapis_ore",
                 "minecraft:redstone_ore",
                 "minecraft:deepslate_redstone_ore",
                 "minecraft:nether_quartz_ore" ->
                    silkTouch
                            ? List.of(normalized)
                            : List.of();

            /*
             * Ores.
             *
             * These intentionally return the physical ore blocks,
             * not the resulting inventory item.
             */
            case "minecraft:coal" ->
                    silkTouch
                            ? List.of()
                            : List.of(
                            "minecraft:coal_ore",
                            "minecraft:deepslate_coal_ore"
                    );

            case "minecraft:raw_iron" ->
                    silkTouch
                            ? List.of()
                            : List.of(
                            "minecraft:iron_ore",
                            "minecraft:deepslate_iron_ore"
                    );

            case "minecraft:raw_gold" ->
                    silkTouch
                            ? List.of()
                            : List.of(
                            "minecraft:gold_ore",
                            "minecraft:deepslate_gold_ore",
                            "minecraft:nether_gold_ore"
                    );

            case "minecraft:raw_copper" ->
                    silkTouch
                            ? List.of()
                            : List.of(
                            "minecraft:copper_ore",
                            "minecraft:deepslate_copper_ore"
                    );

            case "minecraft:diamond" ->
                    silkTouch
                            ? List.of()
                            : List.of(
                            "minecraft:diamond_ore",
                            "minecraft:deepslate_diamond_ore"
                    );

            case "minecraft:emerald" ->
                    silkTouch
                            ? List.of()
                            : List.of(
                            "minecraft:emerald_ore",
                            "minecraft:deepslate_emerald_ore"
                    );

            case "minecraft:lapis_lazuli" ->
                    silkTouch
                            ? List.of()
                            : List.of(
                            "minecraft:lapis_ore",
                            "minecraft:deepslate_lapis_ore"
                    );

            case "minecraft:redstone" ->
                    silkTouch
                            ? List.of()
                            : List.of(
                            "minecraft:redstone_ore",
                            "minecraft:deepslate_redstone_ore"
                    );

            case "minecraft:quartz" ->
                    silkTouch
                            ? List.of()
                            : List.of(
                            "minecraft:nether_quartz_ore"
                    );

            /*
             * Ancient debris is deliberately NOT treated as a
             * source for netherite scrap. Netherite scrap comes
             * from processing ancient debris, not directly from
             * mining it.
             */
            case "minecraft:netherite_scrap" ->
                    List.of();

            /*
             * Ancient debris drops itself directly. Netherite scrap
             * remains a smelting/processing output, not a mining drop.
             */
            case "minecraft:ancient_debris" ->
                    List.of("minecraft:ancient_debris");

            /*
             * Logs/stems.
             */
            case "minecraft:oak_log",
                 "minecraft:spruce_log",
                 "minecraft:birch_log",
                 "minecraft:jungle_log",
                 "minecraft:acacia_log",
                 "minecraft:dark_oak_log",
                 "minecraft:mangrove_log",
                 "minecraft:cherry_log",
                 "minecraft:crimson_stem",
                 "minecraft:warped_stem",
                 "minecraft:bamboo" ->
                    List.of(normalized);

            /*
             * Leaves require Silk Touch or shears.
             */
            case "minecraft:oak_leaves",
                 "minecraft:spruce_leaves",
                 "minecraft:birch_leaves",
                 "minecraft:jungle_leaves",
                 "minecraft:acacia_leaves",
                 "minecraft:dark_oak_leaves",
                 "minecraft:mangrove_leaves",
                 "minecraft:cherry_leaves" ->
                    silkTouch || shears
                            ? List.of(normalized)
                            : List.of();

            /*
             * Direct block -> item resources.
             */
            case "minecraft:granite",
                 "minecraft:diorite",
                 "minecraft:andesite",
                 "minecraft:tuff",
                 "minecraft:calcite",
                 "minecraft:smooth_basalt",
                 "minecraft:basalt",
                 "minecraft:blackstone",
                 "minecraft:soul_sand",
                 "minecraft:soul_soil",
                 "minecraft:magma_block",
                 "minecraft:prismarine",
                 "minecraft:dark_prismarine",
                 "minecraft:purpur_block" ->
                    List.of(normalized);

            /*
             * Ingots are processing outputs, not mining sources.
             */
            case "minecraft:iron_ingot",
                 "minecraft:gold_ingot",
                 "minecraft:copper_ingot" ->
                    List.of();

            default ->
                    List.of();
        };
    }

    public static boolean hasMiningSource(
            String item
    ) {
        return !getPossibleSources(item).isEmpty();
    }

    public static String preferredSource(
            String item
    ) {
        List<String> sources =
                getPossibleSources(item);

        return sources.isEmpty()
                ? null
                : sources.get(0);
    }

    public static boolean requiresNormalDrops(
            String item
    ) {
        String normalized =
                normalize(item);

        if (normalized == null) {
            return false;
        }

        return switch (normalized) {

            case "minecraft:cobblestone",
                 "minecraft:clay_ball",
                 "minecraft:dirt",
                 "minecraft:coal",
                 "minecraft:raw_iron",
                 "minecraft:raw_gold",
                 "minecraft:raw_copper",
                 "minecraft:diamond",
                 "minecraft:emerald",
                 "minecraft:lapis_lazuli",
                 "minecraft:redstone",
                 "minecraft:quartz" ->
                    true;

            default ->
                    false;
        };
    }

    public static boolean requiresSilkTouch(
            String item
    ) {
        String normalized =
                normalize(item);

        if (normalized == null) {
            return false;
        }

        return switch (normalized) {

            case "minecraft:stone",
                 "minecraft:grass_block",
                 "minecraft:clay",
                 "minecraft:coal_ore",
                 "minecraft:deepslate_coal_ore",
                 "minecraft:iron_ore",
                 "minecraft:deepslate_iron_ore",
                 "minecraft:gold_ore",
                 "minecraft:deepslate_gold_ore",
                 "minecraft:nether_gold_ore",
                 "minecraft:copper_ore",
                 "minecraft:deepslate_copper_ore",
                 "minecraft:diamond_ore",
                 "minecraft:deepslate_diamond_ore",
                 "minecraft:emerald_ore",
                 "minecraft:deepslate_emerald_ore",
                 "minecraft:lapis_ore",
                 "minecraft:deepslate_lapis_ore",
                 "minecraft:redstone_ore",
                 "minecraft:deepslate_redstone_ore",
                 "minecraft:nether_quartz_ore",
                 "minecraft:oak_leaves",
                 "minecraft:spruce_leaves",
                 "minecraft:birch_leaves",
                 "minecraft:jungle_leaves",
                 "minecraft:acacia_leaves",
                 "minecraft:dark_oak_leaves",
                 "minecraft:mangrove_leaves",
                 "minecraft:cherry_leaves" ->
                    true;

            default ->
                    false;
        };
    }

    private static String normalize(
            String id
    ) {
        if (id == null
                || id.isBlank()) {
            return null;
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}