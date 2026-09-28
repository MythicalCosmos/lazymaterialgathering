package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.MiningData;
import net.lucy.model.MiningDrop;

import java.util.Map;
import java.util.TreeMap;

/**
 * Converts schematic block counts into the items that would be obtained
 * by mining those blocks.
 *
 * This class contains calculation logic only. Static Minecraft drop data
 * is supplied by MiningData.
 */
public final class MiningResolver {
    private MiningResolver() {
    }

    /**
     * Calculates what one or more blocks produce.
     *
     * @param blockName       block being mined
     * @param blockQuantity   number of those blocks
     * @param hasSilkTouch   whether Silk Touch is available
     * @param hasShears      whether shears are available
     * @return item name -> quantity
     */
    public static Map<String, Long> getDrops(String blockName, long blockQuantity, boolean hasSilkTouch, boolean hasShears) {
        Map<String, Long> result = new TreeMap<>();
        /*
         * A potted plant produces two items:
         *
         *   flower pot
         *   contained plant
         *
         * MiningDrop only models one drop, so this special case lives
         * outside the normal drop table.
         */
        String pottedPlant = MiningData.getPottedPlant(blockName);
        if (pottedPlant != null) {
            result.merge("flower_pot", blockQuantity, Long::sum);
            result.merge(pottedPlant, blockQuantity, Long::sum);
            return result;
        }

        MiningDrop drop = MiningData.getDrop(blockName);
        /*
         * Most Minecraft blocks simply drop themselves.
         * They do not need an explicit entry in mining_drops.json.
         */
        if (drop == null) {
            result.put(blockName, blockQuantity);
            return result;
        }

        boolean useSpecialDrop = hasSilkTouch || (hasShears && drop.shearsAlsoWork());
        String itemName = useSpecialDrop ? drop.getSilkTouchDrop() : drop.getNormalDrop();
        int perBlockCount = useSpecialDrop ? drop.getSilkTouchCount() : drop.getNormalCount();
        /*
         * A null drop means that the block produces nothing under
         * the current tool conditions.
         *
         * Examples include glass without Silk Touch.
         */
        if (itemName == null) {
            return result;
        }

        result.put(itemName, blockQuantity * perBlockCount);
        return result;
    }

    /**
     * Resolves every block in a schematic into mined items.
     */
    public static Map<String, Long> resolveMinedItems(Map<String, Long> blockCounts, boolean hasSilkTouch, boolean hasShears) {
        Map<String, Long> minedTotals = new TreeMap<>();
        for (Map.Entry<String, Long> entry :
                blockCounts.entrySet()) {
            Map<String, Long> drops = getDrops(entry.getKey(), entry.getValue(), hasSilkTouch, hasShears);
            for (Map.Entry<String, Long> dropEntry : drops.entrySet()) {
                minedTotals.merge(dropEntry.getKey(), dropEntry.getValue(), Long::sum);
            }
        }
        return minedTotals;
    }

    /**
     * Convenience overload for callers that only specify Silk Touch.
     *
     * The configured shears setting is used automatically.
     */
    public static Map<String, Long> resolveMinedItems(Map<String, Long> blockCounts, boolean hasSilkTouch) {
        return resolveMinedItems(blockCounts, hasSilkTouch, Configs.Generic.HAS_SHEARS.getBooleanValue());
    }
}