package net.lucy.calc;

import net.lucy.data.ItemEnchantRequirements;
import net.lucy.data.ItemSources;
import net.lucy.data.Recipes;
import net.lucy.model.EnchantRequirement;
import net.lucy.model.SourceType;

public class ItemClassifier {

    public static SourceType getSourceType(String itemName) {
        if (Recipes.recipes.containsKey(itemName)) {
            return SourceType.CRAFTED;
        }
        return ItemSources.sources.getOrDefault(itemName, SourceType.OTHER);
    }

    public static EnchantRequirement getEnchantRequirement(String itemName) {
        return ItemEnchantRequirements.requirements.getOrDefault(itemName, EnchantRequirement.NONE);
    }
}