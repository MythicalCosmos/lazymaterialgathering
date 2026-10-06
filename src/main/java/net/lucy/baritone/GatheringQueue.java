package net.lucy.baritone;

import net.lucy.model.GatheringTask;
import net.lucy.model.GatheringTaskState;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compatibility facade.
 *
 * The actual gathering state machine lives in GatheringCoordinator.
 */
public final class GatheringQueue {

    private GatheringQueue() {
    }

    public static void start(
            Map<String, Long> materials
    ) {
        GatheringCoordinator.start(materials);
    }

    public static void start(
            List<GatheringTask> tasks
    ) {
        GatheringCoordinator.start(tasks);
    }

    public static void stop() {
        GatheringCoordinator.stop();
    }

    public static void pause() {
        GatheringCoordinator.pause();
    }

    public static void resume() {
        GatheringCoordinator.resume();
    }

    public static boolean isRunning() {
        return GatheringCoordinator.isRunning();
    }

    public static boolean isPaused() {
        return GatheringCoordinator.isPaused();
    }

    public static GatheringTaskState getState() {
        return GatheringCoordinator.getState();
    }

    public static boolean isExploring() {
        return GatheringCoordinator.getState()
                == GatheringTaskState.EXPLORING;
    }

    public static String getCurrentItem() {
        return GatheringCoordinator.getCurrentItem();
    }

    public static long getCurrentRequired() {
        return GatheringCoordinator.getCurrentRequired();
    }

    public static long getCurrentInventory() {
        return GatheringCoordinator.getCurrentInventory();
    }

    public static long getCurrentStillNeeded() {
        return GatheringCoordinator.getCurrentStillNeeded();
    }

    public static int getRemainingCount() {
        return GatheringCoordinator.getRemainingCount();
    }

    public static BlockPos getCurrentResourceTarget() {
        return GatheringCoordinator.getCurrentResourceTarget();
    }

    public static BlockPos getCurrentExplorationTarget() {
        return GatheringCoordinator.getCurrentExplorationTarget();
    }

    public static String getCurrentResourceSource() {
        return GatheringCoordinator.getCurrentResourceSource();
    }

    public static Optional<Double>
    getEstimatedSecondsRemaining() {
        /*
         * Add the existing ETA implementation here if the old queue
         * already has one.
         */
        return Optional.empty();
    }

    public static void register() {
        GatheringCoordinator.register();
    }

    public static void tick() {
        GatheringCoordinator.tick();
    }
}