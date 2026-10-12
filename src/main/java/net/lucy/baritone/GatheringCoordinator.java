package net.lucy.baritone;

import baritone.api.pathing.calc.IPath;
import net.lucy.calc.GatheringPlanner;
import net.lucy.calc.MiningResolver;
import net.lucy.config.Configs;
import net.lucy.data.InventoryUtils;
import net.lucy.data.WorldKnowledge;
import net.lucy.model.GatheringTask;
import net.lucy.model.GatheringTaskState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Coordinates:
 *
 * Calculation
 *      ↓
 * GatheringTask
 *      ↓
 * WorldKnowledge
 *      ↓
 * Baritone
 *      ↓
 * inventory verification
 *
 * The queue never tells Baritone to mine an arbitrary item name.
 * Baritone is only given an explicitly validated physical source.
 */
public final class GatheringCoordinator {

    private static final Deque<GatheringTask> QUEUE =
            new ArrayDeque<>();

    /**
     * Resource locations that have already failed to produce the
     * requested inventory item during this task.
     */
    private static final Set<String> REJECTED_RESOURCE_TARGETS =
            new HashSet<>();

    private static GatheringTask currentTask;
    private static GatheringTaskState state =
            GatheringTaskState.IDLE;

    private static BlockPos currentResourceTarget;
    private static String currentResourceSource;
    private static BlockPos currentExplorationTarget;

    private static long miningStartInventory;
    private static int miningTicks;
    private static int failedAttempts;
    private static int blockedTicks;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int MINING_START_GRACE_TICKS = 10;

    private GatheringCoordinator() {
    }

    public static void register() {
        /*
         * GatheringQueue owns the actual Fabric tick registration.
         */
    }

    public static void start(
            Map<String, Long> rawMaterials
    ) {
        start(GatheringPlanner.plan(rawMaterials));
    }

    public static void start(
            List<GatheringTask> tasks
    ) {
        stop();

        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        /*
         * Largest required amount first.
         *
         * This is deliberately based on the desired inventory item,
         * not on the physical source block.
         */
        List<GatheringTask> ordered =
                new ArrayList<>(tasks);

        ordered.sort(
                Comparator.comparingLong(
                        GatheringTask::required
                ).reversed()
        );

        QUEUE.addAll(ordered);

        /*
         * Resource scanning is target-dependent. Refresh the active target
         * set before rescanning loaded chunks so a new plan can discover
         * materials that were not part of the previous calculation.
         */
        WorldKnowledge.setScanTargets(
                ordered.stream()
                        .map(GatheringTask::item)
                        .distinct()
                        .toList()
        );
        WorldKnowledge.rescanLoadedArea(8, true);

        failedAttempts = 0;
        blockedTicks = 0;

        state = GatheringTaskState.IDLE;

        selectNextTask();
    }

    public static void stop() {
        QUEUE.clear();
        REJECTED_RESOURCE_TARGETS.clear();

        currentTask = null;
        currentResourceTarget = null;
        currentResourceSource = null;
        currentExplorationTarget = null;

        miningStartInventory = 0L;
        miningTicks = 0;
        failedAttempts = 0;
        blockedTicks = 0;

        BaritoneController.cancelAll();

        state = GatheringTaskState.IDLE;
    }

    public static void pause() {
        if (!isRunning()) {
            return;
        }

        BaritoneController.pause();
        state = GatheringTaskState.PAUSED;
    }

    public static void resume() {
        if (state != GatheringTaskState.PAUSED) {
            return;
        }

        BaritoneController.resume();

        state = currentResourceTarget != null
                ? GatheringTaskState.MOVING_TO_RESOURCE
                : GatheringTaskState.EXPLORING;
    }

    public static boolean isRunning() {
        return state != GatheringTaskState.IDLE;
    }

    public static boolean isPaused() {
        return state == GatheringTaskState.PAUSED;
    }

    public static GatheringTaskState getState() {
        return state;
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
        if (currentTask == null) {
            return 0L;
        }

        return InventoryUtils.count(
                currentTask.item()
        );
    }

    public static long getCurrentStillNeeded() {
        if (currentTask == null) {
            return 0L;
        }

        return currentTask.stillNeeded(
                getCurrentInventory()
        );
    }

    public static int getRemainingCount() {
        return QUEUE.size()
                + (currentTask == null ? 0 : 1);
    }

    public static BlockPos getCurrentResourceTarget() {
        return currentResourceTarget;
    }

    public static String getCurrentResourceSource() {
        return currentResourceSource;
    }

    public static BlockPos getCurrentExplorationTarget() {
        return currentExplorationTarget;
    }

    public static Optional<Double>
    getEstimatedSecondsRemaining() {

        if (currentTask == null) {
            return Optional.empty();
        }

        long needed =
                currentTask.stillNeeded(
                        InventoryUtils.count(
                                currentTask.item()
                        )
                );

        if (needed <= 0L) {
            return Optional.of(0.0);
        }

        double seconds = 0.0;

        try {
            Optional<IPath> path =
                    BaritoneController.get()
                            .getPathingBehavior()
                            .getPath();

            if (path.isPresent()) {
                int positions =
                        path.get()
                                .positions()
                                .size();

                if (positions > 1) {
                    seconds +=
                            (positions - 1) * 0.25;
                }
            }
        } catch (RuntimeException ignored) {
        }

        if (currentResourceSource != null) {

            long yield =
                    MiningResolver.getExpectedDropCount(
                            currentResourceSource,
                            currentTask.item(),
                            Configs.Generic
                                    .USE_SILK_TOUCH
                                    .getBooleanValue(),
                            Configs.Generic
                                    .HAS_SHEARS
                                    .getBooleanValue()
                    );

            long blocks =
                    yield > 0L
                            ? (needed + yield - 1L) / yield
                            : needed;

            seconds += blocks * 1.5;

        } else {
            seconds += needed * 1.5;
        }

        return Optional.of(
                Math.max(0.0, seconds)
        );
    }

    public static void tick() {

        if (state == GatheringTaskState.IDLE
                || state == GatheringTaskState.PAUSED) {
            return;
        }

        if (currentTask == null) {
            selectNextTask();
            return;
        }

        switch (state) {

            case MOVING_TO_RESOURCE ->
                    tickMovingToResource();

            case EXPLORING ->
                    tickExploration();

            case MINING ->
                    tickMining();

            case VERIFYING ->
                    tickVerification();

            case RETURNING_TO_DEPOSIT ->
                    tickReturnToDeposit();

            case BLOCKED ->
                    tickBlocked();

            case COMPLETE ->
                    selectNextTask();

            default -> {
            }
        }
    }

    private static void selectNextTask() {

        currentResourceTarget = null;
        currentResourceSource = null;
        currentExplorationTarget = null;

        miningStartInventory = 0L;
        miningTicks = 0;

        REJECTED_RESOURCE_TARGETS.clear();

        if (QUEUE.isEmpty()) {
            currentTask = null;
            state = GatheringTaskState.IDLE;
            return;
        }

        currentTask = QUEUE.removeFirst();

        long inventory =
                InventoryUtils.count(
                        currentTask.item()
                );

        currentTask.setStartingInventory(
                inventory
        );

        if (currentTask.complete(inventory)) {
            state = GatheringTaskState.COMPLETE;
            return;
        }

        if (currentTask.sources().isEmpty()) {
            enterBlocked(
                    "No valid mining source for "
                            + currentTask.item()
            );
            return;
        }

        findTargetOrExplore();
    }

    private static void findTargetOrExplore() {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.world == null
                || client.player == null) {

            enterBlocked(
                    "Minecraft world/player unavailable."
            );

            return;
        }

        String dimension =
                client.world
                        .getRegistryKey()
                        .getValue()
                        .toString();

        Optional<WorldKnowledge.ResourceTarget>
                target =
                WorldKnowledge
                        .findNearestResourceTarget(
                                dimension,
                                currentTask.item(),
                                currentTask.sources(),
                                REJECTED_RESOURCE_TARGETS,
                                client.player
                                        .getBlockPos(),
                                client.world
                        );

        if (target.isPresent()) {

            WorldKnowledge.ResourceTarget
                    resolved = target.get();

            currentResourceTarget =
                    resolved.position();

            currentResourceSource =
                    resolved.source();

            BaritoneController.goTo(
                    currentResourceTarget
            );

            state =
                    GatheringTaskState
                            .MOVING_TO_RESOURCE;

            return;
        }

        if (!beginExploration()) {

            enterBlocked(
                    "No known source or exploration target for "
                            + currentTask.item()
            );
        }
    }

    private static void tickMovingToResource() {

        if (currentTask == null) {
            state = GatheringTaskState.IDLE;
            return;
        }

        long inventory =
                InventoryUtils.count(
                        currentTask.item()
                );

        if (currentTask.complete(inventory)) {
            finishCurrentTask();
            return;
        }

        if (currentResourceTarget == null
                || currentResourceSource == null) {

            findTargetOrExplore();
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null
                || client.world == null) {
            return;
        }

        /*
         * Revalidate the actual block before allowing Baritone
         * to mine it.
         */
        if (client.world.isChunkLoaded(
                currentResourceTarget.getX() >> 4,
                currentResourceTarget.getZ() >> 4
        )) {

            String actual =
                    net.minecraft.registry.Registries.BLOCK
                            .getId(
                                    client.world
                                            .getBlockState(
                                                    currentResourceTarget
                                            )
                                            .getBlock()
                            )
                            .toString();

            if (!currentTask.sources()
                    .contains(actual)) {

                rejectCurrentResourceTarget();

                WorldKnowledge.forgetResource(
                        client.world.getRegistryKey()
                                .getValue()
                                .toString(),
                        currentTask.item(),
                        currentResourceSource,
                        currentResourceTarget
                );

                currentResourceTarget = null;
                currentResourceSource = null;

                findTargetOrExplore();
                return;
            }
        }

        double distance =
                client.player
                        .getBlockPos()
                        .getSquaredDistance(
                                currentResourceTarget
                        );

        if (distance <= 25.0) {
            startMining();
        }
    }

    private static void startMining() {

        if (currentTask == null
                || currentResourceTarget == null
                || currentResourceSource == null) {

            enterBlocked(
                    "Mining attempted without validated source."
            );

            return;
        }

        if (!currentTask.sources()
                .contains(currentResourceSource)) {

            enterBlocked(
                    "Selected source is not valid for "
                            + currentTask.item()
            );

            return;
        }

        long needed =
                currentTask.stillNeeded(
                        InventoryUtils.count(
                                currentTask.item()
                        )
                );

        if (needed <= 0L) {
            finishCurrentTask();
            return;
        }

        miningStartInventory =
                InventoryUtils.count(
                        currentTask.item()
                );

        miningTicks = 0;

        /*
         * IMPORTANT:
         *
         * Baritone receives the PHYSICAL BLOCK.
         *
         * Example:
         *
         * desired item = minecraft:cobblestone
         * source block  = minecraft:stone
         *
         * Therefore:
         *
         * Baritone.mine("minecraft:stone")
         */
        BaritoneController.mine(
                currentResourceSource
        );

        state = GatheringTaskState.MINING;
    }

    private static void tickMining() {

        if (currentTask == null) {
            state = GatheringTaskState.IDLE;
            return;
        }

        long inventory =
                InventoryUtils.count(
                        currentTask.item()
                );

        if (currentTask.complete(inventory)) {

            BaritoneController.cancelMining();

            state =
                    GatheringTaskState.VERIFYING;

            return;
        }

        miningTicks++;

        if (miningTicks <
                MINING_START_GRACE_TICKS) {
            return;
        }

        if (!BaritoneController.isMining()) {

            long gained =
                    inventory -
                            miningStartInventory;

            if (gained > 0L) {

                /*
                 * The validated source produced
                 * the requested inventory item.
                 */
                currentResourceTarget = null;
                currentResourceSource = null;

                miningStartInventory =
                        inventory;

                miningTicks = 0;
                failedAttempts = 0;

                findTargetOrExplore();
                return;
            }

            /*
             * The physical source did not produce
             * the desired inventory item.
             *
             * Reject that exact resource location so
             * we do not repeatedly send Baritone back
             * to the same bad target.
             */
            rejectCurrentResourceTarget();

            failedAttempts++;

            currentResourceTarget = null;
            currentResourceSource = null;

            miningStartInventory =
                    inventory;

            miningTicks = 0;

            if (failedAttempts >=
                    MAX_FAILED_ATTEMPTS) {

                enterBlocked(
                        "Validated sources produced no "
                                + currentTask.item()
                                + " after "
                                + MAX_FAILED_ATTEMPTS
                                + " attempts."
                );

                return;
            }

            findTargetOrExplore();
        }
    }

    private static void tickVerification() {

        if (currentTask == null) {
            state = GatheringTaskState.IDLE;
            return;
        }

        long inventory =
                InventoryUtils.count(
                        currentTask.item()
                );

        if (!currentTask.complete(inventory)) {

            currentResourceTarget = null;
            currentResourceSource = null;

            findTargetOrExplore();
            return;
        }

        finishCurrentTask();
    }

    private static boolean beginExploration() {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.world == null
                || client.player == null
                || currentTask == null) {

            return false;
        }

        String dimension =
                client.world
                        .getRegistryKey()
                        .getValue()
                        .toString();

        Optional<BlockPos> exploration =
                WorldKnowledge
                        .findBestExplorationTarget(
                                dimension,
                                currentTask.item(),
                                currentTask.sources(),
                                client.player
                                        .getBlockPos(),
                                client.world
                        );

        if (exploration.isEmpty()) {
            return false;
        }

        currentExplorationTarget =
                exploration.get();

        BaritoneController.goTo(
                currentExplorationTarget
        );

        state =
                GatheringTaskState.EXPLORING;

        return true;
    }

    private static void tickExploration() {

        if (currentTask == null) {
            state = GatheringTaskState.IDLE;
            return;
        }

        long inventory =
                InventoryUtils.count(
                        currentTask.item()
                );

        if (currentTask.complete(inventory)) {
            finishCurrentTask();
            return;
        }

        if (currentExplorationTarget == null) {
            findTargetOrExplore();
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null) {
            return;
        }

        if (client.player
                .getBlockPos()
                .getSquaredDistance(
                        currentExplorationTarget
                ) <= 100.0) {

            WorldKnowledge.rescanLoadedArea(2);

            currentExplorationTarget = null;

            findTargetOrExplore();
        }
    }

    private static void tickReturnToDeposit() {

        if (currentTask == null) {
            state = GatheringTaskState.IDLE;
            return;
        }

        var deposit =
                DepositLocations.get();

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (deposit.isEmpty()
                || client.player == null) {

            finishCurrentTaskWithoutDeposit();
            return;
        }

        BlockPos target =
                deposit.get();

        if (client.player
                .getBlockPos()
                .getSquaredDistance(target)
                <= 9.0) {

            finishCurrentTaskWithoutDeposit();
            return;
        }

        if (!BaritoneController.isPathing()) {
            BaritoneController.goTo(target);
        }
    }

    private static void finishCurrentTask() {

        currentResourceTarget = null;
        currentResourceSource = null;
        currentExplorationTarget = null;

        if (Configs.Generic
                .RETURN_TO_DEPOSIT_BETWEEN_ITEMS
                .getBooleanValue()
                && !QUEUE.isEmpty()
                && DepositLocations.isSet()) {

            BaritoneController.cancelAll();

            state =
                    GatheringTaskState
                            .RETURNING_TO_DEPOSIT;

            return;
        }

        finishCurrentTaskWithoutDeposit();
    }

    private static void finishCurrentTaskWithoutDeposit() {

        currentResourceTarget = null;
        currentResourceSource = null;
        currentExplorationTarget = null;

        state =
                GatheringTaskState.COMPLETE;
    }

    private static void tickBlocked() {

        if (currentTask == null) {
            state = GatheringTaskState.IDLE;
            return;
        }

        blockedTicks++;

        if (blockedTicks < 20) {
            return;
        }

        blockedTicks = 0;

        if (findKnownSource()) {
            failedAttempts = 0;
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.world != null
                && client.player != null) {

            WorldKnowledge.rescanLoadedArea(1);
        }

        if (beginExploration()) {
            failedAttempts = 0;
            return;
        }

        failedAttempts++;

        if (failedAttempts >=
                MAX_FAILED_ATTEMPTS) {

            System.out.println(
                    "[LMG] Giving up on "
                            + currentTask.item()
                            + ": no validated source found."
            );

            currentTask = null;
            state = GatheringTaskState.IDLE;
        }
    }

    private static boolean findKnownSource() {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.world == null
                || client.player == null
                || currentTask == null) {

            return false;
        }

        String dimension =
                client.world
                        .getRegistryKey()
                        .getValue()
                        .toString();

        Optional<WorldKnowledge.ResourceTarget>
                target =
                WorldKnowledge
                        .findNearestResourceTarget(
                                dimension,
                                currentTask.item(),
                                currentTask.sources(),
                                REJECTED_RESOURCE_TARGETS,
                                client.player
                                        .getBlockPos(),
                                client.world
                        );

        if (target.isEmpty()) {
            return false;
        }

        currentResourceTarget =
                target.get().position();

        currentResourceSource =
                target.get().source();

        BaritoneController.goTo(
                currentResourceTarget
        );

        state =
                GatheringTaskState
                        .MOVING_TO_RESOURCE;

        return true;
    }

    private static void rejectCurrentResourceTarget() {

        if (currentTask == null
                || currentResourceTarget == null
                || currentResourceSource == null) {

            return;
        }

        REJECTED_RESOURCE_TARGETS.add(
                WorldKnowledge.resourceKey(
                        currentTask.item(),
                        currentResourceSource,
                        currentResourceTarget
                )
        );
    }

    private static void enterBlocked(
            String reason
    ) {

        System.out.println(
                "[LMG] BLOCKED: " + reason
        );

        BaritoneController.cancelAll();

        currentResourceTarget = null;
        currentResourceSource = null;
        currentExplorationTarget = null;

        miningStartInventory = 0L;
        miningTicks = 0;
        blockedTicks = 0;

        state =
                GatheringTaskState.BLOCKED;
    }
}