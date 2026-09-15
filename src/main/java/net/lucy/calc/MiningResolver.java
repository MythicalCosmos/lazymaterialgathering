package net.lucy.calc;

import net.lucy.data.MiningDrops;
import net.lucy.model.MiningDrop;

import java.util.Map;
import java.util.TreeMap;

public class MiningResolver {

    public static String getDrop(String blockName, boolean hasSilkTouch) {
        MiningDrop drop = MiningDrops.drops.get(blockName);
        if (drop == null) {
            return blockName;
        }
        return hasSilkTouch ? drop.silkTouchDrop : drop.normalDrop;
    }

    public static Map<String, Long> resolveMinedItems(Map<String, Long> blockCounts, boolean hasSilkTouch) {
        Map<String, Long> minedTotals = new TreeMap<>();
        for (Map.Entry<String, Long> entry : blockCounts.entrySet()) {
            String drop = getDrop(entry.getKey(), hasSilkTouch);
            if (drop == null) {
                continue;
            }
            minedTotals.merge(drop, entry.getValue(), Long::sum);
        }
        return minedTotals;
    }
}