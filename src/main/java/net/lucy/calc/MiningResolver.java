package net.lucy.calc;

import net.lucy.config.Configs;

import java.util.Map;
import java.util.TreeMap;

/**
 * Converts physical blocks into the inventory items they produce.
 *
 * This implementation deliberately does NOT depend on:
 *
 *   MiningData
 *   mining_drops.json
 *
 * It models the deterministic/common drops required by the gathering
 * planner.
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
        if (block.equals("minecraft:stone")) {

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
        if (block.equals("minecraft:grass_block")) {

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
         */
        if (block.equals("minecraft:clay")) {

            if (hasSilkTouch) {

                result.put(
                        "minecraft:clay",
                        blockQuantity
                );

            } else {

                result.put(
                        "minecraft:clay_ball",
                        blockQuantity * 4L
                );
            }

            return result;
        }

        /*
         * Leaves.
         *
         * With shears or Silk Touch, the leaf block itself
         * is obtained.
         *
         * Without either, leaves are not treated as a reliable
         * source for the requested leaf block.
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
         * Ores.
         *
         * Silk Touch means the ore block itself is obtained.
         * Without Silk Touch, the normal resource is obtained.
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

                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:raw_copper",
                        blockQuantity * 4L
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
                        blockQuantity * 4L
                );

                return result;
            }

            case "minecraft:redstone_ore",
                 "minecraft:deepslate_redstone_ore" -> {

                result.put(
                        hasSilkTouch
                                ? block
                                : "minecraft:redstone",
                        blockQuantity * 4L
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
         * Blocks whose mined item is simply themselves.
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

        for (Map.Entry<String, Long> entry :
                blockCounts.entrySet()) {

            Map<String, Long> drops =
                    getDrops(
                            entry.getKey(),
                            entry.getValue(),
                            hasSilkTouch,
                            hasShears
                    );

            for (Map.Entry<String, Long> drop :
                    drops.entrySet()) {

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
                Configs.Generic.HAS_SHEARS
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

        if (id == null || id.isBlank()) {
            return "";
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}