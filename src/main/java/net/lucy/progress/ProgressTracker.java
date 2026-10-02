package net.lucy.progress;

import java.util.Map;

public final class ProgressTracker {
    private static final ProgressTracker INSTANCE = new ProgressTracker();
    private SchematicProgress current;
    private ProgressTracker() {
    }

    public static ProgressTracker get() {
        return INSTANCE;
    }

    public void start(String schematicName, Map<String, Long> requirements) {
        current = new SchematicProgress(schematicName, requirements);
    }

    public void clear() {
        current = null;
    }

    public SchematicProgress getCurrent() {
        return current;
    }

    public boolean hasCurrent() {
        return current != null;
    }
}