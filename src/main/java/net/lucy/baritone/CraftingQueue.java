package net.lucy.baritone;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lucy.model.CraftingTask;
import net.lucy.model.CraftingTaskState;

import java.util.Map;

public final class CraftingQueue {
    private CraftingQueue() {}
    public static void start(Map<String, Long> targetItems) {CraftingCoordinator.start(targetItems);}
    public static void startFromSchematic() {CraftingCoordinator.startFromSchematic();}
    public static void stop() {CraftingCoordinator.stop();}
    public static void pause() {CraftingCoordinator.pause();}
    public static void resume() {CraftingCoordinator.resume();}
    public static boolean isRunning() {return CraftingCoordinator.isRunning();}
    public static boolean isPaused() {return CraftingCoordinator.isPaused();}
    public static CraftingTaskState getState() {return CraftingCoordinator.getState();}
    public static CraftingTask getCurrentTask() {return CraftingCoordinator.getCurrentTask();}
    public static int getRemainingTasks() {return CraftingCoordinator.getRemainingTasks();}
    public static long getCraftsRemaining() {return CraftingCoordinator.getCraftsRemaining();}
    public static String getWaitingReason() {return CraftingCoordinator.getWaitingReason();}
    public static String getBlockedReason() {return CraftingCoordinator.getBlockedReason();}
    public static void register() {
        CraftingCoordinator.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> CraftingCoordinator.tick());
    }
    public static void tick() {CraftingCoordinator.tick();}
}