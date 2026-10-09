package net.lucy.baritone;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.Map;

public final class MaterialPipelineQueue {
    private MaterialPipelineQueue() {}
    public static void start(Map<String, Long> targets) {MaterialPipelineCoordinator.start(targets);}
    public static void startFromCalculation() {MaterialPipelineCoordinator.startFromCalculation();}
    public static void stop() {MaterialPipelineCoordinator.stop();}
    public static void pause() {MaterialPipelineCoordinator.pause();}
    public static void resume() {MaterialPipelineCoordinator.resume();}
    public static boolean isRunning() {return MaterialPipelineCoordinator.isRunning();}
    public static boolean isPaused() {return MaterialPipelineCoordinator.isPaused();}
    public static MaterialPipelineState getState() {return MaterialPipelineCoordinator.getState();}
    public static String getCurrentItem() {return MaterialPipelineCoordinator.getCurrentItem();}
    public static int getRemainingTasks() {return MaterialPipelineCoordinator.getRemainingTasks();}
    public static String getBlockedReason() {return MaterialPipelineCoordinator.getBlockedReason();}
    public static void register() {
        MaterialPipelineCoordinator.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> MaterialPipelineCoordinator.tick());
    }
    public static void tick() {MaterialPipelineCoordinator.tick();}
}