package net.lucy.calc;

import net.lucy.config.Configs;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecipeFinder {
    private RecipeFinder() {
    }

    /**
     * Finds every recipe path for an item.
     *
     * This includes:
     * - every recipe producing the item
     * - every alternative Ingredient choice
     * - every recursive combination underneath those choices
     */
    public static List<RecipePath> findAll(String itemName) {
        return find(itemName, false);
    }

    /**
     * Same as findAll(), but respects the player's enabled/disabled
     * recipe settings.
     */
    public static List<RecipePath> findAllEnabled(String itemName) {
        return find(itemName, true);
    }

    private static List<RecipePath> find(String itemName, boolean enabledOnly) {
        return findRecursive(itemName, enabledOnly, new HashSet<>());
    }

    private static List<RecipePath> findRecursive(String itemName, boolean enabledOnly, Set<String> visiting) {
        /*
         * Cycle protection.
         *
         * Example:
         *
         * A -> B
         * B -> A
         *
         * We still return the path, but stop expanding it.
         */
        if (visiting.contains(itemName)) {
            return List.of(RecipePath.cycle(itemName));
        }

        List<Recipe> recipes = Recipes.recipes.get(itemName);
        if (recipes == null || recipes.isEmpty()) {
            return List.of(RecipePath.rawMaterial(itemName));
        }

        List<Recipe> options = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (!enabledOnly || Configs.isRecipeEnabled(itemName, recipe.id)) {
                options.add(recipe);
            }
        }

        /*
         * If the user disabled everything, retain the same safety behavior
         * as RawMaterials.
         */
        if (options.isEmpty()) {
            options.addAll(recipes);
        }

        Set<String> nextVisiting = new HashSet<>(visiting);
        nextVisiting.add(itemName);
        List<RecipePath> result = new ArrayList<>();
        for (Recipe recipe : options) {
            List<List<String>> choices = recipe.getEffectiveIngredientChoices();
            if (choices.isEmpty()) {
                /*
                 * We intentionally do not treat zero-ingredient recipes as
                 * free resources.
                 */
                continue;
            }

            List<List<IngredientPath>> slotPaths = new ArrayList<>();
            boolean failed = false;
            /*
             * For each ingredient slot:
             *
             *   slot -> every possible item choice
             *        -> every possible path for that item
             */
            for (List<String> slotChoices : choices) {
                List<IngredientPath> pathsForSlot = new ArrayList<>();
                if (slotChoices == null || slotChoices.isEmpty()) {
                    failed = true;
                    break;
                }

                for (String ingredientName : slotChoices) {
                    List<RecipePath> paths = findRecursive(ingredientName, enabledOnly, nextVisiting);
                    for (RecipePath path : paths) {
                        pathsForSlot.add(new IngredientPath(ingredientName, path));
                    }
                }

                if (pathsForSlot.isEmpty()) {
                    failed = true;
                    break;
                }
                slotPaths.add(pathsForSlot);
            }

            if (failed) {
                continue;
            }

            /*
             * Cartesian product across every ingredient slot.
             *
             * This is what turns:
             *
             * slot 1: A/B
             * slot 2: C/D
             *
             * into:
             *
             * A+C
             * A+D
             * B+C
             * B+D
             */
            List<List<IngredientPath>> combinations = new ArrayList<>();
            buildCombinations(slotPaths, 0, new ArrayList<>(), combinations);
            for (List<IngredientPath> combination : combinations) {
                result.add(new RecipePath(itemName, recipe, combination, false, false));
            }
        }
        return result;
    }

    private static void buildCombinations(List<List<IngredientPath>> slots, int index, List<IngredientPath> current, List<List<IngredientPath>> result) {
        if (index >= slots.size()) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (IngredientPath path : slots.get(index)) {
            current.add(path);
            buildCombinations(slots, index + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private record IngredientPath(String ingredientName, RecipePath path) {
    }

    public static final class RecipePath {
        private final String itemName;
        private final Recipe recipe;
        private final List<IngredientPath> ingredients;
        private final boolean rawMaterial;
        private final boolean cycle;
        private RecipePath(String itemName, Recipe recipe, List<IngredientPath> ingredients, boolean rawMaterial, boolean cycle) {
            this.itemName = itemName;
            this.recipe = recipe;
            this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
            this.rawMaterial = rawMaterial;
            this.cycle = cycle;
        }

        private static RecipePath rawMaterial(String itemName) {
            return new RecipePath(itemName, null, List.of(), true, false);
        }

        private static RecipePath cycle(String itemName) {
            return new RecipePath(itemName, null, List.of(), false, true);
        }

        public String getItemName() {
            return itemName;
        }

        public Recipe getRecipe() {
            return recipe;
        }

        public List<RecipePath> getIngredients() {
            List<RecipePath> result = new ArrayList<>();
            for (IngredientPath ingredient : ingredients) {
                result.add(ingredient.path());
            }
            return Collections.unmodifiableList(result);
        }

        public List<String> getIngredientNames() {
            List<String> result = new ArrayList<>();
            for (IngredientPath ingredient : ingredients) {
                result.add(ingredient.ingredientName());
            }
            return Collections.unmodifiableList(result);
        }

        public boolean isRawMaterial() {
            return rawMaterial;
        }

        public boolean isCycle() {
            return cycle;
        }

        public boolean isRecipe() {
            return recipe != null;
        }

        public String getRecipeId() {
            return recipe == null ? null : recipe.id;
        }

        public int getOutputCount() {
            return recipe == null ? 1 : Math.max(1, recipe.outputCount);
        }

        @Override
        public String toString() {
            if (rawMaterial) {
                return itemName;
            }

            if (cycle) {
                return itemName + " [cycle]";
            }

            StringBuilder result = new StringBuilder();
            result.append(itemName).append("<-").append(recipe.id).append(" [");

            for (int i = 0; i < ingredients.size(); i++) {
                if (i > 0) {
                    result.append(", ");
                }
                result.append(ingredients.get(i).path());
            }
            result.append("]");
            return result.toString();
        }
    }
}