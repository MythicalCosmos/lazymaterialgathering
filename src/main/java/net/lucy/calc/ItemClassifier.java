package net.lucy.calc;

import net.lucy.data.ItemSources;
import net.lucy.data.Recipes;
import net.lucy.model.Sources;

import java.util.Set;

public class ItemClassifier {

    /** The primary way to describe where an item comes from, for the Source column etc. */
    public static Sources.Type getSourceType(String itemName) {
        if (Recipes.recipes.containsKey(itemName)) {
            return Sources.Type.CRAFTED;
        }

        Set<Sources> options = ItemSources.sources.get(itemName);

        if (options == null || options.isEmpty()) {
            return Sources.Type.OTHER;
        }

        Sources primary = options.iterator().next();
        return primary.getType();
    }

    /** Every known way to get an item, for screens that want to show more than just the primary one. */
    public static Set<Sources> getAllSources(String itemName) {
        return ItemSources.sources.getOrDefault(itemName, Set.of());
    }
}
