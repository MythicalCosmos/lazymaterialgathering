package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RawMaterials {

    public static Map<String, Long> calculate(Map<String, Long> items) {
        return calculate(items, null);
    }

    /**
     * Breaks the items down into raw materials.
     * If choicesOut is given, every item that had more than one recipe to choose from is added to it.
     */
    public static Map<String, Long> calculate(Map<String, Long> items, @Nullable Set<String> choicesOut) {
        Map<String, Long> rawMaterialTotals = new TreeMap<>();

        for (Map.Entry<String, Long> entry : items.entrySet()) {
            resolveRawMaterials(entry.getKey(), entry.getValue(), rawMaterialTotals, choicesOut, new HashSet<>());
        }
        return rawMaterialTotals;
    }

    // "beingCrafted" holds the items we are in the middle of breaking down, to stop endless loops
    // (for example iron ingot -> iron block -> iron ingot).
    private static void resolveRawMaterials(String itemName, long quantity, Map<String, Long> totals,
                                            @Nullable Set<String> choicesOut, Set<String> beingCrafted) {
        List<Recipe> options = Recipes.recipes.get(itemName);

        if (options == null || options.isEmpty() || beingCrafted.contains(itemName)) {
            totals.merge(itemName, quantity, Long::sum);
            return;
        }

        if (choicesOut != null && options.size() > 1) {
            choicesOut.add(itemName);
        }

        Recipe chosen = selectRecipe(itemName, options);

        if (chosen.type == RecipeType.NATURAL) {
            totals.merge(itemName, quantity, Long::sum);
            return;
        }

        long craftsNeeded = (long) Math.ceil((double) quantity / chosen.outputCount);

        beingCrafted.add(itemName);
        for (Map.Entry<String, Integer> ingredient : chosen.ingredients.entrySet()) {
            long totalNeeded = (long) ingredient.getValue() * craftsNeeded;
            resolveRawMaterials(ingredient.getKey(), totalNeeded, totals, choicesOut, beingCrafted);
        }
        beingCrafted.remove(itemName);
    }

    // The recipe the player picked for this item, or the first one if they haven't picked
    public static Recipe selectRecipe(String itemName, List<Recipe> options) {
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