package net.lucy.calc;

import net.lucy.data.InventoryUtils;
import net.lucy.model.GatheringTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds validated, quantity-aware gathering tasks from the
 * calculated raw-material list.
 */
public final class GatheringPlanner {

    private GatheringPlanner() {
    }

    public static List<GatheringTask> plan(
            Map<String, Long> rawMaterials
    ) {
        List<GatheringTask> result =
                new ArrayList<>();

        if (rawMaterials == null
                || rawMaterials.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, Long> entry
                : rawMaterials.entrySet()) {

            String item =
                    normalize(entry.getKey());

            long required =
                    Math.max(
                            0L,
                            entry.getValue()
                    );

            if (item == null
                    || required <= 0L) {
                continue;
            }

            long inventory =
                    InventoryUtils.count(item);

            /*
             * The player already has enough of this
             * desired inventory item.
             */
            if (inventory >= required) {
                continue;
            }

            /*
             * Resolve the desired inventory item into
             * actual physical blocks that can produce it.
             */
            List<String> sources =
                    MiningSourceResolver
                            .getPossibleSources(item);

            /*
             * Preserve unsupported items in the plan too. The coordinator
             * can then report them explicitly instead of silently dropping
             * required materials from the user's gathering plan.
             */
            GatheringTask task =
                    new GatheringTask(
                            item,
                            required
                    );

            task.setStartingInventory(
                    inventory
            );

            task.setSources(
                    sources
            );

            result.add(task);
        }

        /*
         * Mine the largest requested quantities first.
         */
        result.sort(
                Comparator.comparingLong(
                        GatheringTask::required
                ).reversed()
        );

        return result;
    }

    private static String normalize(
            String id
    ) {
        if (id == null
                || id.isBlank()) {
            return null;
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}