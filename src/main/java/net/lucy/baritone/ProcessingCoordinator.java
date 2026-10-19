package net.lucy.baritone;

import net.lucy.calc.ProcessingPlanner;
import net.lucy.data.CalculationData;
import net.lucy.data.InventoryUtils;
import net.lucy.data.Recipes;
import net.lucy.model.ProcessingPlan;
import net.lucy.model.ProcessingTask;
import net.lucy.model.ProcessingTaskState;
import net.lucy.model.RecipeType;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Map;

/**
 * Executes furnace-family processing plans.
 *
 * Supported processors:
 *  - Furnace
 *  - Blast Furnace
 *  - Smoker
 *
 * Processing is batch-aware. A ProcessingTask may require many outputs, so
 * the coordinator continues loading and processing batches until the target
 * output quantity is actually present in the player's inventory.
 *
 * Every interaction is verified against the actual inventory/container state
 * before the coordinator advances.
 */
public final class ProcessingCoordinator {

    private static final int PROCESSOR_RANGE = 8;
    private static final int PROCESSOR_SEARCH_RANGE = 12;

    /*
     * Four ticks gives the client enough time to receive the container state
     * after an inventory interaction before another interaction is attempted.
     */
    private static final long ACTION_DELAY_TICKS = 4L;

    /*
     * We deliberately process at most eight outputs per batch.
     *
     * One normal furnace fuel item such as coal processes eight items.
     * Keeping the batch at eight means the coordinator never assumes that a
     * single fuel item can process an entire inventory stack.
     */
    private static final long MAX_OUTPUTS_PER_BATCH = 8L;

    private static boolean running;
    private static boolean paused;

    private static ProcessingTaskState state =
            ProcessingTaskState.IDLE;

    private static ProcessingPlan plan;
    private static int taskIndex;

    private static long stateTicks;
    private static long actionTicks;

    /** Ticks spent outside the processor range without an active Baritone path. */
    private static int processorNavigationIdleTicks;
    private static int processorNavigationAttempts;
    private static int processorNavigationStallTicks;
    private static double lastProcessorNavigationDistance = Double.MAX_VALUE;

    private static final int NAVIGATION_IDLE_RETRY_TICKS = 40;
    private static final int MAX_PROCESSOR_NAVIGATION_ATTEMPTS = 5;

    private static String blockedReason = "";

    private static BlockPos processorPos;

    private static boolean processorPlacementStarted;

    /*
     * Total output quantity that this task must have when it finishes.
     */
    private static long targetOutput;

    /*
     * Inventory amount immediately before collecting a processor output batch.
     */
    private static long outputBefore;

    private ProcessingCoordinator() {
    }

    public static void register() {
        /*
         * Tick registration is owned by ProcessingQueue.
         */
    }

    public static void start(Map<String, Long> targets) {
        stop();

        Recipes.ensureLoaded();

        plan = ProcessingPlanner.plan(targets);

        if (plan.tasks().isEmpty()) {
            if (plan.missing().isEmpty()) {
                state = ProcessingTaskState.COMPLETE;
            } else {
                blockedReason = formatMissing(plan.missing());
                state = ProcessingTaskState.BLOCKED;
            }

            return;
        }

        taskIndex = 0;
        stateTicks = 0L;
        actionTicks = 0L;
        processorNavigationIdleTicks = 0;
        processorNavigationAttempts = 0;
        processorNavigationStallTicks = 0;
        lastProcessorNavigationDistance = Double.MAX_VALUE;

        blockedReason = "";

        processorPos = null;
        processorPlacementStarted = false;

        targetOutput = 0L;
        outputBefore = 0L;

        paused = false;
        running = true;

        state = ProcessingTaskState.FINDING_PROCESSOR;
    }

    public static void startFromCalculation() {
        start(CalculationData.getRawMaterials());
    }

    public static void stop() {
        running = false;
        paused = false;

        taskIndex = 0;

        stateTicks = 0L;
        actionTicks = 0L;
        processorNavigationIdleTicks = 0;
        processorNavigationAttempts = 0;

        blockedReason = "";

        processorPos = null;
        processorPlacementStarted = false;

        targetOutput = 0L;
        outputBefore = 0L;

        plan = null;

        state = ProcessingTaskState.IDLE;

        BaritoneController.cancelAll();
    }

    public static void pause() {
        if (!running || paused) {
            return;
        }

        paused = true;
        state = ProcessingTaskState.PAUSED;

        BaritoneController.cancelAll();
    }

    public static void resume() {
        if (!running || !paused) {
            return;
        }

        paused = false;

        if (getCurrentTask() == null) {
            finish();
            return;
        }

        state = ProcessingTaskState.FINDING_PROCESSOR;
        stateTicks = 0L;
        actionTicks = 0L;
    }

    public static boolean isRunning() {
        return running;
    }

    public static boolean isPaused() {
        return paused;
    }

    public static ProcessingTaskState getState() {
        return state;
    }

    public static ProcessingTask getCurrentTask() {
        if (plan == null) {
            return null;
        }

        if (taskIndex < 0
                || taskIndex >= plan.tasks().size()) {
            return null;
        }

        return plan.tasks().get(taskIndex);
    }

    public static int getRemainingTasks() {
        if (plan == null) {
            return 0;
        }

        return Math.max(
                0,
                plan.tasks().size() - taskIndex
        );
    }

    public static String getBlockedReason() {
        return blockedReason;
    }

    public static String getWaitingReason() {
        ProcessingTask task = getCurrentTask();
        if (running
                && !paused
                && state == ProcessingTaskState.COLLECTING
                && task != null
                && !InventoryUtils.hasSpaceFor(task.output())) {
            return "Inventory full. Make room for "
                    + task.output()
                    + " to collect processed output.";
        }

        return "";
    }

    /**
     * Returns the processor currently being used.
     */
    public static BlockPos getProcessorPos() {
        return processorPos;
    }

    /**
     * Returns the final output quantity required for the current processing
     * task.
     */
    public static long getTargetOutput() {
        return targetOutput;
    }

    /**
     * Returns how many outputs are still required for the current task.
     */
    public static long getOutputRemaining() {
        ProcessingTask task = getCurrentTask();

        if (task == null) {
            return 0L;
        }

        long target = targetOutput;

        if (target <= 0L) {
            target =
                    InventoryUtils.count(task.output())
                            + task.required();
        }

        return Math.max(
                0L,
                target - InventoryUtils.count(task.output())
        );
    }

    /**
     * Returns how many eight-output processing batches remain.
     */
    public static long getBatchesRemaining() {
        long remaining = getOutputRemaining();

        if (remaining <= 0L) {
            return 0L;
        }

        return (
                remaining
                        + MAX_OUTPUTS_PER_BATCH
                        - 1L
        ) / MAX_OUTPUTS_PER_BATCH;
    }

    public static void tick() {
        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client == null
                || client.player == null
                || client.world == null) {
            return;
        }

        if (!running || paused) {
            return;
        }

        // Do not treat an unrelated chest/player-inventory screen as a
        // furnace. Close it and reopen the intended processor instead of
        // accidentally transferring materials into arbitrary container slots.
        if (state != ProcessingTaskState.FINDING_PROCESSOR
                && state != ProcessingTaskState.OPENING_PROCESSOR
                && client.currentScreen instanceof HandledScreen<?> handled
                && !(handled.getScreenHandler() instanceof FurnaceScreenHandler)) {
            client.player.closeHandledScreen();
            state = ProcessingTaskState.OPENING_PROCESSOR;
            stateTicks = 0L;
            actionTicks = 0L;
            return;
        }

        stateTicks++;
        actionTicks++;

        switch (state) {
            case FINDING_PROCESSOR ->
                    tickFindingProcessor(client);

            case OPENING_PROCESSOR ->
                    tickOpeningProcessor(client);

            case LOADING_INPUT ->
                    tickLoadingInput(client);

            case LOADING_FUEL ->
                    tickLoadingFuel(client);

            case PROCESSING ->
                    tickProcessing(client);

            case COLLECTING ->
                    tickCollecting(client);

            case VERIFYING ->
                    tickVerifying(client);

            case COMPLETE,
                 BLOCKED,
                 PAUSED,
                 IDLE -> {
                // Terminal / externally controlled state.
            }
        }
    }

    private static void initializeTargetOutput(
            ProcessingTask task
    ) {
        if (targetOutput > 0L) {
            return;
        }

        long current =
                InventoryUtils.count(task.output());

        targetOutput =
                current
                        + task.required();
    }

    private static void tickFindingProcessor(
            MinecraftClient client
    ) {
        ProcessingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        initializeTargetOutput(task);

        /*
         * If the requested output is already present, do not interact with
         * a processor at all.
         */
        if (InventoryUtils.count(task.output())
                >= targetOutput) {

            advanceTask();
            return;
        }

        processorPos =
                findProcessor(
                        client,
                        task.type()
                );

        if (processorPos == null) {
            String processorItem = processorItem(task.type());

            if (WorkstationUtils.hasItem(client, processorItem)) {
                if (stateTicks % ACTION_DELAY_TICKS == 0L) {
                    if (WorkstationUtils.tryPlaceBlock(
                            client,
                            processorItem,
                            2
                    )) {
                        processorPlacementStarted = true;
                    }
                }

                if (stateTicks > 200L) {
                    block(
                            "Could not place a "
                                    + processorName(task.type())
                                    + " nearby. Make a clear solid surface and retry."
                    );
                }

                return;
            }

            if (processorPlacementStarted && stateTicks < 40L) {
                // Wait for the placement interaction to synchronize.
                return;
            }

            block(
                    processorPlacementStarted
                            ? "Processor placement was attempted but no "
                                + processorName(task.type())
                                + " appeared."
                            : "No "
                                + processorName(task.type())
                                + " found within "
                                + PROCESSOR_RANGE
                                + " blocks, and no matching workstation is in inventory."
            );

            return;
        }

        processorPlacementStarted = false;

        double distance =
                client.player.squaredDistanceTo(
                        processorPos.getX() + 0.5D,
                        processorPos.getY() + 0.5D,
                        processorPos.getZ() + 0.5D
                );

        if (distance
                > PROCESSOR_RANGE * PROCESSOR_RANGE) {

            lastProcessorNavigationDistance = distance;
            processorNavigationStallTicks = 0;
            BaritoneController.goTo(processorPos);

            state =
                    ProcessingTaskState.OPENING_PROCESSOR;

            stateTicks = 0L;

            return;
        }

        state =
                ProcessingTaskState.OPENING_PROCESSOR;

        stateTicks = 0L;
        actionTicks = 0L;
    }

    private static void tickOpeningProcessor(
            MinecraftClient client
    ) {
        ProcessingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        initializeTargetOutput(task);

        if (InventoryUtils.count(task.output())
                >= targetOutput) {

            advanceTask();
            return;
        }

        if (processorPos == null) {
            state =
                    ProcessingTaskState.FINDING_PROCESSOR;

            stateTicks = 0L;

            return;
        }

        double distance =
                client.player.squaredDistanceTo(
                        processorPos.getX() + 0.5D,
                        processorPos.getY() + 0.5D,
                        processorPos.getZ() + 0.5D
                );

        if (distance
                > PROCESSOR_RANGE * PROCESSOR_RANGE) {

            // Keep an active path intact instead of replacing it every tick.
            // If Baritone repeatedly stops outside interaction range, retry
            // a bounded number of times and then report an actionable error.
            if (BaritoneController.isPathing()) {
                processorNavigationIdleTicks = 0;
                if (distance < lastProcessorNavigationDistance - 0.25D) {
                    lastProcessorNavigationDistance = distance;
                    processorNavigationStallTicks = 0;
                    return;
                }

                processorNavigationStallTicks++;
                if (processorNavigationStallTicks < 200) {
                    return;
                }

                // A path can remain active without making progress. Cancel it
                // before retrying so the next goal actually replaces it.
                BaritoneController.cancelAll();
                processorNavigationStallTicks = 0;
                if (processorNavigationAttempts >= MAX_PROCESSOR_NAVIGATION_ATTEMPTS) {
                    block(
                            "Baritone made no progress reaching the "
                                    + processorName(task.type())
                                    + " after "
                                    + MAX_PROCESSOR_NAVIGATION_ATTEMPTS
                                    + " retries. Clear the route or move closer, then retry."
                    );
                    return;
                }
                processorNavigationAttempts++;
                lastProcessorNavigationDistance = distance;
                BaritoneController.goTo(processorPos);
                return;
            }

            processorNavigationStallTicks = 0;
            processorNavigationIdleTicks++;
            if (processorNavigationIdleTicks < NAVIGATION_IDLE_RETRY_TICKS) {
                return;
            }

            if (processorNavigationAttempts >= MAX_PROCESSOR_NAVIGATION_ATTEMPTS) {
                block(
                        "Could not reach the "
                                + processorName(getCurrentTask().type())
                                + " after "
                                + MAX_PROCESSOR_NAVIGATION_ATTEMPTS
                                + " navigation attempts. Move closer or clear the route, then retry."
                );
                return;
            }

            processorNavigationAttempts++;
            processorNavigationIdleTicks = 0;
            BaritoneController.goTo(processorPos);
            return;
        }

        processorNavigationIdleTicks = 0;
        processorNavigationAttempts = 0;
        processorNavigationStallTicks = 0;
        lastProcessorNavigationDistance = Double.MAX_VALUE;

        if (!(client.currentScreen
                instanceof HandledScreen<?>)) {

            if (client.interactionManager == null) {
                block(
                        "Minecraft interaction manager is unavailable."
                );

                return;
            }

            client.interactionManager.interactBlock(
                    client.player,
                    Hand.MAIN_HAND,
                    new BlockHitResult(
                            client.player.getPos(),
                            Direction.UP,
                            processorPos,
                            false
                    )
            );

            actionTicks = 0L;

            return;
        }

        state =
                ProcessingTaskState.LOADING_INPUT;

        stateTicks = 0L;
        actionTicks = 0L;
    }

    private static void tickLoadingInput(
            MinecraftClient client
    ) {
        ProcessingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        initializeTargetOutput(task);

        long outputHave =
                InventoryUtils.count(task.output());

        if (outputHave >= targetOutput) {
            advanceTask();
            return;
        }

        if (!(client.currentScreen
                instanceof HandledScreen<?> handled)) {

            state =
                    ProcessingTaskState.OPENING_PROCESSOR;

            stateTicks = 0L;

            return;
        }

        if (actionTicks < ACTION_DELAY_TICKS) {
            return;
        }

        ScreenHandler handler =
                handled.getScreenHandler();

        if (handler == null) {
            block(
                    "Processor screen handler is unavailable."
            );

            return;
        }

        int inputSlot =
                getInputSlot(handler);

        if (inputSlot < 0) {
            block(
                    "Unable to locate processor input slot."
            );

            return;
        }

        /*
         * If the processor still contains input from the previous batch,
         * do not put another batch into it.
         */
        ItemStack existingInput =
                handler
                        .getSlot(inputSlot)
                        .getStack();

        if (!existingInput.isEmpty()) {
            state =
                    ProcessingTaskState.LOADING_FUEL;

            stateTicks = 0L;
            actionTicks = 0L;

            return;
        }

        long outputRemaining =
                targetOutput - outputHave;

        long batchOutputs =
                Math.min(
                        MAX_OUTPUTS_PER_BATCH,
                        outputRemaining
                );

        long outputPerOperation = Math.max(1L, task.outputPerOperation());
        long operationsRequired =
                (batchOutputs + outputPerOperation - 1L) / outputPerOperation;
        long inputRequired =
                operationsRequired * Math.max(1L, task.inputPerOutput());

        long available =
                InventoryUtils.count(task.input());

        if (available < inputRequired) {
            block(
                    "Not enough "
                            + task.input()
                            + ". Need "
                            + inputRequired
                            + ", have "
                            + available
            );

            return;
        }

        if (!moveItemIntoSlot(
                client,
                handler,
                task.input(),
                inputSlot,
                inputRequired
        )) {
            block(
                    "Unable to move "
                            + inputRequired
                            + " "
                            + task.input()
                            + " into the processor."
            );

            return;
        }

        state =
                ProcessingTaskState.LOADING_FUEL;

        stateTicks = 0L;
        actionTicks = 0L;
    }

    private static void tickLoadingFuel(
            MinecraftClient client
    ) {
        ProcessingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        if (!(client.currentScreen
                instanceof HandledScreen<?> handled)) {

            state =
                    ProcessingTaskState.OPENING_PROCESSOR;

            stateTicks = 0L;

            return;
        }

        if (actionTicks < ACTION_DELAY_TICKS) {
            return;
        }

        ScreenHandler handler =
                handled.getScreenHandler();

        if (handler == null) {
            block(
                    "Processor screen handler is unavailable."
            );

            return;
        }

        int fuelSlot =
                getFuelSlot(handler);

        if (fuelSlot < 0) {
            block(
                    "Unable to locate processor fuel slot."
            );

            return;
        }

        ItemStack existingFuel =
                handler
                        .getSlot(fuelSlot)
                        .getStack();

        /*
         * If fuel is already present, let the processor use it.
         */
        if (!existingFuel.isEmpty()) {
            state =
                    ProcessingTaskState.PROCESSING;

            stateTicks = 0L;
            actionTicks = 0L;

            return;
        }

        String fuel =
                findFuelItem();

        if (fuel == null) {
            block(
                    "No furnace fuel was found in the inventory."
            );

            return;
        }

        if (!moveItemIntoSlot(
                client,
                handler,
                fuel,
                fuelSlot,
                1L
        )) {
            block(
                    "Unable to move furnace fuel into the processor."
            );

            return;
        }

        state =
                ProcessingTaskState.PROCESSING;

        stateTicks = 0L;
        actionTicks = 0L;
    }

    private static void tickProcessing(
            MinecraftClient client
    ) {
        ProcessingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        initializeTargetOutput(task);

        if (InventoryUtils.count(task.output())
                >= targetOutput) {

            advanceTask();
            return;
        }

        if (!(client.currentScreen
                instanceof HandledScreen<?> handled)) {

            state =
                    ProcessingTaskState.OPENING_PROCESSOR;

            stateTicks = 0L;

            return;
        }

        ScreenHandler handler =
                handled.getScreenHandler();

        if (handler == null) {
            block(
                    "Processor screen handler disappeared."
            );

            return;
        }

        int inputSlot =
                getInputSlot(handler);

        int fuelSlot =
                getFuelSlot(handler);

        int outputSlot =
                getOutputSlot(handler);

        if (inputSlot < 0
                || fuelSlot < 0
                || outputSlot < 0) {

            block(
                    "Unable to locate processor slots."
            );

            return;
        }

        ItemStack input =
                handler
                        .getSlot(inputSlot)
                        .getStack();

        ItemStack fuel =
                handler
                        .getSlot(fuelSlot)
                        .getStack();

        ItemStack output =
                handler
                        .getSlot(outputSlot)
                        .getStack();

        /*
         * A finished output is ready to be collected.
         */
        if (!output.isEmpty()) {
            state =
                    ProcessingTaskState.COLLECTING;

            stateTicks = 0L;
            actionTicks = 0L;

            return;
        }

        /*
         * If the input disappeared and there is no fuel, the current batch
         * finished but Minecraft has not yet produced an output update.
         * Give the container a little time to synchronize.
         */
        if (input.isEmpty()
                && fuel.isEmpty()) {

            if (stateTicks > 40L) {
                state =
                        ProcessingTaskState.LOADING_INPUT;

                stateTicks = 0L;
                actionTicks = 0L;
            }

            return;
        }

        /*
         * If the input is still present but fuel is gone, the batch may have
         * consumed its fuel. Load another fuel item rather than restarting
         * the entire task.
         */
        if (!input.isEmpty()
                && fuel.isEmpty()) {

            state =
                    ProcessingTaskState.LOADING_FUEL;

            stateTicks = 0L;
            actionTicks = 0L;

            return;
        }

        /*
         * Ten minutes is enough for an ordinary batch. This prevents a
         * broken container state from hanging the automation forever.
         */
        if (stateTicks > 20L * 60L * 10L) {
            block(
                    "Processor did not finish within the timeout."
            );
        }
    }

    private static void tickCollecting(
            MinecraftClient client
    ) {
        ProcessingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        initializeTargetOutput(task);

        if (!(client.currentScreen
                instanceof HandledScreen<?> handled)) {

            state =
                    ProcessingTaskState.OPENING_PROCESSOR;

            stateTicks = 0L;

            return;
        }

        if (actionTicks < ACTION_DELAY_TICKS) {
            return;
        }

        ScreenHandler handler =
                handled.getScreenHandler();

        if (handler == null) {
            block(
                    "Processor screen handler disappeared."
            );

            return;
        }

        int outputSlot =
                getOutputSlot(handler);

        if (outputSlot < 0) {
            block(
                    "Unable to locate processor output slot."
            );

            return;
        }

        ItemStack output =
                handler
                        .getSlot(outputSlot)
                        .getStack();

        if (output.isEmpty()) {
            state =
                    ProcessingTaskState.PROCESSING;

            actionTicks = 0L;

            return;
        }

        // Keep the furnace output in place until the player has room to
        // collect it. This avoids cursor-stack loss and false blocked states.
        if (!InventoryUtils.hasSpaceFor(task.output())) {
            return;
        }

        outputBefore =
                InventoryUtils.count(task.output());

        if (client.interactionManager == null) {
            block(
                    "Minecraft interaction manager is unavailable."
            );

            return;
        }

        client.interactionManager.clickSlot(
                handler.syncId,
                outputSlot,
                0,
                SlotActionType.QUICK_MOVE,
                client.player
        );

        state =
                ProcessingTaskState.VERIFYING;

        stateTicks = 0L;
        actionTicks = 0L;
    }

    private static void tickVerifying(
            MinecraftClient client
    ) {
        ProcessingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        long current =
                InventoryUtils.count(task.output());

        /*
         * The collection succeeded. Now determine whether the whole task is
         * complete or whether another batch is required.
         */
        if (current > outputBefore) {

            if (current >= targetOutput) {
                advanceTask();
                return;
            }

            /*
             * More output is still required.
             *
             * Return to the same processor rather than advancing the task.
             */
            state =
                    ProcessingTaskState.LOADING_INPUT;

            stateTicks = 0L;
            actionTicks = 0L;

            return;
        }

        if (stateTicks > 40L) {
            block(
                    "Processed output was not added to inventory: "
                            + task.output()
            );
        }
    }

    /**
     * Moves an exact quantity from the player's inventory into a processor
     * slot.
     *
     * When the source stack contains no more than the requested amount,
     * QUICK_MOVE is used.
     *
     * When the source stack is larger than requested, the coordinator picks
     * up the source stack, places the requested number one item at a time into
     * the destination, and returns the remaining cursor stack to the source.
     */
    private static boolean moveItemIntoSlot(
            MinecraftClient client,
            ScreenHandler handler,
            String itemId,
            int destinationSlot,
            long requestedCount
    ) {
        if (client.interactionManager == null
                || requestedCount <= 0L) {

            return false;
        }

        Item item =
                Registries.ITEM.get(
                        parseItem(itemId)
                );

        /*
         * Only inspect player inventory slots.
         *
         * Furnace slots occupy indices 0-2.
         */
        for (int slot = 3;
             slot < handler.slots.size();
             slot++) {

            ItemStack stack =
                    handler
                            .getSlot(slot)
                            .getStack();

            if (stack.isEmpty()
                    || !stack.isOf(item)) {
                continue;
            }

            long available =
                    stack.getCount();

            long amount =
                    Math.min(
                            requestedCount,
                            available
                    );

            /*
             * If the entire source stack is wanted, the normal quick-move
             * action is safer and much faster.
             */
            if (amount >= available) {

                client.interactionManager.clickSlot(
                        handler.syncId,
                        slot,
                        0,
                        SlotActionType.QUICK_MOVE,
                        client.player
                );

                return true;
            }

            /*
             * Pick up the complete source stack.
             */
            client.interactionManager.clickSlot(
                    handler.syncId,
                    slot,
                    0,
                    SlotActionType.PICKUP,
                    client.player
            );

            /*
             * Place exactly 'amount' items into the destination using
             * right-click placement.
             */
            for (long i = 0L;
                 i < amount;
                 i++) {

                client.interactionManager.clickSlot(
                        handler.syncId,
                        destinationSlot,
                        1,
                        SlotActionType.PICKUP,
                        client.player
                );
            }

            /*
             * Return the remaining cursor stack to the original inventory
             * slot.
             */
            client.interactionManager.clickSlot(
                    handler.syncId,
                    slot,
                    0,
                    SlotActionType.PICKUP,
                    client.player
            );

            return true;
        }

        return false;
    }

    private static String findFuelItem() {
        String[] fuelCandidates = {
                "minecraft:coal",
                "minecraft:charcoal",
                "minecraft:coal_block",
                "minecraft:oak_log",
                "minecraft:spruce_log",
                "minecraft:birch_log",
                "minecraft:jungle_log",
                "minecraft:acacia_log",
                "minecraft:dark_oak_log",
                "minecraft:mangrove_log",
                "minecraft:cherry_log",
                "minecraft:crimson_stem",
                "minecraft:warped_stem",
                "minecraft:oak_planks",
                "minecraft:spruce_planks",
                "minecraft:birch_planks",
                "minecraft:jungle_planks",
                "minecraft:acacia_planks",
                "minecraft:dark_oak_planks",
                "minecraft:mangrove_planks",
                "minecraft:cherry_planks",
                "minecraft:crimson_planks",
                "minecraft:warped_planks",
                "minecraft:bamboo_planks"
        };

        for (String candidate : fuelCandidates) {
            if (InventoryUtils.count(candidate) > 0L) {
                return candidate;
            }
        }

        return null;
    }

    public static boolean hasProcessorNearby(RecipeType type) {
        return findProcessor(
                MinecraftClient.getInstance(),
                type
        ) != null;
    }

    private static BlockPos findProcessor(
            MinecraftClient client,
            RecipeType type
    ) {
        if (client.world == null
                || client.player == null) {

            return null;
        }

        Block wanted;

        if (type == RecipeType.BLASTING) {
            wanted = Blocks.BLAST_FURNACE;
        } else if (type == RecipeType.SMOKING) {
            wanted = Blocks.SMOKER;
        } else {
            wanted = Blocks.FURNACE;
        }

        BlockPos origin =
                client.player.getBlockPos();

        BlockPos best = null;

        double bestDistance =
                Double.MAX_VALUE;

        for (int x = -PROCESSOR_SEARCH_RANGE;
             x <= PROCESSOR_SEARCH_RANGE;
             x++) {

            for (int y = -PROCESSOR_SEARCH_RANGE;
                 y <= PROCESSOR_SEARCH_RANGE;
                 y++) {

                for (int z = -PROCESSOR_SEARCH_RANGE;
                     z <= PROCESSOR_SEARCH_RANGE;
                     z++) {

                    BlockPos pos =
                            origin.add(x, y, z);

                    if (!client.world
                            .getBlockState(pos)
                            .isOf(wanted)) {

                        continue;
                    }

                    double distance =
                            client.player.squaredDistanceTo(
                                    pos.getX() + 0.5D,
                                    pos.getY() + 0.5D,
                                    pos.getZ() + 0.5D
                            );

                    if (distance
                            > PROCESSOR_SEARCH_RANGE
                            * PROCESSOR_SEARCH_RANGE) {

                        continue;
                    }

                    if (distance < bestDistance) {
                        best = pos;
                        bestDistance = distance;
                    }
                }
            }
        }

        return best;
    }

    private static int getInputSlot(
            ScreenHandler handler
    ) {
        if (handler instanceof FurnaceScreenHandler) {
            return 0;
        }

        return handler.slots.size() >= 3
                ? 0
                : -1;
    }

    private static int getFuelSlot(
            ScreenHandler handler
    ) {
        if (handler instanceof FurnaceScreenHandler) {
            return 1;
        }

        return handler.slots.size() >= 3
                ? 1
                : -1;
    }

    private static int getOutputSlot(
            ScreenHandler handler
    ) {
        if (handler instanceof FurnaceScreenHandler) {
            return 2;
        }

        return handler.slots.size() >= 3
                ? 2
                : -1;
    }

    private static Identifier parseItem(
            String id
    ) {
        try {
            return id.contains(":")
                    ? new Identifier(id)
                    : new Identifier(
                    "minecraft",
                    id
            );
        } catch (IllegalArgumentException e) {
            return new Identifier(
                    "minecraft",
                    "air"
            );
        }
    }

    private static String processorItem(
            RecipeType type
    ) {
        if (type == RecipeType.BLASTING) {
            return "minecraft:blast_furnace";
        }

        if (type == RecipeType.SMOKING) {
            return "minecraft:smoker";
        }

        return "minecraft:furnace";
    }

    private static String processorName(
            RecipeType type
    ) {
        if (type == RecipeType.BLASTING) {
            return "blast furnace";
        }

        if (type == RecipeType.SMOKING) {
            return "smoker";
        }

        return "furnace";
    }

    private static void advanceTask() {
        taskIndex++;

        stateTicks = 0L;
        actionTicks = 0L;
        processorNavigationIdleTicks = 0;
        processorNavigationAttempts = 0;

        processorPos = null;
        processorPlacementStarted = false;

        targetOutput = 0L;
        outputBefore = 0L;

        if (plan == null
                || taskIndex >= plan.tasks().size()) {

            finish();

            return;
        }

        state =
                ProcessingTaskState.FINDING_PROCESSOR;
    }

    private static void finish() {
        running = false;
        paused = false;

        state =
                ProcessingTaskState.COMPLETE;

        BaritoneController.cancelAll();
    }

    private static void block(
            String reason
    ) {
        running = false;
        paused = false;

        blockedReason =
                reason == null
                        ? "Unknown processing error."
                        : reason;

        state =
                ProcessingTaskState.BLOCKED;

        BaritoneController.cancelAll();
    }

    private static String formatMissing(
            Map<String, Long> missing
    ) {
        if (missing == null
                || missing.isEmpty()) {

            return "Processing plan could not be created.";
        }

        StringBuilder builder =
                new StringBuilder(
                        "Missing: "
                );

        boolean first = true;

        for (Map.Entry<String, Long> entry
                : missing.entrySet()) {

            if (!first) {
                builder.append(", ");
            }

            builder.append(entry.getKey())
                    .append(" x")
                    .append(entry.getValue());

            first = false;
        }

        return builder.toString();
    }
}