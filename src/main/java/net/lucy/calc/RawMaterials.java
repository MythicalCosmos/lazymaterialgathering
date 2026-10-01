package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.InventoryUtils;
import net.lucy.data.MiningData;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RawMaterials {
    public static class Result {
        public final Map<String, Long> totals;
        public final Map<String, Map<String, Long>> usedIn;
        public Result(Map<String, Long> totals, Map<String, Map<String, Long>> usedIn) {
            this.totals = totals;
            this.usedIn = usedIn;
        }
    }

    public static Map<String, Long> calculate(Map<String, Long> items) {
        return calculate(items, null);
    }

    public static Map<String, Long> calculate(Map<String, Long> items, @Nullable Set<String> choicesOut) {
        return calculateDetailed(items, choicesOut).totals;
    }

    public static Result calculateDetailed(Map<String, Long> items) {
        return calculateDetailed(items, null);
    }

    public static Result calculateDetailed(Map<String, Long> items, @Nullable Set<String> choicesOut) {
        Recipes.ensureLoaded();
        Map<String, Long> totals = new TreeMap<>();
        Map<String, Map<String, Long>> usedIn = new TreeMap<>();
        for (Map.Entry<String, Long> entry : items.entrySet()) {
            resolveRawMaterials(entry.getKey(), entry.getValue(), totals, choicesOut, usedIn, new HashSet<>());
        }
        return new Result(totals, usedIn);
    }

    private static void resolveRawMaterials(String itemName, long quantity, Map<String, Long> totals, @Nullable Set<String> choicesOut, Map<String, Map<String, Long>> usedIn, Set<String> beingCrafted) {
        List<Recipe> options = Recipes.recipes.get(itemName);
        /*
         * Directly mineable items can be left as raw materials
         * when the configuration prefers mining.
         */
        boolean preferDirectMining =
                Configs.Generic
                        .PREFER_MINING_OVER_CRAFTING
                        .getBooleanValue()
                        &&
                        MiningData.isDirectlyMineable(
                                itemName,
                                Configs.Generic
                                        .USE_SILK_TOUCH
                                        .getBooleanValue(),
                                Configs.Generic
                                        .HAS_SHEARS
                                        .getBooleanValue());
        /*
         * If there is no recipe, we've reached a base material.
         *
         * Also stop recursion if we encounter a circular recipe.
         */
        if (options == null || options.isEmpty() || beingCrafted.contains(itemName) || preferDirectMining) {
            totals.merge(itemName, quantity, Long::sum);
            return;
        }

        List<Recipe> enabled = getEnabledOptions(itemName, options);
        if (choicesOut != null && enabled.size() > 1) {
            choicesOut.add(itemName);
        }

        Recipe chosen = RecipeHeuristics.selectBestRecipe(itemName, enabled);
        if (chosen == null) {
            totals.merge(itemName, quantity, Long::sum);
            return;
        }

        System.out.println("RAW MATERIALS: " + itemName + " x " + quantity + " -> recipe " + chosen.id);
        for (List<String> choices : chosen.getEffectiveIngredientChoices()) {
            System.out.println("  INGREDIENT OPTIONS: " + choices);
        }
        /*
         * Natural recipes are already source materials.
         */
        if (chosen.type == RecipeType.NATURAL) {
            totals.merge(itemName, quantity, Long::sum);
            return;
        }

        long outputCount = Math.max(1L, chosen.outputCount);
        /*
         * Example:
         *
         * 1 log -> 4 planks
         *
         * Need 64 planks:
         *
         * ceil(64 / 4) = 16 crafts
         */
        long craftsNeeded = (quantity + outputCount - 1L) / outputCount;
        beingCrafted.add(itemName);

        for (List<String> choices : chosen.getEffectiveIngredientChoices()) {
            if (choices == null || choices.isEmpty()) {
                continue;
            }

            String ingredientName = chooseIngredientAlternative(choices);
            if (ingredientName == null) {
                continue;
            }
            /*
             * Each ingredient entry represents one
             * required ingredient per craft.
             *
             * Therefore:
             *
             * ingredient amount = craftsNeeded
             */
            long totalNeeded = craftsNeeded;
            usedIn.computeIfAbsent(ingredientName, ignored -> new TreeMap<>()).merge(itemName, totalNeeded, Long::sum);
            resolveRawMaterials(ingredientName, totalNeeded, totals, choicesOut, usedIn, beingCrafted);
        }
        beingCrafted.remove(itemName);
    }

    /**
     * Chooses one concrete item from an Ingredient
     * tag/alternative list.
     *
     * Old behavior:
     *
     *     choices.get(0)
     *
     * That meant a recipe such as:
     *
     *     any_planks
     *
     * always became the first item in the tag,
     * which is usually oak.
     *
     * New priority:
     *
     * 0 = already have it
     * 1 = directly mineable when mining is preferred
     * 2 = has a known recipe
     * 3 = mineable but not preferred
     * 4 = unknown
     *
     * Ties are resolved alphabetically so the result
     * is deterministic.
     */
    private static String chooseIngredientAlternative(List<String> choices) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }

        String best = choices.get(0);
        int bestScore = ingredientScore(best);
        for (int i = 1; i < choices.size(); i++) {
            String candidate = choices.get(i);
            int score = ingredientScore(candidate);
            if (score < bestScore || (score == bestScore && candidate.compareTo(best) < 0)) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static int ingredientScore(String itemName) {
        /*
         * Existing inventory is always the cheapest
         * option because no gathering is necessary.
         */
        long inventory = InventoryUtils.count(itemName);
        if (inventory > 0) {
            return 0;
        }

        boolean mineable = MiningData.isDirectlyMineable(itemName, Configs.Generic.USE_SILK_TOUCH.getBooleanValue(), Configs.Generic.HAS_SHEARS.getBooleanValue());
        if (mineable && Configs.Generic.PREFER_MINING_OVER_CRAFTING.getBooleanValue()) {
            return 1;
        }

        if (Recipes.recipes.containsKey(itemName)) {
            return 2;
        }
        return mineable ? 3 : 4;
    }

    public static List<Recipe> getEnabledOptions(String itemName, List<Recipe> options) {
        List<Recipe> enabled = new ArrayList<>();
        for (Recipe recipe : options) {
            if (Configs.isRecipeEnabled(itemName, recipe.id)) {
                enabled.add(recipe);
            }
        }
        /*
         * Never make an item impossible to calculate
         * merely because the user disabled every recipe.
         */
        return enabled.isEmpty() ? options : enabled;
    }

    public static Recipe selectRecipe(String itemName, List<Recipe> options) {
        List<Recipe> enabled = getEnabledOptions(itemName, options);
        String preferredId = Configs.recipePreferences.get(itemName);
        if (preferredId != null) {
            for (Recipe recipe : enabled) {
                if (recipe.id.equals(preferredId)) {
                    return recipe;
                }
            }
        }
        return enabled.get(0);
    }
}