package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.InventoryUtils;
import net.lucy.data.MiningData;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Converts requested items into the actual base resources
 * required to obtain them.
 *
 * Example:
 *
 *     oak_fence
 *         -> oak_planks + sticks
 *         -> oak_logs
 *
 * Inventory is consumed globally during resolution.
 */
public final class RawMaterials {

    public static class Result {

        public final Map<String, Long> totals;

        public final Map<String, Map<String, Long>> usedIn;

        public Result(
                Map<String, Long> totals,
                Map<String, Map<String, Long>> usedIn
        ) {
            this.totals = totals;
            this.usedIn = usedIn;
        }
    }

    private RawMaterials() {
    }

    public static Map<String, Long> calculate(
            Map<String, Long> items
    ) {
        return calculate(items, null);
    }

    public static Map<String, Long> calculate(
            Map<String, Long> items,
            @Nullable Set<String> choicesOut
    ) {
        return calculateDetailed(items, choicesOut).totals;
    }

    public static Result calculateDetailed(
            Map<String, Long> items
    ) {
        return calculateDetailed(items, null);
    }

    public static Result calculateDetailed(
            Map<String, Long> items,
            @Nullable Set<String> choicesOut
    ) {
        Recipes.ensureLoaded();

        Map<String, Long> totals =
                new TreeMap<>();

        Map<String, Map<String, Long>> usedIn =
                new TreeMap<>();

        /*
         * Inventory available to the entire calculation.
         *
         * Inventory is consumed globally so the same items
         * cannot accidentally be used by multiple branches.
         */
        Map<String, Long> available =
                new HashMap<>();

        if (items == null || items.isEmpty()) {
            return new Result(
                    totals,
                    usedIn
            );
        }

        for (Map.Entry<String, Long> entry : items.entrySet()) {

            String itemName = entry.getKey();

            if (itemName == null || itemName.isBlank()) {
                continue;
            }

            long quantity =
                    Math.max(
                            0L,
                            entry.getValue()
                    );

            if (quantity <= 0L) {
                continue;
            }

            resolveRawMaterials(
                    itemName,
                    quantity,
                    totals,
                    choicesOut,
                    usedIn,
                    available,
                    new HashSet<>()
            );
        }

        return new Result(
                totals,
                usedIn
        );
    }

    private static void resolveRawMaterials(
            String itemName,
            long quantity,
            Map<String, Long> totals,
            @Nullable Set<String> choicesOut,
            Map<String, Map<String, Long>> usedIn,
            Map<String, Long> available,
            Set<String> beingCrafted
    ) {

        if (itemName == null
                || itemName.isBlank()
                || quantity <= 0L) {
            return;
        }

        /*
         * Consume existing inventory first.
         */
        long remaining =
                consumeInventory(
                        available,
                        itemName,
                        quantity
                );

        if (remaining <= 0L) {
            return;
        }

        /*
         * Find recipes using both:
         *
         *     minecraft:oak_fence
         *
         * and:
         *
         *     oak_fence
         */
        List<Recipe> options =
                getRecipesFor(itemName);

        /*
         * If this item has a usable recipe, resolve the recipe.
         *
         * This is intentionally done before the direct-mining
         * fallback. Otherwise crafted blocks such as fences,
         * buttons, beds, hoppers, etc. become terminal materials
         * before their recipes are ever considered.
         */
        if (options != null
                && !options.isEmpty()
                && !beingCrafted.contains(itemName)) {

            List<Recipe> enabled =
                    RecipeHeuristics.getEnabledOptions(
                            itemName,
                            options
                    );

            if (choicesOut != null
                    && enabled.size() > 1) {

                choicesOut.add(itemName);
            }

            Recipe chosen =
                    RecipeHeuristics.selectBestRecipe(
                            itemName,
                            enabled
                    );

            if (chosen != null
                    && chosen.type != RecipeType.NATURAL) {

                List<List<String>> ingredientChoices =
                        chosen.getEffectiveIngredientChoices();

                if (!ingredientChoices.isEmpty()) {

                    long outputCount =
                            Math.max(
                                    1L,
                                    chosen.outputCount
                            );

                    long craftsNeeded =
                            ceilDivide(
                                    remaining,
                                    outputCount
                            );

                    beingCrafted.add(itemName);

                    for (List<String> choices :
                            ingredientChoices) {

                        String ingredient =
                                chooseIngredientAlternative(
                                        choices,
                                        available
                                );

                        /*
                         * Malformed recipe.
                         *
                         * Never silently lose the requested item.
                         */
                        if (ingredient == null
                                || ingredient.isBlank()) {

                            totals.merge(
                                    itemName,
                                    craftsNeeded,
                                    Long::sum
                            );

                            continue;
                        }

                        /*
                         * Each ingredient slot is required once
                         * per craft.
                         */
                        long ingredientQuantity =
                                craftsNeeded;

                        usedIn
                                .computeIfAbsent(
                                        ingredient,
                                        ignored ->
                                                new TreeMap<>()
                                )
                                .merge(
                                        itemName,
                                        ingredientQuantity,
                                        Long::sum
                                );

                        resolveRawMaterials(
                                ingredient,
                                ingredientQuantity,
                                totals,
                                choicesOut,
                                usedIn,
                                available,
                                beingCrafted
                        );
                    }

                    beingCrafted.remove(itemName);

                    return;
                }
            }
        }

        /*
         * No usable recipe.
         *
         * This is now the terminal/base-material path.
         *
         * If Prefer Mining Over Crafting is enabled, this is
         * naturally the place where directly obtainable materials
         * terminate.
         */
        totals.merge(
                itemName,
                remaining,
                Long::sum
        );
    }

    /**
     * Gets recipes for an item while accepting both namespaced
     * and non-namespaced recipe keys.
     *
     * Examples:
     *
     *     minecraft:oak_fence
     *     oak_fence
     */
    private static List<Recipe> getRecipesFor(
            String itemName
    ) {

        if (itemName == null || itemName.isBlank()) {
            return null;
        }

        /*
         * Try the exact ID first.
         */
        List<Recipe> recipes =
                Recipes.recipes.get(itemName);

        if (recipes != null && !recipes.isEmpty()) {
            return recipes;
        }

        /*
         * Try without minecraft:.
         */
        if (itemName.startsWith("minecraft:")) {

            String stripped =
                    itemName.substring(
                            "minecraft:".length()
                    );

            recipes =
                    Recipes.recipes.get(stripped);

            if (recipes != null
                    && !recipes.isEmpty()) {

                return recipes;
            }
        }

        /*
         * Try adding minecraft: in case the incoming
         * item was stored without the namespace.
         */
        if (!itemName.contains(":")) {

            recipes =
                    Recipes.recipes.get(
                            "minecraft:" + itemName
                    );

            if (recipes != null
                    && !recipes.isEmpty()) {

                return recipes;
            }
        }

        return null;
    }

    private static long consumeInventory(
            Map<String, Long> available,
            String itemName,
            long requested
    ) {

        /*
         * Populate inventory lazily.
         */
        if (!available.containsKey(itemName)) {

            long inventory =
                    InventoryUtils.count(itemName);

            available.put(
                    itemName,
                    Math.max(
                            0L,
                            inventory
                    )
            );
        }

        long have =
                available.getOrDefault(
                        itemName,
                        0L
                );

        long consumed =
                Math.min(
                        have,
                        requested
                );

        long left =
                have - consumed;

        if (left <= 0L) {
            available.remove(itemName);
        } else {
            available.put(
                    itemName,
                    left
            );
        }

        return requested - consumed;
    }

    /**
     * Picks the best concrete item from an ingredient tag.
     *
     * Priority:
     *
     * 1. Existing inventory
     * 2. Directly obtainable item
     * 3. Item with another recipe
     * 4. Unknown item
     */
    private static String chooseIngredientAlternative(
            List<String> choices,
            Map<String, Long> available
    ) {

        if (choices == null
                || choices.isEmpty()) {
            return null;
        }

        String best = null;
        int bestScore = Integer.MAX_VALUE;

        for (String candidate : choices) {

            if (candidate == null
                    || candidate.isBlank()) {
                continue;
            }

            int score =
                    ingredientScore(
                            candidate,
                            available
                    );

            if (best == null
                    || score < bestScore
                    || (
                    score == bestScore
                            && candidate.compareTo(best) < 0
            )) {

                best = candidate;
                bestScore = score;
            }
        }

        return best;
    }

    private static int ingredientScore(
            String itemName,
            Map<String, Long> available
    ) {

        /*
         * Existing inventory wins.
         */
        if (available.getOrDefault(
                itemName,
                0L
        ) > 0L) {

            return 0;
        }

        /*
         * Directly obtainable resources are preferred
         * when choosing between alternatives.
         */
        if (MiningData.isDirectlyMineable(
                itemName,
                Configs.Generic
                        .USE_SILK_TOUCH
                        .getBooleanValue(),
                Configs.Generic
                        .HAS_SHEARS
                        .getBooleanValue()
        )) {

            return 1;
        }

        /*
         * Prefer an ingredient that has another recipe.
         */
        if (getRecipesFor(itemName) != null) {
            return 2;
        }

        /*
         * Unknown candidate.
         */
        return 3;
    }

    private static long ceilDivide(
            long value,
            long divisor
    ) {

        if (value <= 0L) {
            return 0L;
        }

        if (divisor <= 0L) {
            return value;
        }

        return value / divisor
                + (
                value % divisor == 0L
                        ? 0L
                        : 1L
        );
    }

    public static List<Recipe> getEnabledOptions(
            String itemName,
            List<Recipe> options
    ) {

        return RecipeHeuristics.getEnabledOptions(
                itemName,
                options
        );
    }

    public static Recipe selectRecipe(
            String itemName,
            List<Recipe> options
    ) {

        List<Recipe> enabled =
                getEnabledOptions(
                        itemName,
                        options
                );

        if (enabled.isEmpty()) {
            return null;
        }

        return RecipeHeuristics.selectBestRecipe(
                itemName,
                enabled
        );
    }
}