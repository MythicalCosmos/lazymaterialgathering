package net.lucy.calc;

import net.lucy.data.InventoryUtils;
import net.lucy.data.Recipes;
import net.lucy.model.ProcessingPlan;
import net.lucy.model.ProcessingTask;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds furnace-family processing tasks from the items that later
 * crafting steps actually require.
 *
 * Important:
 *
 *     Crafting target
 *          |
 *          v
 *     processing output
 *          |
 *          v
 *     processing input
 *          |
 *          v
 *     gathering
 *
 * Example:
 *
 *     iron_ingot x32
 *
 * becomes:
 *
 *     process raw_iron -> iron_ingot
 *
 * and the gathering stage is responsible for obtaining raw_iron.
 *
 * Non-processing recipes are deliberately ignored here. They belong
 * to CraftingPlanner, not ProcessingPlanner.
 */
public final class ProcessingPlanner {

    private ProcessingPlanner() {
    }

    public static ProcessingPlan plan(
            Map<String, Long> targetItems
    ) {
        Recipes.ensureLoaded();

        if (targetItems == null
                || targetItems.isEmpty()) {

            return new ProcessingPlan(
                    List.of(),
                    Map.of()
            );
        }

        List<ProcessingTask> tasks =
                new ArrayList<>();

        Map<String, Long> missing =
                new LinkedHashMap<>();

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

            planItem(
                    entry.getKey(),
                    required,
                    tasks,
                    missing
            );
        }

        return new ProcessingPlan(
                tasks,
                missing
        );
    }

    private static void planItem(
            String output,
            long required,
            List<ProcessingTask> tasks,
            Map<String, Long> missing
    ) {
        if (output == null
                || output.isBlank()
                || required <= 0L) {

            return;
        }

        String normalizedOutput =
                normalize(output);

        /*
         * We only care about furnace-family outputs here.
         *
         * A crafting recipe such as:
         *
         *     oak_planks -> sticks
         *
         * is not a processing task.
         *
         * It must simply be ignored by this planner.
         */
        Recipe chosen =
                findProcessingRecipe(
                        normalizedOutput
                );

        if (chosen == null) {
            return;
        }

        /*
         * Determine how much of the output is already available.
         */
        long have =
                InventoryUtils.count(
                        normalizedOutput
                );

        long outputNeeded =
                Math.max(
                        0L,
                        required - have
                );

        if (outputNeeded <= 0L) {
            return;
        }

        List<List<String>> choices =
                chosen.getEffectiveIngredientChoices();

        /*
         * Furnace-family recipes should have exactly one input.
         *
         * If a malformed/custom recipe doesn't satisfy that,
         * do not invent an input.
         */
        if (choices.size() != 1
                || choices.get(0) == null
                || choices.get(0).isEmpty()) {

            missing.merge(
                    normalizedOutput,
                    outputNeeded,
                    Long::sum
            );

            return;
        }

        String input =
                normalize(
                        choices.get(0).get(0)
                );

        if (input == null
                || input.isBlank()) {

            missing.merge(
                    normalizedOutput,
                    outputNeeded,
                    Long::sum
            );

            return;
        }

        long outputPerOperation =
                Math.max(
                        1L,
                        chosen.outputCount
                );

        long operations =
                ceilDivide(
                        outputNeeded,
                        outputPerOperation
                );

        /*
         * Vanilla furnace-family recipes consume exactly one
         * input item per processing operation.
         *
         * Keep this explicit in ProcessingTask so the coordinator
         * knows exactly how many input items it must load.
         */
        long inputRequired =
                operations;

        long inputAvailable =
                InventoryUtils.count(input);

        /*
         * Processing can only be executed for the portion for which
         * the required input exists.
         *
         * The missing input is intentionally returned to the caller.
         *
         * MaterialPipelineCoordinator can then send that missing
         * material to GatheringPlanner.
         */
        long processableOperations =
                Math.min(
                        operations,
                        inputAvailable
                );

        if (processableOperations > 0L) {

            tasks.add(
                    new ProcessingTask(
                            normalizedOutput,
                            input,
                            processableOperations,
                            1L,
                            chosen.type
                    )
            );
        }

        long missingInput =
                inputRequired
                        - processableOperations;

        if (missingInput > 0L) {

            missing.merge(
                    input,
                    missingInput,
                    Long::sum
            );
        }
    }

    /**
     * Finds the preferred furnace-family recipe for an output.
     */
    /**
     * Whether an item can be produced by one of the supported furnace-family
     * recipes. Used by the material pipeline to discover processing outputs
     * required as intermediate crafting ingredients.
     */
    public static boolean hasProcessingRecipe(String output) {
        Recipes.ensureLoaded();
        String normalized = normalize(output);
        return normalized != null
                && findProcessingRecipe(normalized) != null;
    }

    private static Recipe findProcessingRecipe(
            String output
    ) {
        List<Recipe> options =
                Recipes.recipes.getOrDefault(
                        output,
                        List.of()
                );

        if (options.isEmpty()) {
            return null;
        }

        /*
         * Respect recipe preferences where multiple processing
         * recipes exist.
         */
        List<Recipe> enabled =
                RecipeHeuristics.getEnabledOptions(
                        output,
                        options
                );

        if (enabled.isEmpty()) {
            return null;
        }

        /*
         * Prefer the configured recipe heuristic, but only accept
         * actual furnace-family processing recipes.
         */
        Recipe preferred =
                RecipeHeuristics.selectBestRecipe(
                        output,
                        enabled
                );

        if (preferred != null
                && isProcessingType(preferred.type)) {

            return preferred;
        }

        /*
         * Fallback in case the general heuristic selected a
         * non-processing recipe.
         */
        for (Recipe recipe : enabled) {

            if (recipe == null) {
                continue;
            }

            if (isProcessingType(recipe.type)) {
                return recipe;
            }
        }

        return null;
    }

    private static boolean isProcessingType(
            RecipeType type
    ) {
        return type == RecipeType.SMELTING
                || type == RecipeType.BLASTING
                || type == RecipeType.SMOKING;
    }

    private static long ceilDivide(
            long numerator,
            long denominator
    ) {
        if (numerator <= 0L) {
            return 0L;
        }

        if (denominator <= 0L) {
            return numerator;
        }

        return (numerator + denominator - 1L)
                / denominator;
    }

    private static String normalize(
            String id
    ) {
        if (id == null
                || id.isBlank()) {

            return null;
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}