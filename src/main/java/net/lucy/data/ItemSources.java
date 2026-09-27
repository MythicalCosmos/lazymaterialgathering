package net.lucy.data;

import net.lucy.model.Sources;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ItemSources {
    public static final Map<String, Set<Sources>> sources = new HashMap<>();

    static {
        // Stone: Can be MINED with Silk Touch OR obtained via SMELTED recipe
        sources.put("stone", Set.of(
                new Sources(Sources.Type.MINED, "SILK_TOUCH"),
                new Sources(Sources.Type.SMELTED)
        ));

        // Charcoal: Is both NATURAL and can be SMELTED
        sources.put("charcoal", Set.of(
                new Sources(Sources.Type.NATURAL),
                new Sources(Sources.Type.SMELTED)
        ));

        // Diamond: Just regular MINED (no modifier needed)
        sources.put("diamond", Set.of(
                new Sources(Sources.Type.MINED)
        ));
    }

    public static boolean canObtainVia(String item, Sources.Type type) {
        Set<Sources> itemMethods = sources.get(item.toLowerCase());
        if (itemMethods == null) return false;

        return itemMethods.stream().anyMatch(source -> source.getType() == type);
    }
}
