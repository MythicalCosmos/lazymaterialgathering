package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RecipeHeuristics {
    private RecipeHeuristics() {
    }

    public static List<Recipe> getEnabledOptions(String itemName, List<Recipe> options) {
        List<Recipe> enabled = new ArrayList<>();
        if (options == null) {
            return enabled;
        }

        for (Recipe recipe : options) {
            if (recipe != null && Configs.isRecipeEnabled(itemName, recipe.id)) {
                enabled.add(recipe);
            }
        }

        /*
         * Never make an item impossible just because every recipe was
         * accidentally disabled.
         */
        if (enabled.isEmpty()) {
            return new ArrayList<>(options);
        }
        return enabled;
    }

    public static Recipe selectBestRecipe(String itemName, List<Recipe> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }

        List<Recipe> enabled = getEnabledOptions(itemName, options);
        /*
         * Explicit user preference always wins.
         */
        String preferredId = Configs.recipePreferences.get(itemName);
        if (preferredId != null) {
            for (Recipe recipe : enabled) {
                if (recipe != null && preferredId.equals(recipe.id)) {
                    return recipe;
                }
            }
        }

        Recipe best = null;
        for (Recipe recipe : enabled) {
            if (recipe == null) {
                continue;
            }

            if (best == null) {
                best = recipe;
                continue;
            }

            int recipeRank = recipeRank(recipe);
            int bestRank = recipeRank(best);
            if (recipeRank < bestRank) {
                best = recipe;
                continue;
            }

            if (recipeRank == bestRank && recipe.outputCount > best.outputCount) {
                best = recipe;
                continue;
            }

            if (recipeRank == bestRank && recipe.outputCount == best.outputCount && String.valueOf(recipe.id).compareTo(String.valueOf(best.id)) < 0) {
                best = recipe;
            }
        }
        return best;
    }

    public static int applyToAll() {
        int changed = 0;
        for (Map.Entry<String, List<Recipe>> entry : Recipes.recipes.entrySet()) {
            String itemName = entry.getKey();
            List<Recipe> options = entry.getValue();
            if (options == null || options.isEmpty()) {
                continue;
            }

            Recipe selected = selectBestRecipe(itemName, options);
            if (selected == null) {
                continue;
            }

            String previous = Configs.recipePreferences.get(itemName);
            if (!selected.id.equals(previous)) {
                Configs.recipePreferences.put(itemName, selected.id);
                changed++;
            }
        }
        return changed;
    }

    private static int recipeRank(Recipe recipe) {
        if (recipe == null || recipe.type == null) {
            return 3;
        }

        return switch (recipe.type) {
            case CRAFTING -> 0;
            case SMELTING, BLASTING, SMOKING, CAMPFIRE -> 1;
            case STONECUTTING, SMITHING, BREWING -> 2;
            case NATURAL, OTHER -> 3;
        };
    }
}