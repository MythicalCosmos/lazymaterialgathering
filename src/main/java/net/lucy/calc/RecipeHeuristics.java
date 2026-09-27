package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.DataManager;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A developer convenience for the Preferred Recipes screen: bulk-picks a recipe for every
 * item that has more than one enabled option, instead of doing it by hand one at a time.
 *
 * The game doesn't record which recipe players use most often, so there's no real
 * "popularity" data to draw on. This picks the recipe that needs the fewest total
 * ingredients, preferring Crafting over Smelting over anything else as a tie-breaker,
 * which is a reasonable stand-in for "the recipe most people would default to" — but it's
 * a heuristic, not a fact about player behaviour. Treat it as a fast starting point to
 * hand-adjust from, not a final answer.
 */
public class RecipeHeuristics {

    /** Applies the heuristic to every item. Returns how many preferences actually changed. */
    public static int applyToAll() {
        int changed = 0;

        for (Map.Entry<String, List<Recipe>> entry : Recipes.recipes.entrySet()) {
            String itemName = entry.getKey();
            List<Recipe> enabled = RawMaterials.getEnabledOptions(itemName, entry.getValue());

            if (enabled.size() <= 1) {
                continue;
            }

            Recipe best = enabled.stream()
                    .min(Comparator.<Recipe>comparingInt(RecipeHeuristics::totalIngredientCount)
                            .thenComparingInt(r -> typeRank(r.type))
                            .thenComparing(r -> r.id))
                    .orElse(enabled.get(0));

            String current = Configs.recipePreferences.get(itemName);
            if (best.id.equals(current) == false) {
                Configs.recipePreferences.put(itemName, best.id);
                changed++;
            }
        }

        if (changed > 0) {
            Configs.saveToFile();
            DataManager.recalculateRawMaterials();
        }

        return changed;
    }

    private static int totalIngredientCount(Recipe recipe) {
        return recipe.ingredients.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static int typeRank(RecipeType type) {
        if (type == RecipeType.CRAFTING) return 0;
        if (type == RecipeType.SMELTING) return 1;
        return 2;
    }
}