package net.lucy.data;

import net.lucy.model.MiningDrop;
import java.util.HashMap;
import java.util.Map;

public class MiningDrops {
    public static Map<String, MiningDrop> drops = new HashMap<>();

    static {
        drops.put("stone", new MiningDrop("cobblestone", "stone"));
        drops.put("deepslate", new MiningDrop("cobbled_deepslate", "deepslate"));
        drops.put("grass_block", new MiningDrop("dirt", "grass_block"));
        drops.put("coal_ore", new MiningDrop("coal", "coal_ore"));
        drops.put("diamond_ore", new MiningDrop("diamond", "diamond_ore"));
        drops.put("redstone_ore", new MiningDrop("redstone", "redstone_ore"));
        drops.put("glass", new MiningDrop(null, "glass"));
        drops.put("ice", new MiningDrop(null, "ice"));
    }
}