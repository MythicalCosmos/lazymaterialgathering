package net.lucy.model;

import java.util.Objects;

public class ObtainSource {

    public enum Type {
        CRAFTED,
        SMELTED,
        BREWED,
        MINED,
        MOB_DROP,
        FARMED,
        TRADING,
        FISHING,
        CHEST_LOOT,
        BARTERING,
        NATURAL,
        OTHER
    }

    private final Type type;
    private final String modifier;
    private final String description;

    public ObtainSource(Type type) {
        this(type, null, null);
    }

    public ObtainSource(Type type, String modifier) {
        this(type, modifier, null);
    }

    public ObtainSource(Type type, String modifier, String description) {
        this.type = type;
        this.modifier = modifier;
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public String getModifier() {
        return modifier;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasModifier() {
        return modifier != null && !modifier.isEmpty();
    }

    public boolean hasDescription() {
        return description != null && !description.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObtainSource)) return false;

        ObtainSource that = (ObtainSource) o;

        return type == that.type
                && Objects.equals(modifier, that.modifier)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, modifier, description);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(type.name());

        if (modifier != null) {
            result.append("[").append(modifier).append("]");
        }

        if (description != null) {
            result.append(": ").append(description);
        }

        return result.toString();
    }
}