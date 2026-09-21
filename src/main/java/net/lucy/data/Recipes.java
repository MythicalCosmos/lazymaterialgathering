package net.lucy.data;

import net.lucy.calc.RecipeExporter;
import net.lucy.model.Recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Recipes {
    public static Map<String, List<Recipe>> recipes = new HashMap<>();

    // Reads the recipes from the world the player is in. Only does anything the first time.
    public static void ensureLoaded() {
        if (recipes.isEmpty()) {
            recipes = RecipeExporter.collect();
        }
    }
}