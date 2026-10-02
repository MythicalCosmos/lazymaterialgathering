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

    public static List<Recipe> getEnabledOptions(String itemName) {
        List<Recipe> options = Recipes.recipes.get(itemName);
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }
        return getEnabledOptions(itemName, options);
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

    public static Recipe selectBestRecipe(String itemName) {
        List<Recipe> options = Recipes.recipes.get(itemName);
        if (options == null || options.isEmpty()) {
            return null;
        }
        return selectBestRecipe(itemName, options);
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

    public static int getRecipeRank(Recipe recipe) {
        return recipeRank(recipe);
    }

    public static List<Recipe> sortOptions(String itemName) {
        List<Recipe> options = Recipes.recipes.get(itemName);
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }

        List<Recipe> sorted = new ArrayList<>(options);
        sorted.sort((a, b) -> {
            int rankA = recipeRank(a);
            int rankB = recipeRank(b);
            if (rankA != rankB) {
                return Integer.compare(rankA, rankB);
            }

            int outputA = a == null ? 0 : a.outputCount;
            int outputB = b == null ? 0 : b.outputCount;
            if (outputA != outputB) {
                return Integer.compare(outputB, outputA);
            }

            String idA = a == null || a.id == null ? "" : a.id;
            String idB = b == null || b.id == null ? "" : b.id;
            return idA.compareTo(idB);
        });

        return sorted;
    }

    public static boolean hasMultipleOptions(String itemName) {
        return getEnabledOptions(itemName).size() > 1;
    }

    public static int getOptionCount(String itemName) {
        return getEnabledOptions(itemName).size();
    }

    public static List<List<String>> getIngredientChoices(Recipe recipe) {
        if (recipe == null) {
            return List.of();
        }
        return recipe.getEffectiveIngredientChoices();
    }

    public static int getIngredientCount(Recipe recipe) {
        if (recipe == null) {
            return 0;
        }
        return recipe.getEffectiveIngredientChoices().size();
    }

    public static long getCraftCount(Recipe recipe, long requestedQuantity) {
        if (recipe == null || requestedQuantity <= 0) {
            return 0;
        }

        long outputCount = Math.max(1L, recipe.outputCount);
        return (requestedQuantity + outputCount - 1L) / outputCount;
    }

    public static long getIngredientQuantity(Recipe recipe, long requestedQuantity) {
        long crafts = getCraftCount(recipe, requestedQuantity);
        return crafts * getIngredientCount(recipe);
    }
}