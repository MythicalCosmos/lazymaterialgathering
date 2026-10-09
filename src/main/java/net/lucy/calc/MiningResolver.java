package net.lucy.calc;

import net.lucy.config.Configs;

import java.util.Map;
import java.util.TreeMap;

/**
 * Converts physical blocks into the inventory items they produce.
 *
 * This is deliberately explicit and does not depend on
 * mining_drops.json or MiningData.
 */
public final class MiningResolver {

    private MiningResolver() {
    }

    public static Map<String, Long> getDrops(
            String blockName,
            long blockQuantity,
            boolean hasSilkTouch,
            boolean hasShears
    ) {
        Map<String, Long> result =
                new TreeMap<>();

        if (blockName == null
                || blockName.isBlank()
                || blockQuantity <= 0L) {

            return result;
        }

        String block =
                normalize(blockName);

        /*
         * Stone.
         */
        if (block.equals(
                "minecraft:stone"
        )) {

            result.put(
                    hasSilkTouch
                            ? "minecraft:stone"
                            : "minecraft:cobblestone",
                    blockQuantity
            );

            return result;
        }

        /*
         * Grass block.
         */
        if (block.equals(
                "minecraft:grass_block"
        )) {

            result.put(
                    hasSilkTouch
                            ? "minecraft:grass_block"
                            : "minecraft:dirt",
                    blockQuantity
            );

            return result;
        }

        /*
         * Clay.
         *
         * Without Silk Touch, a clay block produces four
         * clay balls.
         */
        if (block.equals(
                "minecraft:clay"
        )) {

            result.put(
                    hasSilkTouch
                            ? "minecraft:clay"
                            : "minecraft:clay_ball",
                    hasSilkTouch
                            ? blockQuantity
                            : blockQuantity * 4L
            );

            return result;
        }

        /*
         * Leaves.
         */
        if (isLeaf(block)) {

            if (hasSilkTouch || hasShears) {
                result.put(
                        block,
                        blockQuantity
                );
            }

            return result;
        }

        /*
         * Ore drops.
         */
        switch (block) {

            case "minecraft:coal_ore",
                 "minecraft:deepslate_coal_ore" -> {

                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:coal",
                        blockQuantity
                );

                return result;
            }

            case "minecraft:iron_ore",
                 "minecraft:deepslate_iron_ore" -> {

                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:raw_iron",
                        blockQuantity
                );

                return result;
            }

            case "minecraft:gold_ore",
                 "minecraft:deepslate_gold_ore",
                 "minecraft:nether_gold_ore" -> {

                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:raw_gold",
                        blockQuantity
                );

                return result;
            }

            case "minecraft:copper_ore",
                 "minecraft:deepslate_copper_ore" -> {

                /*
                 * Copper ore drops raw copper in quantities
                 * greater than one.
                 */
                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:raw_copper",
                        hasSilkTouch
                                ? blockQuantity
                                : blockQuantity * 4L
                );

                return result;
            }

            case "minecraft:diamond_ore",
                 "minecraft:deepslate_diamond_ore" -> {

                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:diamond",
                        blockQuantity
                );

                return result;
            }

            case "minecraft:emerald_ore",
                 "minecraft:deepslate_emerald_ore" -> {

                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:emerald",
                        blockQuantity
                );

                return result;
            }

            case "minecraft:lapis_ore",
                 "minecraft:deepslate_lapis_ore" -> {

                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:lapis_lazuli",
                        hasSilkTouch
                                ? blockQuantity
                                : blockQuantity * 4L
                );

                return result;
            }

            case "minecraft:redstone_ore",
                 "minecraft:deepslate_redstone_ore" -> {

                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:redstone",
                        hasSilkTouch
                                ? blockQuantity
                                : blockQuantity * 4L
                );

                return result;
            }

            case "minecraft:nether_quartz_ore" -> {

                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:quartz",
                        blockQuantity
                );

                return result;
            }

            case "minecraft:ancient_debris" -> {

                result.put(
                        "minecraft:ancient_debris",
                        blockQuantity
                );

                return result;
            }
        }

        /*
         * Direct block -> item resources.
         */
        switch (block) {

            case "minecraft:dirt",
                 "minecraft:coarse_dirt",
                 "minecraft:sand",
                 "minecraft:red_sand",
                 "minecraft:gravel",
                 "minecraft:netherrack",
                 "minecraft:end_stone",
                 "minecraft:obsidian",
                 "minecraft:deepslate",
                 "minecraft:granite",
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
                 "minecraft:purpur_block",
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
                 "minecraft:bamboo" -> {

                result.put(
                        block,
                        blockQuantity
                );
            }
        }

        return result;
    }

    public static long getExpectedDropCount(
            String blockName,
            String desiredItem,
            boolean hasSilkTouch,
            boolean hasShears
    ) {
        if (blockName == null
                || desiredItem == null) {
            return 0L;
        }

        Map<String, Long> drops =
                getDrops(
                        blockName,
                        1L,
                        hasSilkTouch,
                        hasShears
                );

        return drops.getOrDefault(
                normalize(desiredItem),
                0L
        );
    }

    public static Map<String, Long> resolveMinedItems(
            Map<String, Long> blockCounts,
            boolean hasSilkTouch,
            boolean hasShears
    ) {
        Map<String, Long> result =
                new TreeMap<>();

        if (blockCounts == null) {
            return result;
        }

        for (Map.Entry<String, Long> entry
                : blockCounts.entrySet()) {

            Map<String, Long> drops =
                    getDrops(
                            entry.getKey(),
                            entry.getValue(),
                            hasSilkTouch,
                            hasShears
                    );

            for (Map.Entry<String, Long> drop
                    : drops.entrySet()) {

                result.merge(
                        drop.getKey(),
                        drop.getValue(),
                        Long::sum
                );
            }
        }

        return result;
    }

    public static Map<String, Long> resolveMinedItems(
            Map<String, Long> blockCounts,
            boolean hasSilkTouch
    ) {
        return resolveMinedItems(
                blockCounts,
                hasSilkTouch,
                Configs.Generic
                        .HAS_SHEARS
                        .getBooleanValue()
        );
    }

    private static boolean isLeaf(
            String block
    ) {
        return switch (block) {

            case "minecraft:oak_leaves",
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
            return "";
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}