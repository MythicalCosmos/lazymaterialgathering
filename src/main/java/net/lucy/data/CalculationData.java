package net.lucy.data;

import net.lucy.calc.MiningResolver;
import net.lucy.calc.RawMaterials;
import net.lucy.config.Configs;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Central storage for the current schematic calculation.
 */
public final class CalculationData {
    private static Map<String, Long> blockCounts = new TreeMap<>();
    private static Map<String, Long> rawMaterials = new TreeMap<>();
    /*
     * raw material -> item that directly used it -> quantity used
     */
    private static Map<String, Map<String, Long>> rawMaterialUsage = new TreeMap<>();
    private CalculationData() {
    }

    public static void setResults(Map<String, Long> newBlockCounts, RawMaterials.Result result) {
        blockCounts = new TreeMap<>(newBlockCounts);
        rawMaterials = new TreeMap<>(result.totals);
        rawMaterialUsage = new TreeMap<>(result.usedIn);
    }

    public static boolean hasResults() {
        return !blockCounts.isEmpty();
    }

    public static Map<String, Long> getBlockCounts() {
        return Collections.unmodifiableMap(blockCounts);
    }

    public static Map<String, Long> getRawMaterials() {
        return Collections.unmodifiableMap(rawMaterials);
    }
    public static Map<String, Long> getUsageFor(String rawMaterialName) {
        return rawMaterialUsage.getOrDefault(rawMaterialName, Collections.emptyMap());
    }
    /**
     * Converts schematic blocks into the items that need
     * to be obtained/mine/crafted.
     */
    public static Map<String, Long> getMinedItems() {
        return MiningResolver.resolveMinedItems(blockCounts, Configs.Generic.USE_SILK_TOUCH.getBooleanValue());
    }
    /**
     * Recalculates only the raw-material stage.
     */
    public static void recalculateRawMaterials() {
        RawMaterials.Result result = RawMaterials.calculateDetailed(getMinedItems());
        rawMaterials = new TreeMap<>(result.totals);
        rawMaterialUsage = new TreeMap<>(result.usedIn);
    }
    /**
     * Rebuilds the complete result from the currently
     * stored schematic block counts.
     */
    public static void recalculateAll() {
        if (blockCounts.isEmpty()) {
            rawMaterials.clear();
            rawMaterialUsage.clear();
            return;
        }

        Map<String, Long> mined = getMinedItems();
        RawMaterials.Result result = RawMaterials.calculateDetailed(mined);
        rawMaterials = new TreeMap<>(result.totals);
        rawMaterialUsage = new TreeMap<>(result.usedIn);
    }
}