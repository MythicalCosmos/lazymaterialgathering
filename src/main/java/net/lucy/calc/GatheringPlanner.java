package net.lucy.calc;

import net.lucy.data.InventoryUtils;
import net.lucy.model.GatheringTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts calculated raw-material totals into
 * quantity-aware gathering tasks.
 */
public final class GatheringPlanner {

    private GatheringPlanner() {
    }

    public static List<GatheringTask> plan(
            Map<String, Long> rawMaterials
    ) {
        List<GatheringTask> result = new ArrayList<>();

        if (rawMaterials == null || rawMaterials.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, Long> entry : rawMaterials.entrySet()) {

            String item = normalize(entry.getKey());
            long required = Math.max(0L, entry.getValue());

            if (item == null || required == 0L) {
                continue;
            }

            long inventory = InventoryUtils.count(item);

            if (inventory >= required) {
                continue;
            }

            GatheringTask task =
                    new GatheringTask(item, required);

            List<String> sources =
                    MiningSourceResolver.getPossibleSources(item);

            if (sources.isEmpty()) {
                System.out.println(
                        "[LMG] No valid mining source for "
                                + item
                                + ". Task will not be started."
                );

                continue;
            }

            task.setSources(sources);
            result.add(task);
        }

        return result;
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