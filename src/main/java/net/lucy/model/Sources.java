package net.lucy.model;

import java.util.Objects;

public class Sources {

    public enum Type {
        CRAFTED, SMELTED, BREWED, MINED, MOB_DROP, FARMED, NATURAL, OTHER
    }

    private final Type type;
    private final String modifier;

    public Sources(Type type) {
        this(type, null);
    }

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
