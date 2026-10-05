package net.lucy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One quantity-aware gathering objective.
 *
 * item() is the final item the player needs in their inventory.
 * sources() contains the actual blocks/resources that can produce it.
 *
 * Baritone is only given one selected source at a time by the queue.
 */
public final class GatheringTask {

    private final String item;
    private final long required;

    private final List<String> sources = new ArrayList<>();
    private long startingInventory;

    public GatheringTask(String item, long required) {
        this.item = normalize(item);
        this.required = Math.max(0L, required);
    }

    public String item() {
        return item;
    }

    public long required() {
        return required;
    }

    public List<String> sources() {
        return Collections.unmodifiableList(sources);
    }

    public String source() {
        return sources.isEmpty() ? null : sources.get(0);
    }

    public void setSource(String source) {
        sources.clear();

        if (source != null && !source.isBlank()) {
            sources.add(normalize(source));
        }
    }

    public void setSources(List<String> newSources) {
        sources.clear();

        if (newSources == null) {
            return;
        }

        for (String source : newSources) {
            if (source == null || source.isBlank()) {
                continue;
            }

            String normalized = normalize(source);

            if (!sources.contains(normalized)) {
                sources.add(normalized);
            }
        }
    }

    public long startingInventory() {
        return startingInventory;
    }

    public void setStartingInventory(long value) {
        startingInventory = Math.max(0L, value);
    }

    public long stillNeeded(long currentInventory) {
        return Math.max(0L, required - currentInventory);
    }

    public boolean complete(long currentInventory) {
        return currentInventory >= required;
    }

    private static String normalize(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}