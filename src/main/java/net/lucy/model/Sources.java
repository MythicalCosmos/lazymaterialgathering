package net.lucy.model;

import java.util.Objects;

public class Sources {

    // Everything—recipes, mining, drops—is unified into this single enum
    public enum Type {
        CRAFTED, SMELTED, BREWED, MINED, MOB_DROP, FARMED, NATURAL, OTHER
    }

    private final Type type;
    private final String modifier; // e.g., "SILK_TOUCH", or null if not needed

    // Standard constructor for straightforward sources (e.g., SMELTED, MOB_DROP)
    public Sources(Type type) {
        this(type, null);
    }

    // Constructor for sources needing a specific rule/condition (e.g., MINED with SILK_TOUCH)
    public Sources(Type type, String modifier) {
        this.type = type;
        this.modifier = modifier;
    }

    public Type getType() { return type; }
    public String getModifier() { return modifier; }
    public boolean hasModifier() { return modifier != null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sources)) return false;
        Sources sources = (Sources) o;
        return type == sources.type && Objects.equals(modifier, sources.modifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, modifier);
    }

    @Override
    public String toString() {
        return type + (modifier != null ? "[" + modifier + "]" : "");
    }
}
