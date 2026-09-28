package net.lucy.calc;

import net.lucy.data.ObtainSources;
import net.lucy.data.Recipes;
import net.lucy.model.ObtainSource;

import java.util.Set;

public class ItemClassifier {
    /**
     * The primary way to describe where an item comes from.
     */
    public static ObtainSource.Type getSourceType(String itemName) {
        if (Recipes.recipes.containsKey(itemName)) {
            return ObtainSource.Type.CRAFTED;
        }

        Set<ObtainSource> options = ObtainSources.getSources(itemName);
        if (options.isEmpty()) {
            return ObtainSource.Type.OTHER;
        }
        return options.iterator().next().getType();
    }

    /**
     * Returns every known way to obtain an item.
     */
    public static Set<ObtainSource> getAllSources(String itemName) {
        return ObtainSources.getSources(itemName);
    }
}