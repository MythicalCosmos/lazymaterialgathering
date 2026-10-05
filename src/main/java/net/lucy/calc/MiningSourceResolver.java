package net.lucy.calc;

import net.lucy.config.Configs;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the physical Minecraft block(s) that should be mined
 * to obtain a requested inventory item.
 *
 * This intentionally does NOT assume that every item with a
 * corresponding block is a valid mining target.
 */
public final class MiningSourceResolver {

    private MiningSourceResolver() {
    }

    public static List<String> getPossibleSources(String item) {
        String normalized = normalize(item);

        if (normalized == null) {
            return List.of();
        }

        boolean silkTouch =
                Configs.Generic.USE_SILK_TOUCH.getBooleanValue();

        return switch (normalized) {

            /*
             * Stone/cobblestone behavior.
             *
             * Without Silk Touch:
             *   stone -> cobblestone
             *
             * With Silk Touch:
             *   stone -> stone
             */
            case "minecraft:cobblestone" ->
                    silkTouch
                            ? List.of()
                            : List.of("minecraft:stone");

            case "minecraft:stone" ->
                    silkTouch
                            ? List.of("minecraft:stone")
                            : List.of();

            case "minecraft:deepslate" ->
                    List.of("minecraft:deepslate");

            /*
             * Ores and raw materials.
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

            case "minecraft:iron_ore" ->
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

            case "minecraft:gold_ore" ->
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

            case "minecraft:copper_ore" ->
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
                    List.of(
                            "minecraft:nether_quartz_ore"
                    );

            case "minecraft:netherite_scrap" ->
                    List.of(
                            "minecraft:ancient_debris"
                    );

            /*
             * Blocks that are themselves the desired inventory item.
             *
             * These are deliberately explicit instead of falling back
             * to "item -> block" for every registered Minecraft item.
             */
            case "minecraft:dirt",
                 "minecraft:coarse_dirt",
                 "minecraft:sand",
                 "minecraft:red_sand",
                 "minecraft:gravel",
                 "minecraft:netherrack",
                 "minecraft:end_stone",
                 "minecraft:clay",
                 "minecraft:obsidian",
                 "minecraft:oak_log",
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

            default ->
                    List.of();
        };
    }

    /**
     * Returns the preferred source.
     *
     * The ordering in getPossibleSources() is intentional.
     */
    public static String preferredSource(String item) {
        List<String> sources = getPossibleSources(item);

        if (sources.isEmpty()) {
            return null;
        }

        return sources.get(0);
    }

    /**
     * True when the item has at least one explicitly supported
     * physical mining source.
     */
    public static boolean hasMiningSource(String item) {
        return !getPossibleSources(item).isEmpty();
    }

    private static String normalize(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}