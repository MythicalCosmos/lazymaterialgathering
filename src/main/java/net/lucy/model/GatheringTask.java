package net.lucy.model;

/**
 * One quantity-aware gathering objective.
 *
 * item() is the final item the player needs in their inventory.
 * source() is the actual block/resource Baritone should gather.
 */
public final class GatheringTask {

    private final String item;
    private final long required;

    private String source;
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

    public String source() {
        return source;
    }

    public void setSource(String source) {
        this.source = normalize(source);
    }

    public long startingInventory() {
        return startingInventory;
    }

    public void setStartingInventory(long value) {
        startingInventory = Math.max(0L, value);
    }

    public long stillNeeded(long currentInventory) {
        return Math.max(
                0L,
                required - currentInventory
        );
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