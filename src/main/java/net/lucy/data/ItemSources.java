package net.lucy.data;

import net.lucy.model.RecipeType;
import net.lucy.model.SourceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemSources {
    // A Map that holds a List of valid sources/recipes for each item
    public static final Map<String, List<SourceInfo>> sources = new HashMap<>();

    static {
        // Stone setup: Can be MINED (with Silk Touch) OR obtained via SMELTING recipe
        sources.put("stone", List.of(
                new SourceInfo(SourceType.MINED, "SILK_TOUCH"),
                new SourceInfo(RecipeType.SMELTING)
        ));

        // Example for Diamond: Mined normally (no modifier needed)
        sources.put("diamond_ore", List.of(
                new SourceInfo(SourceType.MINED)
        ));

        // Example for Charcoal: Obtained via Smelting recipe
        sources.put("charcoal", List.of(
                new SourceInfo(RecipeType.SMELTING)
        ));
    }

    /**
     * A lightweight helper class to combine SourceType, RecipeType, and Modifiers.
     */
    public static class SourceInfo {
        private final SourceType sourceType;
        private final RecipeType recipeType;
        private final String modifier;

        // Constructor for physical sources (e.g., MINED, MOB_DROP)
        public SourceInfo(SourceType sourceType) {
            this(sourceType, null, null);
        }

        // Constructor for physical sources with modifiers (e.g., MINED with SILK_TOUCH)
        public SourceInfo(SourceType sourceType, String modifier) {
            this(sourceType, null, modifier);
        }

        // Constructor for craftable/smeltable items linked to recipes
        public SourceInfo(RecipeType recipeType) {
            this(null, recipeType, null);
        }

        // Main constructor
        private SourceInfo(SourceType sourceType, RecipeType recipeType, String modifier) {
            this.sourceType = sourceType;
            this.recipeType = recipeType;
            this.modifier = modifier;
        }

        // Getters
        public SourceType getSourceType() { return sourceType; }
        public RecipeType getRecipeType() { return recipeType; }
        public String getModifier() { return modifier; }

        public boolean isRecipe() { return recipeType != null; }
        public boolean isPhysicalSource() { return sourceType != null; }

        @Override
        public String toString() {
            if (isRecipe()) return "Recipe:" + recipeType;
            return "Source:" + sourceType + (modifier != null ? "[" + modifier + "]" : "");
        }
    }
}
