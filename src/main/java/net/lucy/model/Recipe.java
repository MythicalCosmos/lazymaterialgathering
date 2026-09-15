package net.lucy.model;

import java.util.Map;

public class Recipe {
    public String id;
    public RecipeType type;
    public Map<String, Integer> ingredients;
    public int outputCount;

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