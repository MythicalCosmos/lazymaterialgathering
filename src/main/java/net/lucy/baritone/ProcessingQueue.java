package net.lucy.baritone;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lucy.model.ProcessingTask;
import net.lucy.model.ProcessingTaskState;

import java.util.Map;

public final class ProcessingQueue {
    private ProcessingQueue() {
    }

    public static void start(Map<String, Long> targets) {
        ProcessingCoordinator.start(targets);
    }

    public static void startFromCalculation() {
        ProcessingCoordinator.startFromCalculation();
    }

    public static void stop() {
        ProcessingCoordinator.stop();
    }

    public static void pause() {
        ProcessingCoordinator.pause();
    }

    public static void resume() {
        ProcessingCoordinator.resume();
    }

    public static boolean isRunning() {
        return ProcessingCoordinator.isRunning();
    }

    public static boolean isPaused() {
        return ProcessingCoordinator.isPaused();
    }

    public static ProcessingTaskState getState() {
        return ProcessingCoordinator.getState();
    }

    public static ProcessingTask getCurrentTask() {
        return ProcessingCoordinator.getCurrentTask();
    }

    public static int getRemainingTasks() {
        return ProcessingCoordinator.getRemainingTasks();
    }

    public static String getWaitingReason() {
        return ProcessingCoordinator.getWaitingReason();
    }

    public static String getBlockedReason() {
        return ProcessingCoordinator.getBlockedReason();
    }

    public static void register() {
        ProcessingCoordinator.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ProcessingCoordinator.tick());
    }

    public static void tick() {
        ProcessingCoordinator.tick();
    }
}