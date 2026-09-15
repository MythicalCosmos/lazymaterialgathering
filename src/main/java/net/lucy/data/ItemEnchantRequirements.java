package net.lucy.data;

import net.lucy.model.EnchantRequirement;
import java.util.HashMap;
import java.util.Map;

public class ItemEnchantRequirements {
    public static Map<String, EnchantRequirement> requirements = new HashMap<>();

    static {
        requirements.put("grass_block", EnchantRequirement.SILK_TOUCH);
        requirements.put("glass", EnchantRequirement.SILK_TOUCH);
        requirements.put("ice", EnchantRequirement.SILK_TOUCH);
        requirements.put("spawner", EnchantRequirement.SILK_TOUCH);
        requirements.put("diamond_ore", EnchantRequirement.SILK_TOUCH_OR_FORTUNE);
        requirements.put("coal_ore", EnchantRequirement.SILK_TOUCH_OR_FORTUNE);
        requirements.put("redstone_ore", EnchantRequirement.SILK_TOUCH_OR_FORTUNE);
    }
}