package net.lucy.baritone;

import net.lucy.calc.CraftingPlanner;
import net.lucy.data.CalculationData;
import net.lucy.data.InventoryUtils;
import net.lucy.data.Recipes;
import net.lucy.model.CraftingPlan;
import net.lucy.model.CraftingTask;
import net.lucy.model.CraftingTaskState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Map;

/**
 * Executes the generated crafting plan.
 *
 * The coordinator is deliberately inventory-driven.
 *
 * A CraftingTask describes the amount of an item that must exist after
 * crafting. The coordinator therefore does not assume that one click means
 * the task is complete.
 *
 * Example:
 *
 *     task:
 *         item = minecraft:stick
 *         required = 20
 *         outputPerCraft = 4
 *
 * The coordinator will continue crafting until the player's actual inventory
 * contains the task's target quantity.
 */
public final class CraftingCoordinator {

    private static final long ACTION_DELAY_TICKS = 4L;

    private static final double CRAFTING_TABLE_RANGE = 4.0D;

    /*
     * If the crafting result does not change for this many ticks after an
     * attempted craft, treat the operation as blocked.
     */
    private static final long VERIFICATION_TIMEOUT_TICKS = 40L;

    private static boolean running;
    private static boolean paused;

    private static CraftingTaskState state =
            CraftingTaskState.IDLE;

    private static CraftingPlan plan;
    private static int taskIndex;

    private static long stateTicks;
    private static long actionTicks;

    private static String blockedReason = "";

    /*
     * Inventory amount immediately before the most recent craft.
     */
    private static long verificationBefore;

    /*
     * Absolute inventory target for the current task.
     *
     * Example:
     *
     *     inventory before task = 3
     *     task.required()       = 20
     *     targetOutput          = 23
     *
     * This prevents the coordinator from accidentally treating the inventory
     * amount before the task as part of the newly crafted quantity.
     */
    private static long targetOutput;

    private CraftingCoordinator() {
    }

    public static void register() {
        /*
         * Tick registration is owned by CraftingQueue.
         */
    }

    public static void start(Map<String, Long> targets) {
        stop();

        Recipes.ensureLoaded();

        plan =
                CraftingPlanner.plan(targets);

        if (plan.tasks().isEmpty()) {
            if (plan.missing().isEmpty()) {
                state =
                        CraftingTaskState.COMPLETE;
            } else {
                blockedReason =
                        formatMissing(
                                plan.missing()
                        );

                state =
                        CraftingTaskState.BLOCKED;
            }

            return;
        }

        taskIndex = 0;

        stateTicks = 0L;
        actionTicks = 0L;

        verificationBefore = 0L;
        targetOutput = 0L;

        blockedReason = "";

        paused = false;
        running = true;

        state =
                CraftingTaskState.OPENING_INVENTORY;
    }

    /**
     * Starts crafting from the currently calculated schematic/material
     * requirements.
     *
     * CalculationData stores the currently calculated raw-material target
     * map, so this uses that same authoritative calculation input.
     */
    public static void startFromSchematic() {
        start(
                CalculationData.getRawMaterials()
        );
    }

    /**
     * Starts crafting from the current calculation.
     *
     * Kept as a separate API because other pipeline code already uses this
     * name.
     */
    public static void startFromCalculation() {
        start(
                CalculationData.getRawMaterials()
        );
    }

    public static void stop() {
        running = false;
        paused = false;

        taskIndex = 0;

        stateTicks = 0L;
        actionTicks = 0L;

        verificationBefore = 0L;
        targetOutput = 0L;

        blockedReason = "";

        plan = null;

        state =
                CraftingTaskState.IDLE;
    }

    public static void pause() {
        if (!running || paused) {
            return;
        }

        paused = true;

        state =
                CraftingTaskState.PAUSED;
    }

    public static void resume() {
        if (!running || !paused) {
            return;
        }

        paused = false;

        CraftingTask task =
                getCurrentTask();

        if (task == null) {
            running = false;
            state =
                    CraftingTaskState.COMPLETE;

            return;
        }

        /*
         * Recalculate the absolute target from the current inventory when
         * resuming. This avoids counting items that were obtained while the
         * automation was paused.
         */
        targetOutput =
                InventoryUtils.count(
                        task.item()
                ) + task.required();

        stateTicks = 0L;
        actionTicks = 0L;

        state =
                CraftingTaskState.OPENING_INVENTORY;
    }

    public static boolean isRunning() {
        return running;
    }

    public static boolean isPaused() {
        return paused;
    }

    public static CraftingTaskState getState() {
        return state;
    }

    public static CraftingTask getCurrentTask() {
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

    /**
     * Returns the absolute inventory target for the current task.
     */
    public static long getTargetOutput() {
        CraftingTask task =
                getCurrentTask();

        if (task == null) {
            return 0L;
        }

        ensureTargetOutput(task);

        return targetOutput;
    }

    /**
     * Returns how many output items are still needed for the current task.
     */
    public static long getOutputRemaining() {
        CraftingTask task =
                getCurrentTask();

        if (task == null) {
            return 0L;
        }

        ensureTargetOutput(task);

        long current =
                InventoryUtils.count(
                        task.item()
                );

        return Math.max(
                0L,
                targetOutput - current
        );
    }

    /**
     * Returns the number of crafts still required for the current task.
     *
     * This uses the recipe's actual output-per-craft value, so:
     *
     *     required = 20
     *     outputPerCraft = 4
     *
     * reports 5 crafts.
     */
    public static long getCraftsRemaining() {
        CraftingTask task =
                getCurrentTask();

        if (task == null) {
            return 0L;
        }

        long remaining =
                getOutputRemaining();

        if (remaining <= 0L) {
            return 0L;
        }

        long outputPerCraft =
                Math.max(
                        1L,
                        task.outputPerCraft()
                );

        return (
                remaining
                        + outputPerCraft
                        - 1L
        ) / outputPerCraft;
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

        stateTicks++;
        actionTicks++;

        switch (state) {

            case OPENING_INVENTORY ->
                    tickOpeningInventory(client);

            case WAITING_FOR_CRAFTING_TABLE ->
                    tickWaitingForCraftingTable(client);

            case FILLING_RECIPE ->
                    tickFillingRecipe(client);

            case CRAFTING ->
                    tickCrafting(client);

            case VERIFYING ->
                    tickVerifying(client);

            case COMPLETE,
                 BLOCKED,
                 PAUSED,
                 IDLE -> {
                // Terminal / externally controlled states.
            }
        }
    }

    /**
     * Establishes the absolute inventory target for the current task.
     */
    private static void ensureTargetOutput(
            CraftingTask task
    ) {
        if (targetOutput > 0L) {
            return;
        }

        long current =
                InventoryUtils.count(
                        task.item()
                );

        targetOutput =
                current
                        + task.required();
    }

    private static void tickOpeningInventory(
            MinecraftClient client
    ) {
        CraftingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        ensureTargetOutput(task);

        /*
         * Never craft if the player already has enough of the requested item.
         */
        if (InventoryUtils.count(task.item())
                >= targetOutput) {

            advanceTask();
            return;
        }

        if (task.requiresCraftingTable()) {

            BlockPos table =
                    findCraftingTable(client);

            if (table == null) {
                state =
                        CraftingTaskState.WAITING_FOR_CRAFTING_TABLE;

                stateTicks = 0L;

                return;
            }

            if (client.player.squaredDistanceTo(
                    table.getX() + 0.5D,
                    table.getY() + 0.5D,
                    table.getZ() + 0.5D
            ) > CRAFTING_TABLE_RANGE
                    * CRAFTING_TABLE_RANGE) {

                BaritoneController.goTo(table);

                state =
                        CraftingTaskState.WAITING_FOR_CRAFTING_TABLE;

                stateTicks = 0L;

                return;
            }

            if (!(client.currentScreen
                    instanceof HandledScreen<?>)) {

                interactWithBlock(
                        client,
                        table
                );

                stateTicks = 0L;

                return;
            }

            state =
                    CraftingTaskState.FILLING_RECIPE;

            stateTicks = 0L;
            actionTicks = 0L;

            return;
        }

        /*
         * A 2x2 recipe can be crafted directly in the player's inventory.
         */
        if (!(client.currentScreen
                instanceof HandledScreen<?>)) {

            client.setScreen(
                    new InventoryScreen(
                            client.player
                    )
            );

            stateTicks = 0L;

            return;
        }

        state =
                CraftingTaskState.FILLING_RECIPE;

        stateTicks = 0L;
        actionTicks = 0L;
    }

    private static void tickWaitingForCraftingTable(
            MinecraftClient client
    ) {
        CraftingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        ensureTargetOutput(task);

        if (InventoryUtils.count(task.item())
                >= targetOutput) {

            advanceTask();
            return;
        }

        BlockPos table =
                findCraftingTable(client);

        if (table == null) {
            block(
                    "No crafting table is available within "
                            + (int) CRAFTING_TABLE_RANGE
                            + " blocks."
            );

            return;
        }

        double distance =
                client.player.squaredDistanceTo(
                        table.getX() + 0.5D,
                        table.getY() + 0.5D,
                        table.getZ() + 0.5D
                );

        if (distance
                > CRAFTING_TABLE_RANGE
                * CRAFTING_TABLE_RANGE) {

            BaritoneController.goTo(table);

            return;
        }

        if (!(client.currentScreen
                instanceof HandledScreen<?>)) {

            interactWithBlock(
                    client,
                    table
            );

            stateTicks = 0L;

            return;
        }

        state =
                CraftingTaskState.FILLING_RECIPE;

        stateTicks = 0L;
        actionTicks = 0L;
    }

    /**
     * Uses Minecraft's recipe book to place the selected recipe into the
     * crafting grid.
     *
     * This method does NOT consider the task complete. It only prepares the
     * recipe. The actual output is collected and verified in the following
     * states.
     */
    private static void tickFillingRecipe(
            MinecraftClient client
    ) {
        CraftingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        ensureTargetOutput(task);

        if (InventoryUtils.count(task.item())
                >= targetOutput) {

            advanceTask();
            return;
        }

        if (!(client.currentScreen
                instanceof HandledScreen<?> handled)) {

            state =
                    CraftingTaskState.OPENING_INVENTORY;

            stateTicks = 0L;

            return;
        }

        ScreenHandler handler =
                handled.getScreenHandler();

        if (handler == null) {
            state =
                    CraftingTaskState.OPENING_INVENTORY;

            stateTicks = 0L;

            return;
        }

        if (actionTicks
                < ACTION_DELAY_TICKS) {

            return;
        }

        if (client.interactionManager == null) {
            block(
                    "Minecraft interaction manager is unavailable."
            );

            return;
        }

        net.minecraft.recipe.Recipe<?> recipe =
                findRecipe(
                        client,
                        task
                );

        if (recipe == null) {
            block(
                    "Recipe not found: "
                            + task.recipeId()
            );

            return;
        }

        /*
         * Yarn 1.20.1 uses Recipe<?> here, not RecipeEntry<?>.
         *
         * 'false' means we are asking Minecraft to place the recipe into the
         * crafting grid; the actual result is collected separately below.
         */
        client.interactionManager.clickRecipe(
                handler.syncId,
                recipe,
                false
        );

        verificationBefore =
                InventoryUtils.count(
                        task.item()
                );

        state =
                CraftingTaskState.CRAFTING;

        stateTicks = 0L;
        actionTicks = 0L;
    }

    /**
     * Takes exactly one currently displayed crafting-result stack.
     *
     * The important part is that we do NOT immediately advance the task.
     * After taking the result we enter VERIFYING and decide whether another
     * craft is required.
     */
    private static void tickCrafting(
            MinecraftClient client
    ) {
        CraftingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        ensureTargetOutput(task);

        long current =
                InventoryUtils.count(
                        task.item()
                );

        if (current >= targetOutput) {
            advanceTask();
            return;
        }

        if (!(client.currentScreen
                instanceof HandledScreen<?> handled)) {

            state =
                    CraftingTaskState.OPENING_INVENTORY;

            stateTicks = 0L;

            return;
        }

        ScreenHandler handler =
                handled.getScreenHandler();

        if (handler == null) {
            state =
                    CraftingTaskState.OPENING_INVENTORY;

            stateTicks = 0L;

            return;
        }

        if (actionTicks
                < ACTION_DELAY_TICKS) {

            return;
        }

        int resultSlot =
                findCraftingResultSlot(
                        handler
                );

        if (resultSlot < 0
                || resultSlot >= handler.slots.size()) {

            block(
                    "Unable to locate the crafting result slot."
            );

            return;
        }

        net.minecraft.item.ItemStack result =
                handler
                        .getSlot(resultSlot)
                        .getStack();

        if (result.isEmpty()) {

            /*
             * The recipe grid no longer has a craftable result.
             *
             * Re-fill the recipe. This also allows the coordinator to
             * recover if the recipe-book placement did not survive a
             * container synchronization.
             */
            state =
                    CraftingTaskState.FILLING_RECIPE;

            stateTicks = 0L;
            actionTicks = 0L;

            return;
        }

        if (client.interactionManager == null) {
            block(
                    "Minecraft interaction manager is unavailable."
            );

            return;
        }

        /*
         * QUICK_MOVE takes the currently displayed result into the player's
         * inventory. The crafting handler then consumes the ingredients for
         * that craft and updates the result slot.
         */
        verificationBefore =
                InventoryUtils.count(
                        task.item()
                );

        client.interactionManager.clickSlot(
                handler.syncId,
                resultSlot,
                0,
                SlotActionType.QUICK_MOVE,
                client.player
        );

        state =
                CraftingTaskState.VERIFYING;

        stateTicks = 0L;
        actionTicks = 0L;
    }

    /**
     * Verifies that the inventory actually increased.
     *
     * If the task still needs output, another craft is attempted rather than
     * incorrectly advancing to the next task.
     */
    private static void tickVerifying(
            MinecraftClient client
    ) {
        CraftingTask task =
                getCurrentTask();

        if (task == null) {
            finish();
            return;
        }

        ensureTargetOutput(task);

        long current =
                InventoryUtils.count(
                        task.item()
                );

        /*
         * The result arrived in the player's inventory.
         */
        if (current > verificationBefore) {

            /*
             * The entire task is now satisfied.
             */
            if (current >= targetOutput) {
                advanceTask();
                return;
            }

            /*
             * There is still more to craft.
             *
             * The recipe grid may already contain the next result. Return to
             * CRAFTING first; that avoids unnecessarily re-clicking the recipe
             * book every time.
             */
            state =
                    CraftingTaskState.CRAFTING;

            stateTicks = 0L;
            actionTicks = 0L;

            return;
        }

        /*
         * Give the client/server synchronization a short window.
         */
        if (stateTicks
                <= VERIFICATION_TIMEOUT_TICKS) {

            return;
        }

        /*
         * No inventory increase occurred.
         *
         * Before blocking permanently, check whether the recipe result is
         * still present. If it is, retry the craft.
         */
        if (client.currentScreen
                instanceof HandledScreen<?> handled) {

            ScreenHandler handler =
                    handled.getScreenHandler();

            if (handler != null) {

                int resultSlot =
                        findCraftingResultSlot(
                                handler
                        );

                if (resultSlot >= 0
                        && resultSlot
                        < handler.slots.size()) {

                    net.minecraft.item.ItemStack result =
                            handler
                                    .getSlot(resultSlot)
                                    .getStack();

                    if (!result.isEmpty()) {

                        state =
                                CraftingTaskState.CRAFTING;

                        stateTicks = 0L;
                        actionTicks = 0L;

                        return;
                    }
                }
            }
        }

        block(
                "Crafting result was not added to inventory: "
                        + task.item()
        );
    }

    private static void advanceTask() {
        taskIndex++;

        stateTicks = 0L;
        actionTicks = 0L;

        verificationBefore = 0L;
        targetOutput = 0L;

        if (plan == null
                || taskIndex >= plan.tasks().size()) {

            finish();

            return;
        }

        state =
                CraftingTaskState.OPENING_INVENTORY;
    }

    private static void finish() {
        running = false;
        paused = false;

        state =
                CraftingTaskState.COMPLETE;
    }

    private static void block(
            String reason
    ) {
        running = false;
        paused = false;

        blockedReason =
                reason == null
                        ? "Unknown crafting error."
                        : reason;

        state =
                CraftingTaskState.BLOCKED;

        BaritoneController.cancelAll();
    }

    private static BlockPos findCraftingTable(
            MinecraftClient client
    ) {
        if (client.world == null
                || client.player == null) {

            return null;
        }

        BlockPos origin =
                client.player.getBlockPos();

        int radius =
                (int) Math.ceil(
                        CRAFTING_TABLE_RANGE
                );

        BlockPos best = null;

        double bestDistance =
                Double.MAX_VALUE;

        for (int x = -radius;
             x <= radius;
             x++) {

            for (int y = -radius;
                 y <= radius;
                 y++) {

                for (int z = -radius;
                     z <= radius;
                     z++) {

                    BlockPos pos =
                            origin.add(
                                    x,
                                    y,
                                    z
                            );

                    if (!client.world
                            .getBlockState(pos)
                            .isOf(
                                    Blocks.CRAFTING_TABLE
                            )) {

                        continue;
                    }

                    double distance =
                            client.player.squaredDistanceTo(
                                    pos.getX() + 0.5D,
                                    pos.getY() + 0.5D,
                                    pos.getZ() + 0.5D
                            );

                    if (distance
                            > CRAFTING_TABLE_RANGE
                            * CRAFTING_TABLE_RANGE) {

                        continue;
                    }

                    if (distance < bestDistance) {
                        best =
                                pos;

                        bestDistance =
                                distance;
                    }
                }
            }
        }

        return best;
    }

    private static void interactWithBlock(
            MinecraftClient client,
            BlockPos pos
    ) {
        if (client.interactionManager == null
                || client.player == null) {

            return;
        }

        client.interactionManager.interactBlock(
                client.player,
                Hand.MAIN_HAND,
                new BlockHitResult(
                        client.player.getPos(),
                        Direction.UP,
                        pos,
                        false
                )
        );
    }

    private static int findCraftingResultSlot(
            ScreenHandler handler
    ) {
        if (handler
                instanceof AbstractRecipeScreenHandler<?>) {

            return (
                    (AbstractRecipeScreenHandler<?>)
                            handler
            ).getCraftingResultSlotIndex();
        }

        return -1;
    }

    private static net.minecraft.recipe.Recipe<?> findRecipe(
            MinecraftClient client,
            CraftingTask task
    ) {
        if (client.world == null
                || task.recipeId() == null
                || task.recipeId().isBlank()) {

            return null;
        }

        RecipeManager manager =
                client.world.getRecipeManager();

        Identifier id;

        try {
            id =
                    new Identifier(
                            task.recipeId()
                    );
        } catch (IllegalArgumentException e) {
            return null;
        }

        return manager
                .get(id)
                .orElse(null);
    }

    private static String formatMissing(
            Map<String, Long> missing
    ) {
        if (missing == null
                || missing.isEmpty()) {

            return "Crafting plan could not be created.";
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

            builder.append(
                            entry.getKey()
                    )
                    .append(" x")
                    .append(
                            entry.getValue()
                    );

            first = false;
        }

        return builder.toString();
    }
}