package net.lucy.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable furnace-family processing plan.
 */
public final class ProcessingPlan {

    private final List<ProcessingTask> tasks;
    private final Map<String, Long> missing;

    public ProcessingPlan(
            List<ProcessingTask> tasks,
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

    public List<ProcessingTask> tasks() {
        return tasks;
    }

    public Map<String, Long> missing() {
        return missing;
    }

    public boolean isExecutable() {
        return missing.isEmpty();
    }
}