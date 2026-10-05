package net.lucy.calc;

import net.lucy.data.InventoryUtils;
import net.lucy.model.GatheringTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts calculated raw-material totals into
 * quantity-aware gathering tasks.
 *
 * item() is the final item we need in the inventory.
 * source() is the actual block/resource that Baritone
 * should gather to obtain that item.
 */
public final class GatheringPlanner {

    private GatheringPlanner() {
    }

    public static List<GatheringTask> plan(
            Map<String, Long> rawMaterials
    ) {

        List<GatheringTask> result =
                new ArrayList<>();

        for (
                Map.Entry<String, Long> entry :
                rawMaterials.entrySet()
        ) {

            String item = normalize(entry.getKey());

            long required =
                    Math.max(
                            0L,
                            entry.getValue()
                    );

            if (item == null || required == 0L) {
                continue;
            }

            /*
             * The task represents the final inventory item.
             *
             * Example:
             *
             *     item = cobblestone
             *     required = 64
             *
             * The player needs 64 cobblestone, regardless
             * of which block produces it.
             */
            if (InventoryUtils.count(item) >= required) {
                continue;
            }

            GatheringTask task =
                    new GatheringTask(
                            item,
                            required
                    );

            /*
             * Determine what block should actually be mined.
             *
             * Example:
             *
             *     cobblestone -> stone
             *
             * when Silk Touch is disabled.
             */
            String source =
                    MiningSourceResolver.preferredSource(item);

            if (source == null || source.isBlank()) {

                System.out.println(
                        "[LMG] No valid mining source for "
                                + item
                                + ". Task will not be started."
                );

                continue;
            }

            task.setSource(source);

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