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
import java.util.Map;
import java.util.Optional;

/**
 * Controls quantity-aware gathering.
 *
 * Baritone remains responsible for actual movement and mining.
 *
 * GatheringQueue decides:
 *
 *  1. Do we already know where the resource is?
 *  2. If not, where should we explore?
 *  3. Did exploration discover the resource?
 *  4. Should we re-evaluate the target?
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
    private static State state = State.IDLE;
    private static State stateBeforePause;
    private static GatheringTask currentTask;
    private static int failedAttempts;
    private static BlockPos currentResourceTarget;
    private static BlockPos currentExplorationTarget;
    /*
     * Don't constantly abandon a perfectly good Baritone goal.
     */
    private static long lastTargetEvaluation;
    private static final long TARGET_REEVALUATION_MS = 3000L;
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private GatheringQueue() {
    }

    public static void start(Map<String, Long> materials) {
        start(GatheringPlanner.plan(materials));
    }

    public static void start(java.util.List<GatheringTask> tasks) {
        stop(false);
        WorldKnowledge.rescanLoadedArea(1);
        QUEUE.addAll(tasks);
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
        failedAttempts = 0;
        QUEUE.clear();
        if (cancelBaritone) {
            getBaritone().getPathingBehavior().cancelEverything();
            getBaritone().getMineProcess().cancel();
        }
    }

    public static void pause() {
        if (state == State.IDLE || state == State.PAUSED) {
            return;
        }

        stateBeforePause = state;
        state = State.PAUSED;
        getBaritone().getCommandManager().execute("pause");
    }

    public static void resume() {
        if (state != State.PAUSED) {
            return;
        }

        state = stateBeforePause == null ? State.MINING : stateBeforePause;
        stateBeforePause = null;
        getBaritone().getCommandManager().execute("resume");
    }

    public static boolean isRunning() {
        return state != State.IDLE;
    }

    public static boolean isPaused() {
        return state == State.PAUSED;
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

    public static boolean isExploring() {
        return state == State.EXPLORING;
    }

    public static BlockPos getCurrentResourceTarget() {
        return currentResourceTarget;
    }

    public static BlockPos getCurrentExplorationTarget() {
        return currentExplorationTarget;
    }

    public static Optional<Double>
    getEstimatedSecondsRemaining() {
        return getBaritone().getPathingBehavior().estimatedTicksToGoal().map(t -> t / 20.0);
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
            getBaritone().getMineProcess().cancel();
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
                if (!getBaritone().getPathingBehavior().isPathing()) {
                    startNextItem();
                }
            }
            default -> {
            }
        }
    }

    private static void tickMovingToResource() {
        /*
         * Once Baritone arrives, start mining.
         */
        if (!getBaritone().getPathingBehavior().isPathing()) {
            if (currentResourceTarget != null) {
                double distance = MinecraftClient.getInstance().player.getBlockPos().getSquaredDistance(currentResourceTarget);
                /*
                 * We don't require the player to stand on the exact
                 * block because Baritone may stop nearby.
                 */
                if (distance <= 16.0) {
                    startMiningCurrent();
                    return;
                }
            }

            /*
             * We arrived somewhere unexpected. Re-evaluate.
             */
            findAndGoToKnownResource();
        }
    }

    private static void tickExploring() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        /*
         * The scanner is working continuously as chunks load.
         *
         * Before doing anything else, see whether a real resource
         * location has appeared.
         */
        Optional<BlockPos> discovered = WorldKnowledge.findNearestResource(client.world.getRegistryKey().getValue().toString(), currentTask.item(), client.player.getBlockPos(), client.world);
        if (discovered.isPresent()) {
            currentExplorationTarget = null;
            currentResourceTarget = discovered.get();
            state = State.MOVING_TO_RESOURCE;
            getBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentResourceTarget));
            return;
        }

        /*
         * If we reached the exploration destination, rescan the
         * surrounding loaded area and choose another target.
         */
        if (!getBaritone().getPathingBehavior().isPathing()) {
            WorldKnowledge.rescanLoadedArea(2);
            Optional<BlockPos> next = findExplorationTarget();
            if (next.isPresent()) {
                currentExplorationTarget = next.get();
                getBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentExplorationTarget));
                return;
            }

            /*
             * We have no useful exploration target.
             * Use Baritone's normal mining process as the final
             * fallback.
             */
            startMiningCurrent();
        }
    }

    private static void tickMining() {
        /*
         * Re-check the inventory every tick.
         */
        long have = InventoryUtils.count(currentTask.item());
        if (currentTask.complete(have)) {
            goToDepositOrNext();
            return;
        }

        /*
         * Baritone stopping does not necessarily mean failure.
         * The mine process can finish because the currently visible
         * target blocks were exhausted.
         */
        if (!getBaritone().getMineProcess().isActive()) {
            failedAttempts++;
            /*
             * Before counting this as a failure, see if exploration
             * has discovered a new known source.
             */
            if (findAndGoToKnownResource()) {
                failedAttempts = 0;
                return;
            }

            /*
             * If we cannot find one, explore instead of repeatedly
             * issuing the exact same mine command.
             */
            if (failedAttempts < MAX_FAILED_ATTEMPTS) {
                if (beginExploration()) {
                    return;
                }
            }

            /*
             * Final fallback.
             */
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                System.out.println("[LMG] Could not gather enough of " + currentTask.item() + " after " + MAX_FAILED_ATTEMPTS + " attempts.");
                state = State.IDLE;
                return;
            }
            startMiningCurrent();
        }
    }

    private static void goToDepositOrNext() {
        Optional<BetterBlockPos> deposit = Configs.Generic.RETURN_TO_DEPOSIT_BETWEEN_ITEMS.getBooleanValue() ? DepositLocations.get() : Optional.empty();
        if (deposit.isPresent()) {
            state = State.RETURNING_TO_DEPOSIT;
            getBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(deposit.get()));
        } else {
            startNextItem();
        }
    }

    private static void startNextItem() {
        currentTask = QUEUE.pollFirst();
        failedAttempts = 0;
        currentResourceTarget = null;
        currentExplorationTarget = null;
        if (currentTask == null) {
            state = State.IDLE;
            return;
        }

        currentTask.setStartingInventory(InventoryUtils.count(currentTask.item()));
        if (currentTask.complete(currentTask.startingInventory())) {
            startNextItem();
            return;
        }
        findAndGoToKnownResource();
    }

    /**
     * Returns true if a known resource target was found and navigation
     * was started.
     */
    private static boolean findAndGoToKnownResource() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }

        String dimension = client.world.getRegistryKey().getValue().toString();
        Optional<BlockPos> target = WorldKnowledge.findNearestResource(dimension, currentTask.item(), client.player.getBlockPos(), client.world);
        if (target.isEmpty()) {
            return false;
        }

        currentResourceTarget = target.get();
        currentExplorationTarget = null;
        state = State.MOVING_TO_RESOURCE;
        lastTargetEvaluation = System.currentTimeMillis();
        getBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentResourceTarget));
        return true;
    }

    /**
     * Starts adaptive exploration.
     */
    private static boolean beginExploration() {
        Optional<BlockPos> target = findExplorationTarget();
        if (target.isEmpty()) {
            return false;
        }

        currentResourceTarget = null;
        currentExplorationTarget = target.get();
        state = State.EXPLORING;
        lastTargetEvaluation = System.currentTimeMillis();
        getBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentExplorationTarget));
        return true;
    }

    private static Optional<BlockPos>
    findExplorationTarget() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return Optional.empty();
        }

        String dimension = client.world.getRegistryKey().getValue().toString();
        return WorldKnowledge.findBestExplorationTarget(dimension, currentTask.item(), client.player.getBlockPos(), client.world);
    }

    private static void startMiningCurrent() {
        if (currentTask == null) {
            return;
        }

        currentResourceTarget = null;
        currentExplorationTarget = null;
        state = State.MINING;
        getBaritone().getMineProcess().mineByName(currentTask.item());
    }

    private static IBaritone getBaritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }
}