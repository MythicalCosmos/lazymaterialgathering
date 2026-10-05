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
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final Deque<GatheringTask> QUEUE =
            new ArrayDeque<>();

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

    /* ========================================================= */
    /* Starting / stopping                                       */
    /* ========================================================= */

    public static void start(Map<String, Long> materials) {
        start(GatheringPlanner.plan(materials));
    }

    public static void start(List<GatheringTask> tasks) {

        stop(false);

        /*
         * Make sure anything currently loaded is represented in
         * the persistent world knowledge database before we start.
         */
        WorldKnowledge.rescanLoadedArea(1);

        if (tasks != null) {

            tasks.stream()
                    .filter(task -> task != null)
                    .filter(task -> task.required() > 0)
                    .forEach(QUEUE::addLast);
        }

        /*
         * The first item is selected using remaining quantity,
         * not simply the order supplied by the planner.
         */
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

    /* ========================================================= */
    /* Pause / resume                                            */
    /* ========================================================= */

    public static void pause() {

        if (state == State.IDLE
                || state == State.PAUSED) {

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

        state =
                stateBeforePause == null
                        ? State.MINING
                        : stateBeforePause;

        stateBeforePause = null;

        BaritoneController.resume();
    }

    /* ========================================================= */
    /* Status                                                    */
    /* ========================================================= */

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
        return currentTask == null
                ? null
                : currentTask.item();
    }

    public static long getCurrentRequired() {
        return currentTask == null
                ? 0L
                : currentTask.required();
    }

    public static long getCurrentInventory() {
        return currentTask == null
                ? 0L
                : InventoryUtils.count(
                currentTask.item()
        );
    }

    public static long getCurrentStillNeeded() {

        return currentTask == null
                ? 0L
                : currentTask.stillNeeded(
                getCurrentInventory()
        );
    }

    public static int getRemainingCount() {

        return QUEUE.size()
                + (
                currentTask == null
                        || state == State.IDLE
                        ? 0
                        : 1
        );
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

        return BaritoneController
                .get()
                .getPathingBehavior()
                .estimatedTicksToGoal()
                .map(ticks -> ticks / 20.0);
    }

    /* ========================================================= */
    /* Registration / ticking                                    */
    /* ========================================================= */

    public static void register() {

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> tick()
        );
    }

    private static void tick() {

        if (state == State.IDLE
                || state == State.PAUSED
                || currentTask == null) {

            return;
        }

        long have =
                InventoryUtils.count(
                        currentTask.item()
                );

        /*
         * Always check the actual inventory before doing more
         * work. This means the queue adapts if the player already
         * has some of the required material.
         */
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

    /* ========================================================= */
    /* Moving to known resource                                  */
    /* ========================================================= */

    private static void tickMovingToResource() {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null) {
            return;
        }

        /*
         * Baritone is still traveling.
         */
        if (BaritoneController.isPathing()) {
            return;
        }

        /*
         * We lost our target.
         */
        if (currentResourceTarget == null) {

            if (!findAndGoToKnownResource()) {
                beginExploration();
            }

            return;
        }

        double squaredDistance =
                client.player
                        .getBlockPos()
                        .getSquaredDistance(
                                currentResourceTarget
                        );

        /*
         * We are close enough to start mining.
         */
        if (squaredDistance <= 25.0) {

            startMiningCurrent();

            return;
        }

        /*
         * Baritone may stop several blocks away from the
         * exact target depending on the path.
         *
         * If we aren't close enough, try the target again.
         */
        if (!findAndGoToKnownResource()) {

            beginExploration();
        }
    }

    /* ========================================================= */
    /* Exploration                                               */
    /* ========================================================= */

    private static void tickExploring() {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null
                || client.world == null) {

            return;
        }

        /*
         * Newly loaded chunks may have revealed the requested
         * resource.
         *
         * Always prefer a known source over blindly mining.
         */
        if (findAndGoToKnownResource()) {
            return;
        }

        /*
         * Keep feeding the persistent knowledge database while
         * traveling through unexplored terrain.
         */
        WorldKnowledge.rescanLoadedArea(2);

        if (BaritoneController.isPathing()) {
            return;
        }

        Optional<BlockPos> next =
                findExplorationTarget();

        if (next.isPresent()) {

            currentExplorationTarget =
                    next.get();

            BaritoneController.goTo(
                    currentExplorationTarget
            );

            return;
        }

        /*
         * IMPORTANT:
         *
         * We deliberately DO NOT fall back to an unrestricted
         * Baritone mining command here.
         *
         * If the planner requested a material and we cannot
         * currently find a source or useful exploration target,
         * we wait/fail rather than letting Baritone decide to
         * mine random blocks simply because they are mineable.
         */
        System.out.println(
                "[LMG] No exploration target found for "
                        + currentTask.item()
                        + ". Refusing to blindly mine."
        );

        failedAttempts++;

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {

            System.out.println(
                    "[LMG] Could not find a source for "
                            + currentTask.item()
                            + " after "
                            + MAX_FAILED_ATTEMPTS
                            + " attempts. Stopping."
            );

            state = State.IDLE;
        }
    }

    /* ========================================================= */
    /* Mining                                                    */
    /* ========================================================= */

    private static void tickMining() {

        long have =
                InventoryUtils.count(
                        currentTask.item()
                );

        /*
         * We got enough.
         */
        if (currentTask.complete(have)) {

            goToDepositOrNext();

            return;
        }

        /*
         * Baritone is currently mining the requested target.
         */
        if (BaritoneController.isMining()) {
            return;
        }

        /*
         * Mining stopped before we got enough.
         *
         * First check whether newly scanned chunks revealed a
         * known source.
         */
        if (findAndGoToKnownResource()) {

            failedAttempts = 0;

            return;
        }

        failedAttempts++;

        if (failedAttempts < MAX_FAILED_ATTEMPTS) {

            /*
             * Search for another useful exploration target.
             */
            if (beginExploration()) {
                return;
            }

            /*
             * IMPORTANT:
             *
             * Do NOT call startMiningCurrent() here.
             *
             * If we have no known source and no exploration
             * target, repeatedly telling Baritone to mine the
             * same item can result in it mining something merely
             * because it happens to be mineable.
             */
            System.out.println(
                    "[LMG] Mining stopped for "
                            + currentTask.item()
                            + ", but no known source or "
                            + "exploration target exists."
            );

            return;
        }

        System.out.println(
                "[LMG] Could not gather enough of "
                        + currentTask.item()
                        + " after "
                        + MAX_FAILED_ATTEMPTS
                        + " attempts."
        );

        state = State.IDLE;
    }

    /* ========================================================= */
    /* Deposit handling                                          */
    /* ========================================================= */

    private static void goToDepositOrNext() {

        Optional<BetterBlockPos> deposit =
                Configs.Generic
                        .RETURN_TO_DEPOSIT_BETWEEN_ITEMS
                        .getBooleanValue()
                        ? DepositLocations.get()
                        : Optional.empty();

        if (deposit.isPresent()) {

            state =
                    State.RETURNING_TO_DEPOSIT;

            BaritoneController.goTo(
                    deposit.get()
            );

        } else {

            startNextItem();
        }
    }

    /* ========================================================= */
    /* Queue prioritization                                      */
    /* ========================================================= */

    /**
     * Sorts all remaining gathering tasks by the amount that is
     * ACTUALLY still needed.
     *
     * Example:
     *
     * Stone      required 2400, inventory 0   -> 2400 needed
     * Spruce     required 800,  inventory 0   -> 800 needed
     * Iron       required 120,  inventory 50  -> 70 needed
     *
     * Result:
     *
     * Stone
     * Spruce
     * Iron
     *
     * If the player later obtains 2200 stone, the next sort gives:
     *
     * Spruce
     * Stone
     * Iron
     */
    private static void sortRemainingQueue() {
        List<GatheringTask> tasks = new ArrayList<>(QUEUE);

        tasks.sort(
                Comparator
                        .comparingLong(
                                (GatheringTask task) ->
                                        -task.stillNeeded(
                                                InventoryUtils.count(task.item())
                                        )
                        )
                        .thenComparing(GatheringTask::item)
        );

        QUEUE.clear();
        QUEUE.addAll(tasks);
    }

    /**
     * Selects the next highest-priority material.
     *
     * The largest REMAINING material requirement wins.
     *
     * We intentionally do this dynamically rather than trusting
     * the order returned by GatheringPlanner.
     */
    private static void startNextItem() {

        /*
         * Re-evaluate the queue before every new material.
         */
        sortRemainingQueue();

        currentTask =
                QUEUE.pollFirst();

        failedAttempts = 0;

        currentResourceTarget = null;

        currentExplorationTarget = null;

        currentResourceSource = null;

        if (currentTask == null) {

            state = State.IDLE;

            return;
        }

        long inventory =
                InventoryUtils.count(
                        currentTask.item()
                );

        currentTask.setStartingInventory(
                inventory
        );

        /*
         * We may already have enough due to gathering another
         * material, manual inventory changes, etc.
         */
        if (currentTask.complete(inventory)) {

            startNextItem();

            return;
        }

        /*
         * First choice:
         *
         * 1. Known resource source
         * 2. Intelligent exploration
         * 3. Stop rather than blindly mining
         */
        if (findAndGoToKnownResource()) {
            return;
        }

        if (beginExploration()) {
            return;
        }

        /*
         * There is no known source and no exploration target.
         *
         * We do NOT call Baritone.mineByName() blindly.
         */
        startMiningCurrent();
    }

    /* ========================================================= */
    /* Known resource lookup                                     */
    /* ========================================================= */

    /**
     * Looks for a persisted source block for the exact material
     * that the planner says we need.
     */
    private static boolean findAndGoToKnownResource() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null
                || client.world == null
                || currentTask == null) {
            return false;
        }

        String source = currentTask.source();

        if (source == null || source.isBlank()) {
            System.out.println(
                    "[LMG] No mining source assigned for "
                            + currentTask.item()
            );
            return false;
        }

        String dimension =
                client.world
                        .getRegistryKey()
                        .getValue()
                        .toString();

        Optional<WorldKnowledge.ResourceTarget> target =
                WorldKnowledge.findNearestResourceTarget(
                        dimension,
                        currentTask.item(),
                        source,
                        client.player.getBlockPos(),
                        client.world
                );

        if (target.isEmpty()) {
            return false;
        }

        WorldKnowledge.ResourceTarget resource =
                target.get();

        currentResourceTarget =
                resource.position();

        currentResourceSource =
                resource.source();

        currentExplorationTarget = null;

        state = State.MOVING_TO_RESOURCE;

        BaritoneController.goTo(
                currentResourceTarget
        );

        return true;
    }

    /* ========================================================= */
    /* Exploration target                                        */
    /* ========================================================= */

    private static boolean beginExploration() {

        Optional<BlockPos> target =
                findExplorationTarget();

        if (target.isEmpty()) {
            return false;
        }

        currentResourceTarget = null;

        currentResourceSource = null;

        currentExplorationTarget =
                target.get();

        state =
                State.EXPLORING;

        BaritoneController.goTo(
                currentExplorationTarget
        );

        return true;
    }

    private static Optional<BlockPos>
    findExplorationTarget() {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null
                || client.world == null
                || currentTask == null) {

            return Optional.empty();
        }

        String dimension =
                client.world
                        .getRegistryKey()
                        .getValue()
                        .toString();

        return WorldKnowledge.findBestExplorationTarget(
                dimension,
                currentTask.item(),
                client.player.getBlockPos(),
                client.world
        );
    }

    /* ========================================================= */
    /* Starting a mining operation                               */
    /* ========================================================= */

    /**
     * Starts Baritone mining for the CURRENT PLANNED MATERIAL.
     *
     * This method intentionally does not ask Baritone to decide
     * what is useful.
     *
     * The planner has already decided what material is required.
     * Baritone's job is only to obtain that material.
     */
    private static void startMiningCurrent() {
        if (currentTask == null) {
            return;
        }

        if (currentTask.required() <= 0) {
            startNextItem();
            return;
        }

        long stillNeeded =
                currentTask.stillNeeded(
                        InventoryUtils.count(
                                currentTask.item()
                        )
                );

        if (stillNeeded <= 0) {
            goToDepositOrNext();
            return;
        }

        String source = currentTask.source();

        if (source == null || source.isBlank()) {
            System.out.println(
                    "[LMG] Cannot mine "
                            + currentTask.item()
                            + ": no valid source was assigned."
            );

            state = State.IDLE;
            return;
        }

        state = State.MINING;
        currentExplorationTarget = null;
        currentResourceSource = source;

        System.out.println(
                "[LMG] Mining "
                        + source
                        + " for "
                        + stillNeeded
                        + "x "
                        + currentTask.item()
        );

        BaritoneController.mine(source);
    }
}