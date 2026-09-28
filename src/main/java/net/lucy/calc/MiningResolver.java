package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.MiningData;
import net.lucy.model.MiningDrop;

import java.util.Map;
import java.util.TreeMap;

public final class MiningResolver {
    private MiningResolver() {
    }

    public static Map<String, Long> getDrops(String blockName, long blockQuantity, boolean hasSilkTouch, boolean hasShears) {
        Map<String, Long> result = new TreeMap<>();
        String pottedPlant = MiningData.getPottedPlant(blockName);
        if (pottedPlant != null) {
            result.merge("flower_pot", blockQuantity, Long::sum);
            result.merge(pottedPlant, blockQuantity, Long::sum);
            return result;
        }

        MiningDrop drop = MiningData.getDrop(blockName);
        /*
         * No special entry means the block is assumed to drop itself.
         */
        if (drop == null) {
            result.put(blockName, blockQuantity);
            return result;
        }

        boolean special = hasSilkTouch || (hasShears && drop.shearsAlsoWork());
        String itemName = special ? drop.getSilkTouchDrop() : drop.getNormalDrop();
        int count = special ? drop.getSilkTouchCount() : drop.getNormalCount();
        if (itemName == null || count <= 0) {
            return result;
        }

        result.put(itemName, blockQuantity * count);
        return result;
    }

    public static Map<String, Long>
    resolveMinedItems(Map<String, Long> blockCounts, boolean hasSilkTouch, boolean hasShears) {
        Map<String, Long> result = new TreeMap<>();
        for (Map.Entry<String, Long> entry : blockCounts.entrySet()) {
            Map<String, Long> drops = getDrops(entry.getKey(), entry.getValue(), hasSilkTouch, hasShears);
            for (Map.Entry<String, Long> drop : drops.entrySet()) {
                result.merge(drop.getKey(), drop.getValue(), Long::sum);
            }
        }
        return result;
    }

    public static Map<String, Long>
    resolveMinedItems(Map<String, Long> blockCounts, boolean hasSilkTouch) {
        return resolveMinedItems(blockCounts, hasSilkTouch, Configs.Generic.HAS_SHEARS.getBooleanValue());
    }
}