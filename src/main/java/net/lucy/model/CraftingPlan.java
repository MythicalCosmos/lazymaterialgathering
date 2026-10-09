package net.lucy.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable result of crafting-plan generation.
 */
public final class CraftingPlan {

    private final List<CraftingTask> tasks;
    private final Map<String, Long> missing;

    public CraftingPlan(
            List<CraftingTask> tasks,
            Map<String, Long> missing
    ) {
        this.tasks = List.copyOf(
                tasks == null ? List.of() : tasks
        );

        this.missing = Collections.unmodifiableMap(
                new LinkedHashMap<>(
                        missing == null ? Map.of() : missing
                )
        );
    }

    public List<CraftingTask> tasks() {
        return tasks;
    }

    public Map<String, Long> missing() {
        return missing;
    }

    public boolean isExecutable() {
        return missing.isEmpty();
    }
}