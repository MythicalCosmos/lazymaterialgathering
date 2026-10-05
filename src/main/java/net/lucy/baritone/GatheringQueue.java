package net.lucy.baritone;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lucy.calc.GatheringPlanner;
import net.lucy.config.Configs;
import net.lucy.data.InventoryUtils;
import net.lucy.data.WorldKnowledge;
import net.lucy.model.GatheringTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import baritone.api.utils.BetterBlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Quantity-aware gathering state machine.
 *
 * The queue tracks the final inventory item, while each task carries
 * one or more physical source blocks that Baritone is allowed to mine.
 */
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
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private static State state = State.IDLE;
    private static State stateBeforePause;
    private static GatheringTask currentTask;
    private static int failedAttempts;
    private static BlockPos currentResourceTarget;
    private static BlockPos currentExplorationTarget;
    private static String currentResourceSource;

    private GatheringQueue() {}

    public static void start(java.util.Map<String, Long> materials) {
        start(GatheringPlanner.plan(materials));
    }

    public static void start(List<GatheringTask> tasks) {
        stop(false);
        WorldKnowledge.rescanLoadedArea(1);

        if (tasks != null) {
            tasks.stream()
                    .filter(task -> task != null && task.required() > 0)
                    .sorted(Comparator
                            .comparingLong(GatheringTask::required)
                            .reversed()
                            .thenComparing(GatheringTask::item))
                    .forEach(QUEUE::addLast);
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

    public static Optional<Double> getEstimatedSecondsRemaining() {
        return BaritoneController.get()
                .getPathingBehavior()
                .estimatedTicksToGoal()
                .map(ticks -> ticks / 20.0);
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
            case MOVING_TO_RESOURCE -> tickMovingToResource();
            case EXPLORING -> tickExploring();
            case MINING -> tickMining();
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
        if (client.player == null || client.world == null) {
            return;
        }

        if (currentResourceTarget == null) {
            if (!findAndGoToKnownResource()) {
                beginExploration();
            }
            return;
        }

        if (BaritoneController.isPathing()) {
            return;
        }

        double squaredDistance = client.player.getBlockPos().getSquaredDistance(currentResourceTarget);
        if (squaredDistance <= 25.0) {
            startMiningCurrent();
            return;
        }

        // The old target may have become stale while travelling.
        if (!findAndGoToKnownResource()) {
            beginExploration();
        }
    }

    private static void tickExploring() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        if (findAndGoToKnownResource()) {
            return;
        }

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

        // Do NOT fall back to unrestricted Baritone mining.
        // Without a known source, the queue waits for another scan.
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            System.out.println("[LMG] No known exploration/source target for " + currentTask.item() + ". Stopping this gathering run.");
            state = State.IDLE;
        }
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

        if (findAndGoToKnownResource()) {
            failedAttempts = 0;
            return;
        }

        failedAttempts++;
        if (failedAttempts < MAX_FAILED_ATTEMPTS && beginExploration()) {
            return;
        }

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            System.out.println("[LMG] Could not gather enough of " + currentTask.item() + " after " + MAX_FAILED_ATTEMPTS + " attempts.");
            state = State.IDLE;
        }
    }

    private static void goToDepositOrNext() {
        Optional<BetterBlockPos> deposit =
                Configs.Generic.RETURN_TO_DEPOSIT_BETWEEN_ITEMS.getBooleanValue()
                        ? DepositLocations.get()
                        : Optional.empty();

        if (deposit.isPresent()) {
            state = State.RETURNING_TO_DEPOSIT;
            BaritoneController.goTo(deposit.get());
        } else {
            startNextItem();
        }
    }

    private static void startNextItem() {
        while (true) {
            sortRemainingQueue();

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
                continue;
            }

            if (!findAndGoToKnownResource() && !beginExploration()) {
                startMiningCurrent();
            }
            return;
        }
    }

    /**
     * Chooses the closest known valid source among every source allowed
     * for the current task.
     */
    private static boolean findAndGoToKnownResource() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || currentTask == null) {
            return false;
        }

        List<String> sources = currentTask.sources();
        if (sources.isEmpty()) {
            return false;
        }

        String dimension = client.world.getRegistryKey().getValue().toString();
        Optional<WorldKnowledge.ResourceTarget> target =
                WorldKnowledge.findNearestResourceTarget(
                        dimension,
                        currentTask.item(),
                        sources,
                        client.player.getBlockPos(),
                        client.world
                );

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

    private static Optional<BlockPos> findExplorationTarget() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || currentTask == null) {
            return Optional.empty();
        }

        if (currentTask.sources().isEmpty()) {
            return Optional.empty();
        }

        String dimension = client.world.getRegistryKey().getValue().toString();
        return WorldKnowledge.findBestExplorationTarget(
                dimension,
                currentTask.item(),
                currentTask.sources(),
                client.player.getBlockPos(),
                client.world
        );
    }

    private static void startMiningCurrent() {
        if (currentTask == null || currentTask.required() <= 0) {
            startNextItem();
            return;
        }

        long stillNeeded = currentTask.stillNeeded(InventoryUtils.count(currentTask.item()));
        if (stillNeeded <= 0) {
            goToDepositOrNext();
            return;
        }

        String source = currentResourceSource;
        if (source == null || source.isBlank()) {
            source = currentTask.source();
        }

        // Critical safety rule: never give Baritone the final item merely
        // because it happens to have a same-named block. A source must have
        // been explicitly resolved by MiningSourceResolver.
        if (source == null || source.isBlank() || !currentTask.sources().contains(source)) {
            System.out.println("[LMG] Cannot mine " + currentTask.item() + ": no validated physical source is assigned.");
            state = State.IDLE;
            return;
        }

        state = State.MINING;
        currentExplorationTarget = null;
        currentResourceSource = source;
        System.out.println("[LMG] Mining " + source + " for " + stillNeeded + "x " + currentTask.item());
        BaritoneController.mine(source);
    }

    private static void sortRemainingQueue() {
        List<GatheringTask> tasks = new ArrayList<>(QUEUE);
        tasks.sort(Comparator
                .comparingLong((GatheringTask task) ->
                        -task.stillNeeded(InventoryUtils.count(task.item())))
                .thenComparing(GatheringTask::item));
        QUEUE.clear();
        QUEUE.addAll(tasks);
    }
}
