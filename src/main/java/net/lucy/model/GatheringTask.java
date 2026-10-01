package net.lucy.model;

/**
 * One quantity-aware gathering objective.
 *
 * required() is the final amount the player needs to have in their inventory.
 * The GatheringQueue compares that against the player's current inventory.
 */
public final class GatheringTask {

    private final String item;
    private final long required;

    private long startingInventory;

    public GatheringTask(String item, long required) {
        this.item = item;
        this.required = Math.max(0L, required);
    }

    public String item() {
        return item;
    }

    public long required() {
        return required;
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
}