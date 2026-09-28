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
    /**
     * Returns the recipe options that are currently enabled for an item.
     *
     * Recipe preferences are handled separately by selectBestRecipe().
     */
    public static List<Recipe> getEnabledOptions(String itemName) {
        List<Recipe> options = Recipes.recipes.get(itemName);
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }
        return getEnabledOptions(itemName, options);
    }

    /**
     * Filters a supplied recipe list using the user's recipe settings.
     *
     * If every recipe has been disabled, the original options are returned
     * so that an item does not become impossible to resolve.
     */
    public static List<Recipe> getEnabledOptions(String itemName, List<Recipe> options) {
        List<Recipe> enabled = new ArrayList<>();
        for (Recipe recipe : options) {
            if (Configs.isRecipeEnabled(itemName, recipe.id)) {
                enabled.add(recipe);
            }
        }

        if (enabled.isEmpty()) {
            return new ArrayList<>(options);
        }
        return enabled;
    }

    /**
     * Selects the recipe that should normally be used by the raw-material
     * calculator.
     *
     * Selection order:
     *
     * 1. Explicit user recipe preference
     * 2. Heuristic ranking
     * 3. Stable recipe ID ordering
     */
    public static Recipe selectBestRecipe(String itemName, List<Recipe> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }

        List<Recipe> enabled = getEnabledOptions(itemName, options);
        /*
         * Respect an explicit recipe preference first.
         */
        String preferredId = Configs.recipePreferences.get(itemName);
        if (preferredId != null) {
            for (Recipe recipe : enabled) {
                if (recipe.id.equals(preferredId)) {
                    return recipe;
                }
            }
        }

        /*
         * Otherwise select the recipe with the lowest heuristic rank.
         */
        Recipe best = null;
        for (Recipe recipe : enabled) {if (best == null) {
                best = recipe;
                continue;
            }

            int recipeRank = recipeRank(recipe);
            int bestRank = recipeRank(best);
            if (recipeRank < bestRank) {
                best = recipe;
                continue;
            }

            /*
             * If two recipes have the same rank, prefer the one that
             * produces more items per operation.
             */
            if (recipeRank == bestRank && recipe.outputCount > best.outputCount) {
                best = recipe;
                continue;
            }

            /*
             * Finally, use the recipe ID as a deterministic tie-breaker.
             */
            if (recipeRank == bestRank && recipe.outputCount == best.outputCount && recipe.id.compareTo(best.id) < 0) {
                best = recipe;
            }
        }
        return best;
    }

    /**
     * Selects the best recipe for an item using the recipes currently
     * registered in Recipes.
     */
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

            String preferredId = Configs.recipePreferences.get(itemName);
            if (preferredId == null || !preferredId.equals(selected.id)) {
                Configs.recipePreferences.put(itemName, selected.id);
                changed++;
            }
        }

        return changed;
    }

    /**
     * Determines the heuristic priority of a recipe.
     *
     * Lower values are preferred.
     *
     * The ordering is intentionally broad:
     *
     * 0 = normal crafting
     * 1 = normal furnace-like processing
     * 2 = other processing methods
     * 3 = natural/unknown sources
     *
     * This does NOT determine every possible recipe path. RecipeFinder
     * remains responsible for exhaustive path enumeration.
     */
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

    /**
     * Returns the heuristic rank of a recipe.
     *
     * This public helper is useful to GUI code or debugging code that wants
     * to display why one recipe was selected.
     */
    public static int getRecipeRank(Recipe recipe) {
        return recipeRank(recipe);
    }

    /**
     * Returns all recipes for an item sorted according to the same
     * heuristics used by selectBestRecipe().
     *
     * The returned list is a new list and does not modify Recipes.recipes.
     */
    public static List<Recipe> sortOptions(String itemName) {
        List<Recipe> options = Recipes.recipes.get(itemName);

        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }

        List<Recipe> sorted = new ArrayList<>(options);

        sorted.sort(
                (a, b) -> {
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
                }
                );
        return sorted;
    }

    /**
     * Determines whether an item has multiple recipe options available.
     */
    public static boolean hasMultipleOptions(String itemName) {
        List<Recipe> options = getEnabledOptions(itemName);
        return options.size() > 1;
    }

    /**
     * Returns the number of enabled recipe options for an item.
     */
    public static int getOptionCount(String itemName) {
        return getEnabledOptions(itemName).size();
    }

    /**
     * Returns the recipe's effective ingredient choices.
     *
     * This helper exists so older cached recipes that only contain the
     * legacy ingredients map continue to work.
     */
    public static List<List<String>>
    getIngredientChoices(Recipe recipe) {
        if (recipe == null) {
            return List.of();
        }
        return recipe.getEffectiveIngredientChoices();
    }

    /**
     * Calculates the number of individual ingredient slots required for one
     * execution of a recipe.
     */
    public static int getIngredientCount(Recipe recipe) {
        if (recipe == null) {
            return 0;
        }

        return recipe.getEffectiveIngredientChoices().size();
    }

    /**
     * Calculates how many operations are required to produce the requested
     * quantity.
     */
    public static long getCraftCount(Recipe recipe, long requestedQuantity) {
        if (recipe == null || requestedQuantity <= 0) {
            return 0;
        }

        long outputCount = Math.max(1, recipe.outputCount);
        return (requestedQuantity + outputCount - 1) / outputCount;
    }

    /**
     * Returns the number of copies of an ingredient slot needed for a
     * requested output quantity.
     */
    public static long getIngredientQuantity(Recipe recipe, long requestedQuantity) {
        long crafts = getCraftCount(recipe, requestedQuantity);
        return crafts * getIngredientCount(recipe);
    }
}