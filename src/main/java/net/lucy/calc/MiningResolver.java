package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.MiningDrops;
import net.lucy.model.MiningDrop;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MiningResolver {

    // Breaking a potted plant gives you the empty flower pot AND the plant it held \u2014
    // two separate items from one block, which MiningDrop (one drop, one count) can't
    // represent on its own. Block name -> the plain item id of the plant it holds.
    private static final Map<String, String> POTTED_PLANTS = new HashMap<>();
    static {
        POTTED_PLANTS.put("potted_acacia_sapling", "acacia_sapling");
        POTTED_PLANTS.put("potted_allium", "allium");
        POTTED_PLANTS.put("potted_azalea_bush", "azalea");
        POTTED_PLANTS.put("potted_azure_bluet", "azure_bluet");
        POTTED_PLANTS.put("potted_bamboo", "bamboo");
        POTTED_PLANTS.put("potted_birch_sapling", "birch_sapling");
        POTTED_PLANTS.put("potted_blue_orchid", "blue_orchid");
        POTTED_PLANTS.put("potted_brown_mushroom", "brown_mushroom");
        POTTED_PLANTS.put("potted_cactus", "cactus");
        POTTED_PLANTS.put("potted_cherry_sapling", "cherry_sapling");
        POTTED_PLANTS.put("potted_cornflower", "cornflower");
        POTTED_PLANTS.put("potted_crimson_fungus", "crimson_fungus");
        POTTED_PLANTS.put("potted_crimson_roots", "crimson_roots");
        POTTED_PLANTS.put("potted_dandelion", "dandelion");
        POTTED_PLANTS.put("potted_dark_oak_sapling", "dark_oak_sapling");
        POTTED_PLANTS.put("potted_dead_bush", "dead_bush");
        POTTED_PLANTS.put("potted_fern", "fern");
        POTTED_PLANTS.put("potted_flowering_azalea_bush", "flowering_azalea");
        POTTED_PLANTS.put("potted_jungle_sapling", "jungle_sapling");
        POTTED_PLANTS.put("potted_lily_of_the_valley", "lily_of_the_valley");
        POTTED_PLANTS.put("potted_mangrove_propagule", "mangrove_propagule");
        POTTED_PLANTS.put("potted_oak_sapling", "oak_sapling");
        POTTED_PLANTS.put("potted_orange_tulip", "orange_tulip");
        POTTED_PLANTS.put("potted_oxeye_daisy", "oxeye_daisy");
        POTTED_PLANTS.put("potted_pink_tulip", "pink_tulip");
        POTTED_PLANTS.put("potted_poppy", "poppy");
        POTTED_PLANTS.put("potted_red_mushroom", "red_mushroom");
        POTTED_PLANTS.put("potted_red_tulip", "red_tulip");
        POTTED_PLANTS.put("potted_spruce_sapling", "spruce_sapling");
        POTTED_PLANTS.put("potted_torchflower", "torchflower");
        POTTED_PLANTS.put("potted_warped_fungus", "warped_fungus");
        POTTED_PLANTS.put("potted_warped_roots", "warped_roots");
        POTTED_PLANTS.put("potted_white_tulip", "white_tulip");
        POTTED_PLANTS.put("potted_wither_rose", "wither_rose");
    }

    /**
     * What one block gives you, as (item name -> how many). Usually a single entry, but
     * a potted plant gives two (the pot and the plant).
     */
    public static Map<String, Long> getDrops(String blockName, long blockQuantity, boolean hasSilkTouch, boolean hasShears) {
        Map<String, Long> result = new TreeMap<>();

        String pottedPlant = POTTED_PLANTS.get(blockName);
        if (pottedPlant != null) {
            result.merge("flower_pot", blockQuantity, Long::sum);
            result.merge(pottedPlant, blockQuantity, Long::sum);
            return result;
        }

        MiningDrop drop = MiningDrops.drops.get(blockName);
        if (drop == null) {
            result.put(blockName, blockQuantity);
            return result;
        }

        boolean useSpecialDrop = hasSilkTouch || (hasShears && drop.shearsAlsoWork);

        String itemName = useSpecialDrop ? drop.silkTouchDrop : drop.normalDrop;
        int perBlockCount = useSpecialDrop ? drop.silkTouchCount : drop.normalCount;

        if (itemName == null) {
            return result; // nothing without the right tool
        }

        result.put(itemName, blockQuantity * perBlockCount);
        return result;
    }

    public static Map<String, Long> resolveMinedItems(Map<String, Long> blockCounts, boolean hasSilkTouch, boolean hasShears) {
        Map<String, Long> minedTotals = new TreeMap<>();

        for (Map.Entry<String, Long> entry : blockCounts.entrySet()) {
            for (Map.Entry<String, Long> dropEntry : getDrops(entry.getKey(), entry.getValue(), hasSilkTouch, hasShears).entrySet()) {
                minedTotals.merge(dropEntry.getKey(), dropEntry.getValue(), Long::sum);
            }
        }

        return minedTotals;
    }

    // Kept for any existing callers that only pass Silk Touch; reads the shears setting itself.
    public static Map<String, Long> resolveMinedItems(Map<String, Long> blockCounts, boolean hasSilkTouch) {
        return resolveMinedItems(blockCounts, hasSilkTouch, Configs.Generic.HAS_SHEARS.getBooleanValue());
    }
}