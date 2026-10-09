package net.lucy.calc;

import net.lucy.data.InventoryUtils;
import net.lucy.data.Recipes;
import net.lucy.model.CraftingPlan;
import net.lucy.model.CraftingTask;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CraftingPlanner {

    private CraftingPlanner() {
    }

    /**
     * Builds a complete crafting plan against the player's CURRENT inventory.
     *
     * Inventory is treated as a shared resource pool.
     *
     * Important:
     *
     * If two recipe ingredients both want the same item, the first dependency
     * consumes it from PlanningState.available(). The second dependency sees
     * the reduced amount.
     *
     * Ingredient alternatives are evaluated against cloned planning states so
     * the planner never commits an alternative until the complete dependency
     * branch has been evaluated.
     */
    public static CraftingPlan plan(
            Map<String, Long> targetItems
    ) {
        Recipes.ensureLoaded();

        if (targetItems == null
                || targetItems.isEmpty()) {

            return new CraftingPlan(
                    List.of(),
                    Map.of()
            );
        }

        PlanningState state =
                new PlanningState();

        for (Map.Entry<String, Long> entry
                : targetItems.entrySet()) {

            if (entry.getKey() == null) {
                continue;
            }

            long required =
                    Math.max(
                            0L,
                            entry.getValue()
                    );

            if (required <= 0L) {
                continue;
            }

            ensureAvailable(
                    entry.getKey(),
                    required,
                    state,
                    new HashSet<>()
            );
        }

        return new CraftingPlan(
                Collections.unmodifiableList(
                        new ArrayList<>(state.tasks)
                ),
                state.missing
        );
    }

    /**
     * Ensures that the requested quantity exists in the simulated inventory.
     *
     * State.available() represents inventory that has not yet been reserved
     * by an earlier dependency in this plan.
     */
    private static boolean ensureAvailable(
            String item,
            long required,
            PlanningState state,
            Set<String> visiting
    ) {
        String normalized =
                normalize(item);

        if (normalized == null
                || required <= 0L) {

            return true;
        }

        /*
         * First consume items that are already available in the shared
         * planning inventory.
         */
        long available =
                state.available(normalized);

        if (available >= required) {
            state.consume(
                    normalized,
                    required
            );

            return true;
        }

        long missing =
                required - available;

        /*
         * Reserve whatever inventory actually exists before attempting to
         * satisfy the remainder.
         */
        state.consume(
                normalized,
                available
        );

        List<Recipe> options =
                Recipes.recipes.get(normalized);

        /*
         * A directly mineable material is intentionally left as a gathering
         * requirement rather than being converted into a crafting dependency.
         */
        if (options == null
                || options.isEmpty()
                || MiningSourceResolver.hasMiningSource(normalized)) {

            state.missing.merge(
                    normalized,
                    missing,
                    Long::sum
            );

            return false;
        }

        /*
         * Prevent recursive recipe cycles.
         */
        if (!visiting.add(normalized)) {

            state.missing.merge(
                    normalized,
                    missing,
                    Long::sum
            );

            return false;
        }

        List<Recipe> enabled =
                RecipeHeuristics.getEnabledOptions(
                        normalized,
                        options
                );

        Recipe recipe =
                RecipeHeuristics.selectBestRecipe(
                        normalized,
                        enabled
                );

        if (recipe == null
                || recipe.type != RecipeType.CRAFTING) {

            visiting.remove(normalized);

            state.missing.merge(
                    normalized,
                    missing,
                    Long::sum
            );

            return false;
        }

        long output =
                Math.max(
                        1L,
                        recipe.outputCount
                );

        long crafts =
                ceilDivide(
                        missing,
                        output
                );

        List<List<String>> choices =
                recipe.getEffectiveIngredientChoices();

        if (choices == null
                || choices.isEmpty()) {

            visiting.remove(normalized);

            state.missing.merge(
                    normalized,
                    missing,
                    Long::sum
            );

            return false;
        }

        /*
         * Evaluate every ingredient alternative against the same complete
         * simulated state.
         *
         * We do not simply ask "which ingredient is currently in inventory?"
         * because that can make two sibling recipe slots reserve the same
         * inventory incorrectly.
         */
        PlanningState dependencyState =
                resolveIngredientChoices(
                        choices,
                        crafts,
                        state,
                        visiting
                );

        if (dependencyState == null) {

            /*
             * No complete dependency branch could satisfy the recipe.
             *
             * Record the requested output as missing rather than pretending
             * that the recipe is executable.
             */
            state.missing.merge(
                    normalized,
                    missing,
                    Long::sum
            );

            visiting.remove(normalized);

            return false;
        }

        /*
         * The complete dependency branch succeeded.
         *
         * Replace the current state with the branch that actually worked.
         */
        state.copyFrom(
                dependencyState
        );

        Map<String, Long> ingredientTotals =
                dependencyState.lastIngredientTotals;

        state.tasks.add(
                new CraftingTask(
                        normalized,
                        missing,
                        crafts,
                        output,
                        recipe.id,
                        ingredientTotals,
                        requiresCraftingTable(recipe)
                )
        );

        /*
         * The crafted output becomes available to later dependencies.
         *
         * It is also immediately consumed for the amount requested by the
         * current caller.
         */
        state.addAvailable(
                normalized,
                crafts * output
        );

        state.consume(
                normalized,
                required
        );

        visiting.remove(normalized);

        return true;
    }

    /**
     * Evaluates all ingredient slots as one transaction.
     *
     * Each alternative receives a cloned PlanningState. The first completely
     * executable branch is selected, with a score used to choose between
     * multiple executable branches.
     */
    private static PlanningState resolveIngredientChoices(
            List<List<String>> choices,
            long crafts,
            PlanningState base,
            Set<String> visiting
    ) {
        PlanningState best =
                null;

        PlanScore bestScore =
                null;

        for (int i = 0;
             i < choices.size();
             i++) {

            List<String> alternatives =
                    choices.get(i);

            if (alternatives == null
                    || alternatives.isEmpty()) {

                continue;
            }

            PlanningState candidateBase =
                    best == null
                            ? base.copy()
                            : base.copy();

            PlanningState candidate =
                    resolveIngredientChoiceRecursive(
                            choices,
                            i,
                            crafts,
                            candidateBase,
                            visiting
                    );

            if (candidate == null) {
                continue;
            }

            PlanScore score =
                    score(
                            candidate,
                            base
                    );

            if (best == null
                    || score.compareTo(bestScore) < 0) {

                best = candidate;
                bestScore = score;
            }

            /*
             * The recursive method already evaluates the complete list.
             * Only one complete branch is returned per starting alternative.
             */
            if (best != null) {
                break;
            }
        }

        return best;
    }

    /**
     * Recursive ingredient-choice resolver.
     *
     * At every recipe slot:
     *
     * 1. Clone the current state.
     * 2. Try each valid ingredient.
     * 3. Resolve all of its dependencies.
     * 4. Continue to the next ingredient only if that branch succeeds.
     *
     * This makes ingredient selection transactional.
     */
    private static PlanningState resolveIngredientChoiceRecursive(
            List<List<String>> choices,
            int index,
            long crafts,
            PlanningState state,
            Set<String> visiting
    ) {
        if (index >= choices.size()) {
            return state;
        }

        List<String> alternatives =
                choices.get(index);

        if (alternatives == null
                || alternatives.isEmpty()) {

            return null;
        }

        List<String> ordered =
                orderAlternatives(
                        alternatives,
                        state
                );

        for (String ingredient : ordered) {

            String normalized =
                    normalize(ingredient);

            if (normalized == null) {
                continue;
            }

            PlanningState candidate =
                    state.copy();

            /*
             * Each ingredient slot represents one ingredient per craft.
             */
            long required =
                    crafts;

            Map<String, Long> beforeMissing =
                    new LinkedHashMap<>(
                            candidate.missing
                    );

            boolean success =
                    ensureAvailable(
                            normalized,
                            required,
                            candidate,
                            new HashSet<>(visiting)
                    );

            if (!success) {
                continue;
            }

            /*
             * Record the selected ingredient in the task's ingredient map.
             */
            candidate.lastIngredientTotals.merge(
                    normalized,
                    required,
                    Long::sum
            );

            PlanningState result =
                    resolveIngredientChoiceRecursive(
                            choices,
                            index + 1,
                            crafts,
                            candidate,
                            visiting
                    );

            if (result != null) {
                return result;
            }

            /*
             * Explicitly retained for clarity: failed alternatives never
             * mutate the parent state because candidate is a clone.
             */
            candidate.missing.clear();
            candidate.missing.putAll(
                    beforeMissing
            );
        }

        return null;
    }

    /**
     * Orders alternatives without consuming any resources.
     *
     * Preference order:
     *
     * 1. Already reserved/available inventory
     * 2. Directly mineable material
     * 3. Material with a valid crafting recipe
     * 4. Original recipe order
     */
    private static List<String> orderAlternatives(
            List<String> alternatives,
            PlanningState state
    ) {
        List<String> ordered =
                new ArrayList<>();

        for (String alternative : alternatives) {
            if (alternative == null
                    || alternative.isBlank()) {
                continue;
            }

            String normalized =
                    normalize(alternative);

            if (normalized == null
                    || ordered.contains(normalized)) {
                continue;
            }

            ordered.add(normalized);
        }

        ordered.sort(
                (a, b) -> {

                    int availableA =
                            state.available(a) > 0L
                                    ? 0
                                    : 1;

                    int availableB =
                            state.available(b) > 0L
                                    ? 0
                                    : 1;

                    if (availableA != availableB) {
                        return Integer.compare(
                                availableA,
                                availableB
                        );
                    }

                    boolean mineableA =
                            MiningSourceResolver
                                    .hasMiningSource(a);

                    boolean mineableB =
                            MiningSourceResolver
                                    .hasMiningSource(b);

                    if (mineableA != mineableB) {
                        return Boolean.compare(
                                mineableB,
                                mineableA
                        );
                    }

                    boolean craftableA =
                            hasCraftingRecipe(a);

                    boolean craftableB =
                            hasCraftingRecipe(b);

                    if (craftableA != craftableB) {
                        return Boolean.compare(
                                craftableB,
                                craftableA
                        );
                    }

                    return a.compareTo(b);
                }
        );

        return ordered;
    }

    private static boolean hasCraftingRecipe(
            String item
    ) {
        List<Recipe> recipes =
                Recipes.recipes.get(
                        normalize(item)
                );

        if (recipes == null
                || recipes.isEmpty()) {

            return false;
        }

        for (Recipe recipe : recipes) {
            if (recipe != null
                    && recipe.type == RecipeType.CRAFTING) {

                return true;
            }
        }

        return false;
    }

    /**
     * Scores a completed dependency branch.
     *
     * Lower is better.
     *
     * We prefer:
     *  - fewer unresolved missing materials;
     *  - fewer newly introduced missing units;
     *  - fewer planned crafting operations.
     */
    private static PlanScore score(
            PlanningState candidate,
            PlanningState base
    ) {
        long missingUnits = 0L;

        for (long amount :
                candidate.missing.values()) {

            missingUnits +=
                    Math.max(
                            0L,
                            amount
                    );
        }

        long baseMissingUnits = 0L;

        for (long amount :
                base.missing.values()) {

            baseMissingUnits +=
                    Math.max(
                            0L,
                            amount
                    );
        }

        long addedMissing =
                Math.max(
                        0L,
                        missingUnits
                                - baseMissingUnits
                );

        long taskCount =
                candidate.tasks.size();

        return new PlanScore(
                addedMissing,
                taskCount
        );
    }

    private static final class PlanScore
            implements Comparable<PlanScore> {

        private final long missing;
        private final long tasks;

        private PlanScore(
                long missing,
                long tasks
        ) {
            this.missing = missing;
            this.tasks = tasks;
        }

        @Override
        public int compareTo(
                PlanScore other
        ) {
            int missingComparison =
                    Long.compare(
                            missing,
                            other.missing
                    );

            if (missingComparison != 0) {
                return missingComparison;
            }

            return Long.compare(
                    tasks,
                    other.tasks
            );
        }
    }

    private static boolean requiresCraftingTable(
            Recipe recipe
    ) {
        if (recipe.gridSlots != null) {

            for (int i = 0;
                 i < recipe.gridSlots.length;
                 i++) {

                if (recipe.gridSlots[i] == null
                        || recipe.gridSlots[i].isBlank()) {

                    continue;
                }

                if (i / 3 >= 2
                        || i % 3 >= 2) {

                    return true;
                }
            }
        }

        return recipe
                .getEffectiveIngredientChoices()
                .size() > 4;
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

        return (
                value / divisor
                        + (
                        value % divisor == 0L
                                ? 0L
                                : 1L
                )
        );
    }

    private static String normalize(
            String id
    ) {
        if (id == null
                || id.isBlank()) {

            return null;
        }

        String value =
                id.trim();

        return value.startsWith("minecraft:")
                ? value
                : "minecraft:" + value;
    }

    /**
     * Shared transactional planning state.
     */
    private static final class PlanningState {

        /*
         * Inventory/materials still available to satisfy future dependencies.
         */
        final Map<String, Long> available =
                new HashMap<>();

        /*
         * Crafting operations already scheduled.
         */
        final List<CraftingTask> tasks =
                new ArrayList<>();

        /*
         * Materials that cannot currently be satisfied.
         */
        final Map<String, Long> missing =
                new LinkedHashMap<>();

        /*
         * Ingredient selections for the task currently being constructed.
         */
        Map<String, Long> lastIngredientTotals =
                new LinkedHashMap<>();

        long available(
                String item
        ) {
            String normalized =
                    normalize(item);

            if (normalized == null) {
                return 0L;
            }

            if (!available.containsKey(normalized)) {
                available.put(
                        normalized,
                        InventoryUtils.count(
                                normalized
                        )
                );
            }

            return available.getOrDefault(
                    normalized,
                    0L
            );
        }

        void addAvailable(
                String item,
                long count
        ) {
            String normalized =
                    normalize(item);

            if (normalized == null
                    || count <= 0L) {

                return;
            }

            available.put(
                    normalized,
                    available(normalized)
                            + count
            );
        }

        void consume(
                String item,
                long count
        ) {
            String normalized =
                    normalize(item);

            if (normalized == null
                    || count <= 0L) {

                return;
            }

            long have =
                    available(normalized);

            available.put(
                    normalized,
                    Math.max(
                            0L,
                            have - count
                    )
            );
        }

        PlanningState copy() {
            PlanningState copy =
                    new PlanningState();

            copy.available.putAll(
                    available
            );

            copy.tasks.addAll(
                    tasks
            );

            copy.missing.putAll(
                    missing
            );

            copy.lastIngredientTotals =
                    new LinkedHashMap<>(
                            lastIngredientTotals
                    );

            return copy;
        }

        void copyFrom(
                PlanningState other
        ) {
            available.clear();
            available.putAll(
                    other.available
            );

            tasks.clear();
            tasks.addAll(
                    other.tasks
            );

            missing.clear();
            missing.putAll(
                    other.missing
            );

            lastIngredientTotals =
                    new LinkedHashMap<>(
                            other.lastIngredientTotals
                    );
        }
    }
}