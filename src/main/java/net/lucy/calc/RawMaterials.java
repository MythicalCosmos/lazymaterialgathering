package net.lucy.calc;

import net.lucy.config.Configs;
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

    /** Everything calculate() can work out in one pass, so it's only done once per calculation. */
    public static class Result {
        public final Map<String, Long> totals;

        // raw material name -> (item that used it -> how much of it that item's recipe needed).
        // This is what the Raw Materials screen shows when you hover or click a row.
        public final Map<String, Map<String, Long>> usedIn;

        public Result(Map<String, Long> totals, Map<String, Map<String, Long>> usedIn) {
            this.totals = totals;
            this.usedIn = usedIn;
        }
    }

    public static Map<String, Long> calculate(Map<String, Long> items) {
        return calculate(items, null);
    }

    /**
     * Breaks the items down into raw materials.
     * If choicesOut is given, every item that has more than one ENABLED recipe is added to it
     * (used by the Preferred Recipes screen to know which items are worth asking about).
     */
    public static Map<String, Long> calculate(Map<String, Long> items, @Nullable Set<String> choicesOut) {
        return calculateDetailed(items, choicesOut).totals;
    }

    /** Same as calculate(), but also returns which items each raw material was used by. */
    public static Result calculateDetailed(Map<String, Long> items) {
        return calculateDetailed(items, null);
    }

    public static Result calculateDetailed(Map<String, Long> items, @Nullable Set<String> choicesOut) {
        Map<String, Long> totals = new TreeMap<>();
        Map<String, Map<String, Long>> usedIn = new TreeMap<>();

        for (Map.Entry<String, Long> entry : items.entrySet()) {
            resolveRawMaterials(entry.getKey(), entry.getValue(), totals, choicesOut, usedIn, new HashSet<>());
        }

        return new Result(totals, usedIn);
    }

    // "beingCrafted" holds the items we are in the middle of breaking down, to stop endless loops
    // (for example iron ingot -> iron block -> iron ingot).
    private static void resolveRawMaterials(String itemName, long quantity, Map<String, Long> totals,
                                            @Nullable Set<String> choicesOut,
                                            Map<String, Map<String, Long>> usedIn,
                                            Set<String> beingCrafted) {
        List<Recipe> options = Recipes.recipes.get(itemName);

        if (options == null || options.isEmpty() || beingCrafted.contains(itemName)) {
            totals.merge(itemName, quantity, Long::sum);
            return;
        }

        if (choicesOut != null && getEnabledOptions(itemName, options).size() > 1) {
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

            // Record that itemName's recipe directly needs this much of the ingredient,
            // regardless of whether the ingredient turns out to be a raw material itself
            // or gets broken down further.
            usedIn.computeIfAbsent(ingredient.getKey(), key -> new TreeMap<>())
                    .merge(itemName, totalNeeded, Long::sum);

            resolveRawMaterials(ingredient.getKey(), totalNeeded, totals, choicesOut, usedIn, beingCrafted);
        }
        beingCrafted.remove(itemName);
    }

    // Only the recipes the player hasn't turned off. Falls back to every recipe if that
    // would otherwise leave nothing to choose from (shouldn't normally happen).
    public static List<Recipe> getEnabledOptions(String itemName, List<Recipe> options) {
        List<Recipe> enabled = new ArrayList<>();

        for (Recipe recipe : options) {
            if (Configs.isRecipeEnabled(itemName, recipe.id)) {
                enabled.add(recipe);
            }
        }

        return enabled.isEmpty() ? options : enabled;
    }

    // The recipe used for this item: the preferred one, if it's enabled, otherwise the
    // first enabled recipe.
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