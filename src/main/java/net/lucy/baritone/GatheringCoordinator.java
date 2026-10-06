package net.lucy.baritone;

import net.lucy.calc.GatheringPlanner;
import net.lucy.data.InventoryUtils;
import net.lucy.data.WorldKnowledge;
import net.lucy.model.GatheringTask;
import net.lucy.model.GatheringTaskState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
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
     * Resource locations which failed to produce the requested
     * inventory item during this task.
     *
     * This prevents the coordinator from repeatedly selecting
     * the exact same bad source.
     */
    private static final Set<String> REJECTED_RESOURCE_TARGETS =
            new HashSet<>();

    private static GatheringTask currentTask;

    private static GatheringTaskState state =
            GatheringTaskState.IDLE;

    private static BlockPos currentResourceTarget;
    private static String currentResourceSource;
    private static BlockPos currentExplorationTarget;

    /**
     * Inventory count immediately before the current Baritone
     * mining operation began.
     */
    private static long miningStartInventory;

    private static int failedAttempts;
    private static int blockedTicks;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private GatheringCoordinator() {
    }

    public static void register() {
        /*
         * Called from the client tick handler.
         */
    }

    public static void start(
            Map<String, Long> rawMaterials
    ) {
        start(
                GatheringPlanner.plan(rawMaterials)
        );
    }

    public static void start(
            List<GatheringTask> tasks
    ) {
        stop();

        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        QUEUE.addAll(tasks);

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

        if (currentResourceTarget != null) {
            state = GatheringTaskState.MOVING_TO_RESOURCE;
        } else {
            state = GatheringTaskState.EXPLORING;
        }
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

        failedAttempts = 0;
        blockedTicks = 0;

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

        Optional<WorldKnowledge.ResourceTarget> target =
                WorldKnowledge.findNearestResourceTarget(
                        dimension,
                        currentTask.item(),
                        currentTask.sources(),
                        REJECTED_RESOURCE_TARGETS,
                        client.player.getBlockPos(),
                        client.world
                );

        if (target.isPresent()) {

            WorldKnowledge.ResourceTarget resolved =
                    target.get();

            currentResourceTarget =
                    resolved.position();

            currentResourceSource =
                    resolved.source();

            BaritoneController.goTo(
                    currentResourceTarget
            );

            state =
                    GatheringTaskState.MOVING_TO_RESOURCE;

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
            state = GatheringTaskState.COMPLETE;
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
         * Revalidate the physical source block once its chunk
         * is loaded.
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

            if (!actual.equals(
                    currentResourceSource
            )) {

                rejectCurrentResource(
                        "Source block changed."
                );

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

        /*
         * Close enough to begin the physical mining operation.
         */
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

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.world == null
                || client.player == null) {

            enterBlocked(
                    "Minecraft world/player unavailable."
            );

            return;
        }

        /*
         * Final physical validation immediately before mining.
         */
        if (!client.world.isChunkLoaded(
                currentResourceTarget.getX() >> 4,
                currentResourceTarget.getZ() >> 4
        )) {
            enterBlocked(
                    "Validated source chunk is no longer loaded."
            );
            return;
        }

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

        if (!actual.equals(currentResourceSource)) {

            rejectCurrentResource(
                    "Source failed final validation."
            );

            findTargetOrExplore();
            return;
        }

        long needed =
                currentTask.stillNeeded(
                        InventoryUtils.count(
                                currentTask.item()
                        )
                );

        if (needed <= 0L) {
            state = GatheringTaskState.COMPLETE;
            return;
        }

        /*
         * Record the inventory BEFORE Baritone starts.
         */
        miningStartInventory =
                InventoryUtils.count(
                        currentTask.item()
                );

        /*
         * Baritone gets the PHYSICAL BLOCK.
         *
         * Example:
         *
         * raw_iron -> iron_ore
         *
         * never:
         *
         * raw_iron -> raw_iron
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

            state = GatheringTaskState.VERIFYING;
            return;
        }

        /*
         * Baritone stopped before satisfying the task.
         */
        if (!BaritoneController.isMining()) {

            state = GatheringTaskState.VERIFYING;
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

        /*
         * Task is complete.
         */
        if (currentTask.complete(inventory)) {
            state = GatheringTaskState.COMPLETE;
            return;
        }

        /*
         * Determine whether this mining operation actually
         * produced the requested inventory item.
         */
        if (inventory <= miningStartInventory) {

            rejectCurrentResource(
                    "Mining source produced no additional "
                            + currentTask.item()
            );

            findTargetOrExplore();
            return;
        }

        /*
         * We made progress, but still need more.
         */
        currentResourceTarget = null;
        currentResourceSource = null;

        findTargetOrExplore();
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
                WorldKnowledge.findBestExplorationTarget(
                        dimension,
                        currentTask.item(),
                        currentTask.sources(),
                        client.player.getBlockPos(),
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
            state = GatheringTaskState.COMPLETE;
            return;
        }

        if (currentExplorationTarget == null) {
            findTargetOrExplore();
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.world == null
                || client.player == null) {
            return;
        }

        if (client.player.getBlockPos()
                .getSquaredDistance(
                        currentExplorationTarget
                ) <= 100.0) {

            WorldKnowledge.rescanLoadedArea(2);

            currentExplorationTarget = null;

            findTargetOrExplore();
        }
    }

    private static void tickReturnToDeposit() {

        /*
         * Deposit logic can be connected once the deposit
         * position is exposed by the UI/configuration.
         */
        state = GatheringTaskState.COMPLETE;
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

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {

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

        Optional<WorldKnowledge.ResourceTarget> target =
                WorldKnowledge.findNearestResourceTarget(
                        dimension,
                        currentTask.item(),
                        currentTask.sources(),
                        REJECTED_RESOURCE_TARGETS,
                        client.player.getBlockPos(),
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
                GatheringTaskState.MOVING_TO_RESOURCE;

        return true;
    }

    private static void rejectCurrentResource(
            String reason
    ) {

        if (currentTask == null
                || currentResourceTarget == null
                || currentResourceSource == null) {
            return;
        }

        String key =
                resourceTargetKey(
                        currentTask.item(),
                        currentResourceSource,
                        currentResourceTarget
                );

        REJECTED_RESOURCE_TARGETS.add(key);

        System.out.println(
                "[LMG] Rejecting resource "
                        + key
                        + ": "
                        + reason
        );

        currentResourceTarget = null;
        currentResourceSource = null;
    }

    private static String resourceTargetKey(
            String item,
            String source,
            BlockPos position
    ) {

        return item
                + "|"
                + source
                + "|"
                + position.getX()
                + ","
                + position.getY()
                + ","
                + position.getZ();
    }

    private static void enterBlocked(
            String reason
    ) {

        System.out.println(
                "[LMG] BLOCKED: "
                        + reason
        );

        BaritoneController.cancelAll();

        currentResourceTarget = null;
        currentResourceSource = null;
        currentExplorationTarget = null;

        blockedTicks = 0;

        state =
                GatheringTaskState.BLOCKED;
    }
}