package net.lucy.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One deterministic crafting operation in the generated crafting plan.
 */
public final class CraftingTask {

    private final String item;
    private final long required;
    private final long crafts;
    private final long outputPerCraft;
    private final String recipeId;
    private final Map<String, Long> ingredients;
    private final boolean requiresCraftingTable;

    public CraftingTask(
            String item,
            long required,
            long crafts,
            long outputPerCraft,
            String recipeId,
            Map<String, Long> ingredients,
            boolean requiresCraftingTable
    ) {
        this.item = item;
        this.required = Math.max(0L, required);
        this.crafts = Math.max(0L, crafts);
        this.outputPerCraft = Math.max(1L, outputPerCraft);
        this.recipeId = recipeId;

        this.ingredients = Collections.unmodifiableMap(
                new LinkedHashMap<>(
                        ingredients == null ? Map.of() : ingredients
                )
        );

        this.requiresCraftingTable = requiresCraftingTable;
    }

    public String item() {
        return item;
    }

    public long required() {
        return required;
    }

    public long crafts() {
        return crafts;
    }

    public long outputPerCraft() {
        return outputPerCraft;
    }

    public String recipeId() {
        return recipeId;
    }

    public Map<String, Long> ingredients() {
        return ingredients;
    }

    public boolean requiresCraftingTable() {
        return requiresCraftingTable;
    }
}