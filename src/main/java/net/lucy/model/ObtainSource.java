package net.lucy.model;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record ObtainSource(Type type, String modifier, String description) {

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

    public ObtainSource(Type type) {
        this(type, null, null);
    }

    public ObtainSource(Type type, String modifier) {
        this(type, modifier, null);
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
        if (!(o instanceof ObtainSource(Type type1, String modifier1, String description1))) return false;
        return type == type1 && Objects.equals(modifier, modifier1) && Objects.equals(description, description1);
    }

    @Override
    public @NotNull String toString() {
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