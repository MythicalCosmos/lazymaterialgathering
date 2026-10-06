package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.InventoryUtils;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Comparator;

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

    /*
     * Wood preference order.
     *
     * The quantity in the schematic determines the winner.
     * This list is ONLY the tie-breaker when two or more
     * wood types have the same total weight.
     *
     * Earlier entries have higher priority.
     */
    private static final List<String> WOOD_TYPES = List.of(
            "spruce",
            "oak",
            "dark_oak",
            "bamboo",
            "cherry",
            "jungle",
            "birch",
            "acacia",
            "mangrove",
            "crimson",
            "warped"
    );

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

        Map<String, Long> totals = new TreeMap<>();
        Map<String, Map<String, Long>> usedIn = new TreeMap<>();
        Map<String, Long> available = new HashMap<>();

        if (items == null || items.isEmpty()) {
            return new Result(totals, usedIn);
        }

        /*
         * Determine which wood type is most common in the original
         * schematic requirements.
         *
         * This is calculated once and then used throughout the recipe
         * resolution process so the result remains consistent.
         */
        String preferredWoodType = determinePreferredWoodType(items);

        for (Map.Entry<String, Long> entry : items.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            long quantity = Math.max(0L, entry.getValue());

            if (quantity <= 0L) {
                continue;
            }

            resolveRawMaterials(
                    entry.getKey(),
                    quantity,
                    totals,
                    choicesOut,
                    usedIn,
                    available,
                    new HashSet<>(),
                    preferredWoodType
            );
        }

        return new Result(totals, usedIn);
    }

    private static void resolveRawMaterials(
            String itemName,
            long quantity,
            Map<String, Long> totals,
            @Nullable Set<String> choicesOut,
            Map<String, Map<String, Long>> usedIn,
            Map<String, Long> available,
            Set<String> beingCrafted,
            String preferredWoodType
    ) {
        if (itemName == null
                || itemName.isBlank()
                || quantity <= 0L) {
            return;
        }

        /*
         * Use items already in the player's inventory before gathering
         * additional materials.
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
         * IMPORTANT:
         *
         * Recipes are checked BEFORE the direct-mining preference.
         *
         * This prevents directly-mineable crafted items such as:
         *
         * oak_fence
         * chest
         * glass
         * hopper
         * trapdoor
         *
         * from incorrectly becoming raw materials when a recipe exists.
         */
        List<Recipe> options = getRecipesFor(itemName);

        if (options != null && !options.isEmpty()
                && !beingCrafted.contains(itemName)) {

            List<Recipe> enabled =
                    RecipeHeuristics.getEnabledOptions(
                            itemName,
                            options
                    );

            if (choicesOut != null && enabled.size() > 1) {
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
                            Math.max(1L, chosen.outputCount);

                    long craftsNeeded =
                            ceilDivide(
                                    remaining,
                                    outputCount
                            );

                    beingCrafted.add(itemName);

                    for (List<String> choices
                            : ingredientChoices) {

                        String ingredient =
                                chooseIngredientAlternative(
                                        choices,
                                        available,
                                        preferredWoodType
                                );

                        if (ingredient == null
                                || ingredient.isBlank()) {

                            totals.merge(
                                    itemName,
                                    craftsNeeded,
                                    Long::sum
                            );

                            continue;
                        }

                        long ingredientQuantity =
                                craftsNeeded;

                        usedIn
                                .computeIfAbsent(
                                        normalizeItemName(ingredient),
                                        ignored -> new TreeMap<>()
                                )
                                .merge(
                                        normalizeItemName(itemName),
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
                                beingCrafted,
                                preferredWoodType
                        );
                    }

                    beingCrafted.remove(itemName);
                    return;
                }
            }
        }

        /*
         * If no usable recipe exists, THEN respect the player's
         * "prefer mining over crafting" setting.
         */
        String normalizedItem = normalizeItemName(itemName);

        if (Configs.Generic.PREFER_MINING_OVER_CRAFTING.getBooleanValue() && MiningSourceResolver.hasMiningSource(normalizedItem)) {

            totals.merge(
                    normalizedItem,
                    remaining,
                    Long::sum
            );

            return;
        }

        /*
         * Nothing else can resolve this item, so treat it as a raw
         * material.
         */
        totals.merge(
                normalizedItem,
                remaining,
                Long::sum
        );
    }

    /**
     * Finds recipes while handling both:
     *
     * minecraft:oak_fence
     *
     * and:
     *
     * oak_fence
     *
     * recipe IDs.
     */
    private static List<Recipe> getRecipesFor(
            String itemName
    ) {
        if (itemName == null || itemName.isBlank()) {
            return null;
        }

        String normalized =
                normalizeItemName(itemName);

        List<Recipe> recipes =
                Recipes.recipes.get(itemName);

        if (recipes != null && !recipes.isEmpty()) {
            return recipes;
        }

        recipes =
                Recipes.recipes.get(normalized);

        if (recipes != null && !recipes.isEmpty()) {
            return recipes;
        }

        String namespaced =
                "minecraft:" + normalized;

        recipes =
                Recipes.recipes.get(namespaced);

        if (recipes != null && !recipes.isEmpty()) {
            return recipes;
        }

        return null;
    }

    /**
     * Chooses the best ingredient from a recipe's alternatives.
     *
     * Wood alternatives receive special handling:
     *
     * 1. Preferred schematic wood type.
     * 2. Item already in inventory.
     * 3. Directly mineable item.
     * 4. Craftable item.
     * 5. Alphabetical fallback.
     */
    private static String chooseIngredientAlternative(
            List<String> choices,
            Map<String, Long> available,
            @Nullable String preferredWoodType
    ) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }

        /*
         * If this recipe contains multiple wood alternatives,
         * always select the wood type that won the schematic-wide
         * weighting calculation.
         */
        if (preferredWoodType != null) {
            for (String candidate : choices) {
                if (candidate == null || candidate.isBlank()) {
                    continue;
                }

                String normalized =
                        normalizeItemName(candidate);

                String candidateWoodType =
                        getWoodType(normalized);

                if (preferredWoodType.equals(candidateWoodType)) {
                    return candidate;
                }
            }
        }

        String best = null;
        int bestScore = Integer.MAX_VALUE;

        for (String candidate : choices) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }

            int score =
                    ingredientScore(
                            candidate,
                            available,
                            preferredWoodType
                    );

            if (best == null
                    || score < bestScore
                    || (score == bestScore
                    && candidate.compareTo(best) < 0)) {

                best = candidate;
                bestScore = score;
            }
        }

        return best;
    }
    /**
     * Gives wood alternatives a slightly better score when they match
     * the schematic's dominant wood type.
     */
    private static int ingredientScore(
            String itemName,
            Map<String, Long> available,
            @Nullable String preferredWoodType
    ) {
        String normalizedItem =
                normalizeItemName(itemName);

        if (preferredWoodType != null
                && isWoodItem(normalizedItem)
                && preferredWoodType.equals(
                getWoodType(normalizedItem)
        )) {
            return -1;
        }

        if (available.getOrDefault(itemName, 0L) > 0L) {
            return 0;
        }

        if (available.getOrDefault(normalizedItem, 0L) > 0L) {
            return 0;
        }

        if (MiningSourceResolver.hasMiningSource(normalizedItem)) {
            return 1;
        }

        if (getRecipesFor(normalizedItem) != null) {
            return 2;
        }

        return 3;
    }

    /**
     * Determines the wood species most heavily represented in the
     * original schematic requirements.
     *
     * We count the actual quantities, not merely the number of item
     * types.
     *
     * Example:
     *
     * oak_fence       x 440
     * oak_trapdoor     x 52
     * spruce_gate      x 24
     *
     * Result:
     *
     * oak
     */
    @Nullable
    private static String determinePreferredWoodType(
            Map<String, Long> items
    ) {
        Map<String, Long> woodWeights = new HashMap<>();

        for (Map.Entry<String, Long> entry : items.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            long quantity = Math.max(0L, entry.getValue());

            if (quantity <= 0L) {
                continue;
            }

            String item = normalizeItemName(entry.getKey());

            String woodType = getWoodType(item);

            if (woodType == null) {
                continue;
            }

            woodWeights.merge(
                    woodType,
                    quantity,
                    Long::sum
            );
        }

        if (woodWeights.isEmpty()) {
            return null;
        }

        String bestWoodType = null;
        long bestWeight = Long.MIN_VALUE;
        int bestPriority = Integer.MAX_VALUE;

        for (int priority = 0; priority < WOOD_TYPES.size(); priority++) {
            String woodType = WOOD_TYPES.get(priority);

            long weight =
                    woodWeights.getOrDefault(
                            woodType,
                            0L
                    );

            if (weight > bestWeight) {
                bestWoodType = woodType;
                bestWeight = weight;
                bestPriority = priority;
            } else if (weight == bestWeight
                    && weight > 0L
                    && priority < bestPriority) {

                bestWoodType = woodType;
                bestPriority = priority;
            }
        }

        System.out.println(
                "[LMG] Preferred wood type: "
                        + bestWoodType
                        + " (weight="
                        + bestWeight
                        + ")"
        );

        System.out.println("[LMG] Wood weights:");

        for (String woodType : WOOD_TYPES) {
            long weight =
                    woodWeights.getOrDefault(
                            woodType,
                            0L
                    );

            if (weight > 0L) {
                System.out.println(
                        "  "
                                + woodType
                                + " = "
                                + weight
                );
            }
        }

        return bestWoodType;
    }

    /**
     * Determines whether an item belongs to a recognized wood family.
     */
    private static boolean isWoodItem(
            String itemName
    ) {
        return getWoodType(itemName) != null;
    }

    /**
     * Gets the wood type from an item name.
     *
     * Examples:
     *
     * oak_planks      -> oak
     * oak_fence       -> oak
     * spruce_trapdoor -> spruce
     * dark_oak_sign   -> dark_oak
     * warped_fence    -> warped
     */
    @Nullable
    private static String getWoodType(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return null;
        }

        String normalized =
                normalizeItemName(itemName);

        for (String woodType : WOOD_TYPES) {
            if (normalized.equals(woodType)
                    || normalized.startsWith(woodType + "_")) {
                return woodType;
            }
        }

        return null;
    }

    /**
     * Removes the minecraft namespace so recipe and item lookups
     * remain consistent.
     */
    private static String normalizeItemName(
            String itemName
    ) {
        if (itemName == null) {
            return "";
        }

        String normalized =
                itemName.trim();

        if (normalized.startsWith(
                "minecraft:"
        )) {
            normalized =
                    normalized.substring(
                            "minecraft:".length()
                    );
        }

        return normalized;
    }

    private static long consumeInventory(
            Map<String, Long> available,
            String itemName,
            long requested
    ) {
        String normalized =
                normalizeItemName(itemName);

        if (!available.containsKey(itemName)
                && !available.containsKey(normalized)) {

            long inventory =
                    InventoryUtils.count(
                            normalized
                    );

            if (inventory > 0L) {
                available.put(
                        normalized,
                        inventory
                );
            } else {
                available.put(
                        normalized,
                        0L
                );
            }
        }

        String lookup =
                available.containsKey(itemName)
                        ? itemName
                        : normalized;

        long have =
                available.getOrDefault(
                        lookup,
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
            available.remove(lookup);
        } else {
            available.put(
                    lookup,
                    left
            );
        }

        return requested - consumed;
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