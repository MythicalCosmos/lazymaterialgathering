package net.lucy.progress;

import net.lucy.data.InventoryUtils;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class SchematicProgress {
    private final String schematicName;
    private final Map<String, Long> required = new TreeMap<>();
    public SchematicProgress(String schematicName, Map<String, Long> requirements) {
        this.schematicName = schematicName == null || schematicName.isBlank() ? "Loaded Schematic" : schematicName;
        if (requirements != null) {
            for (Map.Entry<String, Long> entry : requirements.entrySet()) {
                long amount = Math.max(0L, entry.getValue());
                if (amount > 0L) {
                    required.put(entry.getKey(), amount);
                }
            }
        }
    }

    public String getName() {
        return schematicName;
    }

    public Map<String, Long> getRequired() {
        return Collections.unmodifiableMap(required);
    }

    public long getRequired(String item) {
        return required.getOrDefault(item, 0L);
    }

    /**
     * Reads the player's inventory live.
     *
     * This means the overlay does not need to be manually
     * notified every time an item enters the inventory.
     */
    public long getObtained(String item) {
        return InventoryUtils.count(item);
    }

    public long getRemaining(String item) {
        long requiredAmount = getRequired(item);
        long obtained = getObtained(item);
        return Math.max(0L, requiredAmount - obtained);
    }

    public long getTotalRequired() {
        long total = 0L;
        for (long amount : required.values()) {
            total += amount;
        }
        return total;
    }

    public long getTotalObtained() {
        long total = 0L;
        for (String item : required.keySet()) {
            total += Math.min(getRequired(item), getObtained(item));
        }
        return total;
    }

    /**
     * Overall gathering progress.
     *
     * This is based on the calculated raw materials
     * rather than schematic block placement.
     */
    public double getProgress() {
        long total = getTotalRequired();
        if (total <= 0L) {
            return 1.0;
        }

        return Math.min(1.0, (double) getTotalObtained() / (double) total);
    }

    public boolean isComplete() {
        return getProgress() >= 1.0;
    }
}