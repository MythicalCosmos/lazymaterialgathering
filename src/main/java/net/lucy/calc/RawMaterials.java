package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RawMaterials {

    public static Map<String, Long> calculate(Map<String, Long> blockCounts) {

        Map<String, Long> rawMaterialTotals = new TreeMap<>();
        for (Map.Entry<String, Long> entry : blockCounts.entrySet()) {
            resolveRawMaterials(entry.getKey(), entry.getValue(), rawMaterialTotals);
        }
        return rawMaterialTotals;
    }

    private static void resolveRawMaterials(String itemName, long quantity, Map<String, Long> totals) {
        List<Recipe> options = Recipes.recipes.get(itemName);

        if (options == null || options.isEmpty()) {
            totals.merge(itemName, quantity, Long::sum);
            return;
        }

        Recipe chosen = selectRecipe(itemName, options);

        if (chosen.type == RecipeType.NATURAL) {
            totals.merge(itemName, quantity, Long::sum);
            return;
        }

        long craftsNeeded = (long) Math.ceil((double) quantity / chosen.outputCount);

        for (Map.Entry<String, Integer> ingredient : chosen.ingredients.entrySet()) {
            long totalNeeded = (long) ingredient.getValue() * craftsNeeded;
            resolveRawMaterials(ingredient.getKey(), totalNeeded, totals);
        }
    }

    private static Recipe selectRecipe(String itemName, List<Recipe> options) {
        String preferredId = Configs.recipePreferences.get(itemName);
        if (preferredId != null) {
            for (Recipe recipe : options) {
                if (recipe.id.equals(preferredId)) {
                    return recipe;
                }
            }
        }
        return options.get(0);
    }
}