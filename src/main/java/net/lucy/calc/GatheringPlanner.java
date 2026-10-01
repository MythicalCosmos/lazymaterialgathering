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

        List<GatheringTask> result =
                new ArrayList<>();

        for (
                Map.Entry<String, Long> entry :
                rawMaterials.entrySet()
        ) {

            long required =
                    Math.max(
                            0L,
                            entry.getValue()
                    );

            if (required == 0L) {
                continue;
            }

            /*
             * The task stores the final inventory amount
             * that is required.
             *
             * GatheringQueue checks the current inventory
             * and only gathers the missing amount.
             */
            if (
                    InventoryUtils.count(
                            entry.getKey()
                    ) < required
            ) {

                result.add(
                        new GatheringTask(
                                entry.getKey(),
                                required
                        )
                );
            }
        }

        return result;
    }
}