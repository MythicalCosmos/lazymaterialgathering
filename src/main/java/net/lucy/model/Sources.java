package net.lucy.model;

import java.util.Objects;

public class Sources {
    private final SourceType sourceType;
    private final RecipeType recipeType; // Optional: connects to a recipe if CRAFTED/SMELTED
    private final String modifier;       // Optional: e.g., "SILK_TOUCH"

    // Constructor for a basic source (e.g., MOB_DROP, FARMED)
    public Sources(SourceType sourceType) {
        this(sourceType, null, null);
    }

    // Constructor for a source requiring a modifier (e.g., MINED with SILK_TOUCH)
    public Sources(SourceType sourceType, String modifier) {
        this(sourceType, null, modifier);
    }

    // Constructor for a source linked to a recipe (e.g., CRAFTED via CRAFTING)
    public Sources(SourceType sourceType, RecipeType recipeType) {
        this(sourceType, recipeType, null);
    }

    // Full constructor
    public Sources(SourceType sourceType, RecipeType recipeType, String modifier) {
        this.sourceType = sourceType;
        this.recipeType = recipeType;
        this.modifier = modifier;
    }

    // Getters
    public SourceType getSourceType() { return sourceType; }
    public RecipeType getRecipeType() { return recipeType; }
    public String getModifier() { return modifier; }

    // Helpers to easily identify requirements
    public boolean isSilkTouchRequired() {
        return "SILK_TOUCH".equalsIgnoreCase(modifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sources)) return false;
        Sources source = (Sources) o;
        return sourceType == source.sourceType &&
                recipeType == source.recipeType &&
                Objects.equals(modifier, source.modifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceType, recipeType, modifier);
    }

    @Override
    public String toString() {
        return "Source{" +
                "type=" + sourceType +
                (recipeType != null ? ", recipe=" + recipeType : "") +
                (modifier != null ? ", modifier='" + modifier + '\'' : "") +
                '}';
    }
}
