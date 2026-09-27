package net.lucy.model;

import java.util.Map;

public class Recipe {
    public String id;
    public RecipeType type;
    public Map<String, Integer> ingredients;
    public int outputCount;

    // The 3x3 crafting grid, left-to-right then top-to-bottom (index 0 = top-left, 8 =
    // bottom-right), null in empty slots. Only set for CRAFTING recipes, so the recipe
    // can be drawn the way it actually looks in a crafting table, not just as a plain
    // ingredient list. Shapeless recipes have no real layout, so their ingredients are
    // just packed in reading order as an approximation.
    public String[] gridSlots;

    public Recipe(String id, RecipeType type, Map<String, Integer> ingredients, int outputCount) {
        this.id = id;
        this.type = type;
        this.ingredients = ingredients;
        this.outputCount = outputCount;
    }

    public Recipe(String id, Map<String, Integer> ingredients) {
        this(id, RecipeType.CRAFTING, ingredients, 1);
    }
}