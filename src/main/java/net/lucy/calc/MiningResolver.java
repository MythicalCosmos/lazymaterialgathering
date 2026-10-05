package net.lucy.calc;

import net.lucy.config.Configs;

import java.util.Map;
import java.util.TreeMap;

/**
 * Converts planned physical block counts into the inventory
 * items those blocks produce.
 *
 * This class intentionally does not use MiningData or
 * mining_drops.json.
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

        String block = normalize(blockName);

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
         * Deepslate.
         */
        if (block.equals("minecraft:deepslate")) {
            result.put(
                    "minecraft:deepslate",
                    blockQuantity
            );

            return result;
        }

        /*
         * Ores.
         */
        switch (block) {

            case "minecraft:coal_ore",
                 "minecraft:deepslate_coal_ore" ->
                    result.put(
                            "minecraft:coal",
                            blockQuantity
                    );

            case "minecraft:iron_ore",
                 "minecraft:deepslate_iron_ore" ->
                    result.put(
                            "minecraft:raw_iron",
                            blockQuantity
                    );

            case "minecraft:gold_ore",
                 "minecraft:deepslate_gold_ore",
                 "minecraft:nether_gold_ore" ->
                    result.put(
                            "minecraft:raw_gold",
                            blockQuantity
                    );

            case "minecraft:copper_ore",
                 "minecraft:deepslate_copper_ore" ->
                    result.put(
                            "minecraft:raw_copper",
                            blockQuantity * 4L
                    );

            case "minecraft:diamond_ore",
                 "minecraft:deepslate_diamond_ore" ->
                    result.put(
                            "minecraft:diamond",
                            blockQuantity
                    );

            case "minecraft:emerald_ore",
                 "minecraft:deepslate_emerald_ore" ->
                    result.put(
                            "minecraft:emerald",
                            blockQuantity
                    );

            case "minecraft:lapis_ore",
                 "minecraft:deepslate_lapis_ore" ->
                    result.put(
                            "minecraft:lapis_lazuli",
                            blockQuantity * 4L
                    );

            case "minecraft:redstone_ore",
                 "minecraft:deepslate_redstone_ore" ->
                    result.put(
                            "minecraft:redstone",
                            blockQuantity * 4L
                    );

            case "minecraft:nether_quartz_ore" ->
                    result.put(
                            "minecraft:quartz",
                            blockQuantity
                    );

            case "minecraft:ancient_debris" ->
                    result.put(
                            "minecraft:ancient_debris",
                            blockQuantity
                    );

            default -> resolveCommonBlock(
                    result,
                    block,
                    blockQuantity,
                    hasSilkTouch,
                    hasShears
            );
        }

        return result;
    }

    private static void resolveCommonBlock(
            Map<String, Long> result,
            String block,
            long quantity,
            boolean hasSilkTouch,
            boolean hasShears
    ) {
        /*
         * For ordinary blocks whose block item is the same item,
         * mining produces that item.
         */
        switch (block) {

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
                    result.put(block, quantity);

            case "minecraft:grass_block" ->
                    result.put(
                            hasSilkTouch
                                    ? "minecraft:grass_block"
                                    : "minecraft:dirt",
                            quantity
                    );

            default ->
                /*
                 * Unknown blocks are deliberately not assumed
                 * to have an identical item drop.
                 */
                    yieldNothing(result);
        }
    }

    private static void yieldNothing(
            Map<String, Long> result
    ) {
        // Intentionally empty.
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
                Configs.Generic.HAS_SHEARS.getBooleanValue()
        );
    }

    private static String normalize(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}