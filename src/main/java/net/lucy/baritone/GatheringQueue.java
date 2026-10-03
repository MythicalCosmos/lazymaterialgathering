package net.lucy.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lucy.calc.GatheringPlanner;
import net.lucy.config.Configs;
import net.lucy.data.InventoryUtils;
import net.lucy.data.WorldKnowledge;
import net.lucy.model.GatheringTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GatheringQueue {
    private enum State {
        IDLE,
        MOVING_TO_RESOURCE,
        EXPLORING,
        MINING,
        RETURNING_TO_DEPOSIT,
        PAUSED
    }

    private static final Deque<GatheringTask> QUEUE = new ArrayDeque<>();
    private static State state = State.IDLE;
    private static State stateBeforePause;
    private static GatheringTask currentTask;
    private static int failedAttempts;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static BlockPos currentResourceTarget;
    private static BlockPos currentExplorationTarget;
    private static String currentResourceSource;
    private GatheringQueue() {
    }

    public static void start(Map<String, Long> materials) {
        start(GatheringPlanner.plan(materials));
    }

    public static void start(
            List<GatheringTask> tasks) {
        stop(false);
        WorldKnowledge.rescanLoadedArea(1);
        if (tasks != null) {
            QUEUE.addAll(tasks);
        }
        startNextItem();
    }

    public static void stop() {
        stop(true);
    }

    private static void stop(boolean cancelBaritone) {
        state = State.IDLE;
        stateBeforePause = null;
        currentTask = null;
        currentResourceTarget = null;
        currentExplorationTarget = null;
        currentResourceSource = null;
        failedAttempts = 0;
        QUEUE.clear();
        if (cancelBaritone) {
            BaritoneController.cancelAll();
        }
    }

    public static void pause() {
        if (state == State.IDLE || state == State.PAUSED) {
            return;
        }

        stateBeforePause = state;
        state = State.PAUSED;
        BaritoneController.pause();
    }

    public static void resume() {
        if (state != State.PAUSED) {
            return;
        }

        state = stateBeforePause == null ? State.MINING : stateBeforePause;
        stateBeforePause = null;
        BaritoneController.resume();
    }

    public static boolean isRunning() {
        return state != State.IDLE;
    }

    public static boolean isPaused() {
        return state == State.PAUSED;
    }

    public static boolean isExploring() {
        return state == State.EXPLORING;
    }

    public static String getCurrentItem() {
        return currentTask == null ? null : currentTask.item();
    }

    public static long getCurrentRequired() {
        return currentTask == null ? 0L : currentTask.required();
    }

    public static long getCurrentInventory() {
        return currentTask == null ? 0L : InventoryUtils.count(currentTask.item());
    }

    public static long getCurrentStillNeeded() {
        return currentTask == null ? 0L : currentTask.stillNeeded(getCurrentInventory());
    }

    public static int getRemainingCount() {
        return QUEUE.size() + (currentTask == null || state == State.IDLE ? 0 : 1);
    }

    public static BlockPos getCurrentResourceTarget() {
        return currentResourceTarget;
    }

    public static BlockPos getCurrentExplorationTarget() {
        return currentExplorationTarget;
    }

    public static String getCurrentResourceSource() {
        return currentResourceSource;
    }

    public static Optional<Double>
    getEstimatedSecondsRemaining() {
        return BaritoneController.get().getPathingBehavior().estimatedTicksToGoal().map(ticks -> ticks / 20.0);
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        if (state == State.IDLE || state == State.PAUSED || currentTask == null) {
            return;
        }

        long have = InventoryUtils.count(currentTask.item());
        if (currentTask.complete(have)) {
            BaritoneController.cancelMining();
            goToDepositOrNext();
            return;
        }

        switch (state) {
            case MOVING_TO_RESOURCE ->
                    tickMovingToResource();
            case EXPLORING ->
                    tickExploring();
            case MINING ->
                    tickMining();
            case RETURNING_TO_DEPOSIT -> {
                if (!BaritoneController.isPathing()) {
                    startNextItem();
                }
            }
            default -> {
            }
        }
    }

    private static void tickMovingToResource() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        if (BaritoneController.isPathing()) {
            return;
        }

        if (currentResourceTarget == null) {
            findAndGoToKnownResource();
            return;
        }

        double squaredDistance = client.player.getBlockPos().getSquaredDistance(currentResourceTarget);
        if (squaredDistance <= 25.0) {
            startMiningCurrent();
        }
        /*
         * Baritone may stop within several blocks of the
         * target rather than directly on it.
         */

        /*
         * The target may have become invalid while traveling.
         */
        if (!findAndGoToKnownResource()) {
            beginExploration();
        }
    }

    private static void tickExploring() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        /*
         * New chunks may have revealed the requested resource.
         */
        if (findAndGoToKnownResource()) {
            return;
        }

        /*
         * Keep the knowledge database fed while traveling.
         */
        WorldKnowledge.rescanLoadedArea(2);
        if (BaritoneController.isPathing()) {
            return;
        }

        Optional<BlockPos> next = findExplorationTarget();
        if (next.isPresent()) {
            currentExplorationTarget = next.get();
            BaritoneController.goTo(currentExplorationTarget);
            return;
        }
        /*
         * No frontier could be found.
         *
         * Let Baritone try its normal mining behavior as a
         * final fallback.
         */
        startMiningCurrent();
    }

    private static void tickMining() {
        long have = InventoryUtils.count(currentTask.item());
        if (currentTask.complete(have)) {
            goToDepositOrNext();
            return;
        }

        if (BaritoneController.isMining()) {
            return;
        }

        /*
         * The mining process stopped without fulfilling the
         * quantity. First look for newly discovered known sources.
         */
        if (findAndGoToKnownResource()) {
            failedAttempts = 0;
            return;
        }

        failedAttempts++;
        if (failedAttempts < MAX_FAILED_ATTEMPTS) {
            if (beginExploration()) {
                return;
            }
            /*
             * No frontier target yet. Give the normal mine process
             * another chance.
             */
            startMiningCurrent();
            return;
        }
        System.out.println("[LMG] Could not gather enough of " + currentTask.item() + " after " + MAX_FAILED_ATTEMPTS + " attempts.");
        state = State.IDLE;
    }

    private static void goToDepositOrNext() {
        Optional<BetterBlockPos> deposit = Configs.Generic.RETURN_TO_DEPOSIT_BETWEEN_ITEMS.getBooleanValue() ? DepositLocations.get() : Optional.empty();
        if (deposit.isPresent()) {
            state = State.RETURNING_TO_DEPOSIT;
            BaritoneController.goTo(deposit.get());
        } else {
            startNextItem();
        }
    }

    private static void startNextItem() {
        currentTask = QUEUE.pollFirst();
        failedAttempts = 0;
        currentResourceTarget = null;
        currentExplorationTarget = null;
        currentResourceSource = null;
        if (currentTask == null) {
            state = State.IDLE;
            return;
        }

        long inventory = InventoryUtils.count(currentTask.item());
        currentTask.setStartingInventory(inventory);
        if (currentTask.complete(inventory)) {
            startNextItem();
            return;
        }
        if (!findAndGoToKnownResource()) {
            if (!beginExploration()) {
                startMiningCurrent();
            }
        }
    }

    /**
     * Looks for a persisted source block.
     */
    private static boolean findAndGoToKnownResource() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || currentTask == null) {
            return false;
        }

        String dimension = client.world.getRegistryKey().getValue().toString();
        Optional<WorldKnowledge.ResourceTarget> target = WorldKnowledge.findNearestResourceTarget(dimension, currentTask.item(), client.player.getBlockPos(), client.world);
        if (target.isEmpty()) {
            return false;
        }

        WorldKnowledge.ResourceTarget resource = target.get();
        currentResourceTarget = resource.position();
        currentResourceSource = resource.source();
        currentExplorationTarget = null;
        state = State.MOVING_TO_RESOURCE;
        BaritoneController.goTo(currentResourceTarget);
        return true;
    }

    private static boolean beginExploration() {
        Optional<BlockPos> target = findExplorationTarget();
        if (target.isEmpty()) {
            return false;
        }

        currentResourceTarget = null;
        currentResourceSource = null;
        currentExplorationTarget = target.get();
        state = State.EXPLORING;
        BaritoneController.goTo(currentExplorationTarget);
        return true;
    }

    private static Optional<BlockPos>
    findExplorationTarget() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || currentTask == null) {
            return Optional.empty();
        }

        String dimension = client.world.getRegistryKey().getValue().toString();
        return WorldKnowledge.findBestExplorationTarget(dimension, currentTask.item(), client.player.getBlockPos(), client.world);
    }

    private static void startMiningCurrent() {
        if (currentTask == null) {
            return;
        }

        state = State.MINING;
        currentExplorationTarget = null;
        /*
         * If we have a known source block, mine that block.
         *
         * Otherwise fall back to Baritone's item/block name.
         */
        String target = currentResourceSource != null && !currentResourceSource.isBlank() ? currentResourceSource : currentTask.item();
        BaritoneController.mine(target);
    }
}