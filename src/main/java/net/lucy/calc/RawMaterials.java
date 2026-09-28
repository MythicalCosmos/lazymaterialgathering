package net.lucy.calc;

import net.lucy.config.Configs;
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
        boolean preferDirectMining = Configs.Generic.PREFER_MINING_OVER_CRAFTING.getBooleanValue() && MiningData.isDirectlyMineable(itemName, Configs.Generic.USE_SILK_TOUCH.getBooleanValue(), Configs.Generic.HAS_SHEARS.getBooleanValue());
        if (options == null || options.isEmpty() || beingCrafted.contains(itemName) || preferDirectMining) {
            totals.merge(itemName, quantity, Long::sum);
            return;
        }

        List<Recipe> enabled = getEnabledOptions(itemName, options);
        if (choicesOut != null && enabled.size() > 1) {
            choicesOut.add(itemName);
        }

        Recipe chosen = selectRecipe(itemName, options);
        System.out.println("RAW MATERIALS: " + itemName
                + " x " + quantity
                + " -> recipe " + chosen.id);

        for (List<String> choices : chosen.getEffectiveIngredientChoices()) {
            System.out.println("  INGREDIENT OPTIONS: " + choices);
        }
        if (chosen.type == RecipeType.NATURAL) {
            totals.merge(itemName, quantity, Long::sum);
            return;
        }

        long outputCount = Math.max(1, chosen.outputCount);
        long craftsNeeded = (quantity + outputCount - 1L) / outputCount;
        beingCrafted.add(itemName);
        /*
         * RawMaterials deliberately chooses the first alternative.
         *
         * RecipeFinder is responsible for enumerating every alternative.
         */
        for (List<String> choices : chosen.getEffectiveIngredientChoices()) {
            if (choices == null || choices.isEmpty()) {
                continue;
            }

            String ingredientName = choices.get(0);
            long totalNeeded = craftsNeeded;
            usedIn.computeIfAbsent(ingredientName, ignored -> new TreeMap<>()).merge(itemName, totalNeeded, Long::sum);
            resolveRawMaterials(ingredientName, totalNeeded, totals, choicesOut, usedIn, beingCrafted);
        }
        beingCrafted.remove(itemName);
    }

    public static List<Recipe> getEnabledOptions(String itemName, List<Recipe> options) {
        List<Recipe> enabled = new ArrayList<>();
        for (Recipe recipe : options) {
            if (Configs.isRecipeEnabled(itemName, recipe.id)) {
                enabled.add(recipe);
            }
        }

        /*
         * Never make an item impossible to calculate because the user
         * disabled every recipe.
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