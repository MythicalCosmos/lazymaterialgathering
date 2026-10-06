package net.lucy.calc;

import net.lucy.config.Configs;

import java.util.List;

/**
 * Determines which physical Minecraft blocks can satisfy a requested
 * inventory item.
 *
 * This class describes gathering sources, not crafting recipes.
 *
 * It intentionally does not use MiningData or mining_drops.json.
 */
public final class MiningSourceResolver {

    private MiningSourceResolver() {
    }

    public static List<String> getPossibleSources(
            String item
    ) {
        String normalized = normalize(item);

        if (normalized == null) {
            return List.of();
        }

        boolean silkTouch =
                Configs.Generic.USE_SILK_TOUCH
                        .getBooleanValue();

        boolean shears =
                Configs.Generic.HAS_SHEARS
                        .getBooleanValue();

        return switch (normalized) {

            /*
             * Stone / cobblestone.
             */
            case "minecraft:stone" ->
                    silkTouch
                            ? List.of("minecraft:stone")
                            : List.of();

            case "minecraft:cobblestone" ->
                    silkTouch
                            ? List.of()
                            : List.of("minecraft:stone");

            case "minecraft:deepslate" ->
                    List.of("minecraft:deepslate");

            /*
             * Dirt family.
             */
            case "minecraft:dirt" ->
                    silkTouch
                            ? List.of("minecraft:dirt")
                            : List.of(
                            "minecraft:dirt",
                            "minecraft:grass_block"
                    );

            case "minecraft:grass_block" ->
                    silkTouch
                            ? List.of("minecraft:grass_block")
                            : List.of();

            case "minecraft:coarse_dirt" ->
                    List.of("minecraft:coarse_dirt");

            /*
             * Basic terrain.
             */
            case "minecraft:sand" ->
                    List.of("minecraft:sand");

            case "minecraft:red_sand" ->
                    List.of("minecraft:red_sand");

            case "minecraft:gravel" ->
                    List.of("minecraft:gravel");

            case "minecraft:netherrack" ->
                    List.of("minecraft:netherrack");

            case "minecraft:end_stone" ->
                    List.of("minecraft:end_stone");

            case "minecraft:clay" ->
                    silkTouch
                            ? List.of("minecraft:clay")
                            : List.of();

            case "minecraft:obsidian" ->
                    List.of("minecraft:obsidian");

            /*
             * Ores.
             *
             * These are PHYSICAL source blocks.
             *
             * The inventory result is determined separately
             * by MiningResolver.
             */
            case "minecraft:coal" ->
                    List.of(
                            "minecraft:coal_ore",
                            "minecraft:deepslate_coal_ore"
                    );

            case "minecraft:raw_iron" ->
                    List.of(
                            "minecraft:iron_ore",
                            "minecraft:deepslate_iron_ore"
                    );

            case "minecraft:raw_gold" ->
                    List.of(
                            "minecraft:gold_ore",
                            "minecraft:deepslate_gold_ore",
                            "minecraft:nether_gold_ore"
                    );

            case "minecraft:raw_copper" ->
                    List.of(
                            "minecraft:copper_ore",
                            "minecraft:deepslate_copper_ore"
                    );

            case "minecraft:diamond" ->
                    List.of(
                            "minecraft:diamond_ore",
                            "minecraft:deepslate_diamond_ore"
                    );

            case "minecraft:emerald" ->
                    List.of(
                            "minecraft:emerald_ore",
                            "minecraft:deepslate_emerald_ore"
                    );

            case "minecraft:lapis_lazuli" ->
                    List.of(
                            "minecraft:lapis_ore",
                            "minecraft:deepslate_lapis_ore"
                    );

            case "minecraft:redstone" ->
                    List.of(
                            "minecraft:redstone_ore",
                            "minecraft:deepslate_redstone_ore"
                    );

            case "minecraft:quartz" ->
                    List.of("minecraft:nether_quartz_ore");

            case "minecraft:netherite_scrap" ->
                    List.of("minecraft:ancient_debris");

            /*
             * Logs.
             */
            case "minecraft:oak_log" ->
                    List.of("minecraft:oak_log");

            case "minecraft:spruce_log" ->
                    List.of("minecraft:spruce_log");

            case "minecraft:birch_log" ->
                    List.of("minecraft:birch_log");

            case "minecraft:jungle_log" ->
                    List.of("minecraft:jungle_log");

            case "minecraft:acacia_log" ->
                    List.of("minecraft:acacia_log");

            case "minecraft:dark_oak_log" ->
                    List.of("minecraft:dark_oak_log");

            case "minecraft:mangrove_log" ->
                    List.of("minecraft:mangrove_log");

            case "minecraft:cherry_log" ->
                    List.of("minecraft:cherry_log");

            case "minecraft:crimson_stem" ->
                    List.of("minecraft:crimson_stem");

            case "minecraft:warped_stem" ->
                    List.of("minecraft:warped_stem");

            case "minecraft:bamboo" ->
                    List.of("minecraft:bamboo");

            /*
             * Leaves.
             *
             * Shears are a legitimate way to obtain the
             * block itself without requiring Silk Touch.
             */
            case "minecraft:oak_leaves" ->
                    silkTouch || shears
                            ? List.of("minecraft:oak_leaves")
                            : List.of();

            case "minecraft:spruce_leaves" ->
                    silkTouch || shears
                            ? List.of("minecraft:spruce_leaves")
                            : List.of();

            case "minecraft:birch_leaves" ->
                    silkTouch || shears
                            ? List.of("minecraft:birch_leaves")
                            : List.of();

            case "minecraft:jungle_leaves" ->
                    silkTouch || shears
                            ? List.of("minecraft:jungle_leaves")
                            : List.of();

            case "minecraft:acacia_leaves" ->
                    silkTouch || shears
                            ? List.of("minecraft:acacia_leaves")
                            : List.of();

            case "minecraft:dark_oak_leaves" ->
                    silkTouch || shears
                            ? List.of("minecraft:dark_oak_leaves")
                            : List.of();

            case "minecraft:mangrove_leaves" ->
                    silkTouch || shears
                            ? List.of("minecraft:mangrove_leaves")
                            : List.of();

            case "minecraft:cherry_leaves" ->
                    silkTouch || shears
                            ? List.of("minecraft:cherry_leaves")
                            : List.of();

            /*
             * Ingots are not physical mining sources.
             *
             * They belong to recipe/crafting resolution.
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

        if (sources.isEmpty()) {
            return null;
        }

        return sources.get(0);
    }

    /**
     * Items requiring normal mining drops rather than the
     * source block itself.
     */
    public static boolean requiresNormalDrops(
            String item
    ) {
        String normalized = normalize(item);

        if (normalized == null) {
            return false;
        }

        return switch (normalized) {
            case "minecraft:cobblestone",
                 "minecraft:dirt",
                 "minecraft:coal",
                 "minecraft:raw_iron",
                 "minecraft:raw_gold",
                 "minecraft:raw_copper",
                 "minecraft:diamond",
                 "minecraft:emerald",
                 "minecraft:lapis_lazuli",
                 "minecraft:redstone",
                 "minecraft:quartz",
                 "minecraft:netherite_scrap" ->
                    true;

            default ->
                    false;
        };
    }

    /**
     * Items whose desired result is specifically the physical
     * block itself.
     */
    public static boolean requiresSilkTouch(
            String item
    ) {
        String normalized = normalize(item);

        if (normalized == null) {
            return false;
        }

        return switch (normalized) {
            case "minecraft:stone",
                 "minecraft:grass_block",
                 "minecraft:clay",
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
        if (id == null || id.isBlank()) {
            return null;
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}