package net.lucy.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Crops whose "what you actually get from breaking it right now" changes with age: below
 * the listed age, breaking it only gives the seed; at or above it, the block already
 * resolves to the right item on its own (through MiningDrops, or by already matching its
 * own item name), so nothing further is needed here for the mature case.
 *
 * Carrots, potatoes and nether wart are NOT listed here: in vanilla, breaking them at any
 * age always gives the same item (the carrot/potato itself doubles as its own "seed"),
 * so there's no immature/mature item swap to model for those.
 */
public class CropAges {
    public static final Map<String, Integer> MATURE_AGE = new HashMap<>();
    public static final Map<String, String> IMMATURE_DROP = new HashMap<>();

    static {
        MATURE_AGE.put("wheat", 7);
        IMMATURE_DROP.put("wheat", "wheat_seeds");

        MATURE_AGE.put("beetroots", 3);
        IMMATURE_DROP.put("beetroots", "beetroot_seeds");

        MATURE_AGE.put("torchflower_crop", 1);
        IMMATURE_DROP.put("torchflower_crop", "torchflower_seeds");
    }

    /**
     * The item to count instead of the block itself, if this is an immature crop;
     * null if it's not one of the crops listed above, or if it's already mature (or its
     * age couldn't be read, in which case it's assumed mature — the previous behaviour).
     */
    public static String getImmatureDrop(String blockName, Map<String, String> states) {
        Integer matureAge = MATURE_AGE.get(blockName);
        if (matureAge == null) {
            return null;
        }

        String ageText = states.get("age");
        if (ageText == null) {
            return null;
        }

        try {
            int age = Integer.parseInt(ageText);
            return age < matureAge ? IMMATURE_DROP.get(blockName) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}