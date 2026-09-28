package net.lucy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Recipe {
    public String id;
    public RecipeType type;

    /*
     * Legacy/simple ingredient representation.
     *
     * Example:
     * oak_planks -> 4
     */
    public Map<String, Integer> ingredients;

    /*
     * Every ingredient slot, with every item that can satisfy that slot.
     *
     * Example:
     *
     * [
     *   ["oak_planks", "birch_planks", "spruce_planks"],
     *   ["oak_planks", "birch_planks", "spruce_planks"]
     * ]
     *
     * means two slots, each accepting any of those planks.
     *
     * Repeated slots are intentional and represent quantity.
     */
    public List<List<String>> ingredientChoices;

    public int outputCount;

    /*
     * 3x3 crafting grid.
     *
     * This is only a display/layout representation.
     * The complete alternatives are preserved in ingredientChoices.
     */
    public String[] gridSlots;

    public Recipe(
            String id,
            RecipeType type,
            Map<String, Integer> ingredients,
            int outputCount
    ) {
        this(
                id,
                type,
                ingredients,
                new ArrayList<>(),
                outputCount
        );
    }

    public Recipe(
            String id,
            RecipeType type,
            Map<String, Integer> ingredients,
            List<List<String>> ingredientChoices,
            int outputCount
    ) {
        this.id = id;
        this.type = type;
        this.ingredients = ingredients != null
                ? new LinkedHashMap<>(ingredients)
                : new LinkedHashMap<>();

        this.ingredientChoices = copyChoices(ingredientChoices);
        this.outputCount = outputCount;
    }

    public Recipe(String id, Map<String, Integer> ingredients) {
        this(id, RecipeType.CRAFTING, ingredients, 1);
    }

    public List<List<String>> getIngredientChoices() {
        return Collections.unmodifiableList(ingredientChoices);
    }

    /**
     * Returns ingredient choices even for old cached recipes that were
     * created before ingredientChoices existed.
     */
    public List<List<String>> getEffectiveIngredientChoices() {
        if (!ingredientChoices.isEmpty()) {
            return ingredientChoices;
        }

        List<List<String>> result = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            int count = Math.max(0, entry.getValue());

            for (int i = 0; i < count; i++) {
                result.add(Collections.singletonList(entry.getKey()));
            }
        }

        return result;
    }

    private static List<List<String>> copyChoices(
            List<List<String>> choices
    ) {
        List<List<String>> result = new ArrayList<>();

        if (choices == null) {
            return result;
        }

        for (List<String> choicesForSlot : choices) {
            if (choicesForSlot == null) {
                result.add(new ArrayList<>());
            } else {
                result.add(new ArrayList<>(choicesForSlot));
            }
        }

        return result;
    }
}