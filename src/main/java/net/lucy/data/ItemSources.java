package net.lucy.data;

import net.lucy.model.Sources;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ItemSources {
    // Map each item to a Set of all its valid acquisition methods
    public static final Map<String, Set<Sources>> Sources = new HashMap<>();

    static {
        // Stone: Can be MINED with Silk Touch OR obtained via SMELTED recipe
        Sources.put("stone", Set.of(
                new Sources(Sources.Type.MINED, "SILK_TOUCH"),
                new Sources(Sources.Type.SMELTED)
        ));

        // Potion: Obtained via BREWED recipe
        Sources.put("health_potion", Set.of(
                new Sources(Sources.Type.BREWED)
        ));

        // Charcoal: Is both NATURAL and can be SMELTED
        Sources.put("charcoal", Set.of(
                new Sources(Sources.Type.NATURAL),
                new Sources(Sources.Type.SMELTED)
        ));

        // Diamond: Just regular MINED (no modifier needed)
        Sources.put("diamond", Set.of(
                new Sources(Sources.Type.MINED)
        ));
    }

    /**
     * Optional utility method to quickly check if an item can be obtained via a specific type.
     */
    public static boolean canObtainVia(String item, Sources.Type type) {
        Set<Sources> itemMethods = Sourcess.get(item.toLowerCase());
        if (itemMethods == null) return false;

        return itemMethods.stream().anyMatch(Sources -> Sources.getType() == type);
    }
}
