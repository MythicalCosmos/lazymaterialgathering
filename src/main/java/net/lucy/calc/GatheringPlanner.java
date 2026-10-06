package net.lucy.calc;

import net.lucy.data.InventoryUtils;
import net.lucy.model.GatheringTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

            long required =
                    Math.max(0L, entry.getValue());

            if (item == null || required <= 0L) {
                continue;
            }

            long inventory =
                    InventoryUtils.count(item);

            if (inventory >= required) {
                continue;
            }

            GatheringTask task =
                    new GatheringTask(item, required);

            task.setStartingInventory(inventory);

            List<String> sources =
                    MiningSourceResolver.getPossibleSources(item);

            /*
             * Do not create a task that has no legitimate physical
             * source. Crafting/recipe resolution should have handled
             * craftable materials before this point.
             */
            if (sources.isEmpty()) {
                System.out.println(
                        "[LMG] No validated mining source for "
                                + item
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