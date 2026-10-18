package net.lucy.baritone;

import net.lucy.calc.CraftingPlanner;
import net.lucy.calc.GatheringPlanner;
import net.lucy.calc.MiningSourceResolver;
import net.lucy.calc.ProcessingPlanner;
import net.lucy.data.CalculationData;
import net.lucy.data.InventoryUtils;
import net.lucy.model.CraftingPlan;
import net.lucy.model.CraftingTask;
import net.lucy.model.CraftingTaskState;
import net.lucy.model.GatheringTask;
import net.lucy.model.ProcessingPlan;
import net.lucy.model.ProcessingTask;
import net.lucy.model.RecipeType;
import net.minecraft.client.MinecraftClient;
import net.lucy.model.ProcessingTaskState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coordinates the complete material pipeline:
 *
 *     gather -> process -> craft -> verify
 *
 * The pipeline is inventory-driven.
 *
 * Every stage is re-planned after the previous stage changes the
 * inventory. This allows the pipeline to recover from:
 *
 * - partial gathering
 * - partial processing
 * - partial crafting
 * - manually added materials
 * - inventory changes caused by another system
 *
 * The pipeline never assumes that a queue completing means that
 * the final inventory requirement has been satisfied.
 */
public final class MaterialPipelineCoordinator {

    private static final Set<String> REJECTED_GATHERING_ITEMS = new HashSet<>();
    private static final Map<String, Long> FUEL_TARGETS = new LinkedHashMap<>();
    private static String fuelPlanningFailure = "";
    private static int fuelRecoveryAttempts = 0;

    private static MaterialPipelineState state =
            MaterialPipelineState.IDLE;

    private static MaterialPipelineState stateBeforePause =
            MaterialPipelineState.IDLE;

    /**
     * The actual final inventory targets requested by the user.
     *
     * These are NOT replaced with intermediate raw materials.
     */
    private static Map<String, Long> targets =
            new LinkedHashMap<>();

    private static String blockedReason = "";

    private static boolean registered = false;

    private MaterialPipelineCoordinator() {
    }

    /* ========================================================= */
    /* Registration                                               */
    /* ========================================================= */

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
    }

    /* ========================================================= */
    /* Lifecycle                                                  */
    /* ========================================================= */

    public static void start(
            Map<String, Long> requestedTargets
    ) {
        stop();

        if (requestedTargets == null
                || requestedTargets.isEmpty()) {

            state = MaterialPipelineState.COMPLETE;
            return;
        }

        targets = new LinkedHashMap<>();

        for (Map.Entry<String, Long> entry
                : requestedTargets.entrySet()) {

            if (entry.getKey() == null
                    || entry.getKey().isBlank()) {

                continue;
            }

            long amount =
                    Math.max(
                            0L,
                            entry.getValue()
                    );

            if (amount <= 0L) {
                continue;
            }

            targets.put(
                    normalize(entry.getKey()),
                    amount
            );
        }

        if (targets.isEmpty()) {
            state = MaterialPipelineState.COMPLETE;
            return;
        }

        blockedReason = "";

        /*
         * Before doing anything, check whether the player already
         * possesses the requested final items.
         */
        if (allTargetsSatisfied()) {
            state = MaterialPipelineState.COMPLETE;
            return;
        }

        beginGathering();
    }

    /**
     * Starts the full gather -> process -> craft pipeline for the actual
     * schematic block requirements. Raw materials are used by the
     * gather-only screen; using them here would lose the final outputs and
     * prevent the pipeline from processing/crafting them.
     */
    public static void startFromCalculation() {
        start(
                CalculationData.getBlockCounts()
        );
    }

    public static void stop() {
        REJECTED_GATHERING_ITEMS.clear();
        FUEL_TARGETS.clear();
        fuelPlanningFailure = "";
        fuelRecoveryAttempts = 0;
        GatheringQueue.stop();
        ProcessingQueue.stop();
        CraftingQueue.stop();

        state =
                MaterialPipelineState.IDLE;

        stateBeforePause =
                MaterialPipelineState.IDLE;

        targets =
                new LinkedHashMap<>();

        blockedReason = "";
    }

    public static void pause() {
        if (!isRunning()) {
            return;
        }

        stateBeforePause = state;

        GatheringQueue.pause();
        ProcessingQueue.pause();
        CraftingQueue.pause();

        state =
                MaterialPipelineState.PAUSED;
    }

    public static void resume() {
        if (state != MaterialPipelineState.PAUSED) {
            return;
        }

        switch (stateBeforePause) {

            case GATHERING:
            case VERIFYING_GATHERING:
                GatheringQueue.resume();
                break;

            case PROCESSING:
            case VERIFYING_PROCESSING:
                ProcessingQueue.resume();
                break;

            case CRAFTING:
                CraftingQueue.resume();
                break;

            default:
                break;
        }

        state = stateBeforePause;
    }

    /* ========================================================= */
    /* Status                                                     */
    /* ========================================================= */

    public static boolean isRunning() {
        return state != MaterialPipelineState.IDLE
                && state != MaterialPipelineState.COMPLETE
                && state != MaterialPipelineState.BLOCKED;
    }

    public static boolean isPaused() {
        return state == MaterialPipelineState.PAUSED;
    }

    public static MaterialPipelineState getState() {
        return state;
    }

    public static String getBlockedReason() {
        return blockedReason;
    }

    public static Map<String, Long> getTargets() {
        return Map.copyOf(targets);
    }

    public static String getCurrentItem() {

        switch (state) {

            case GATHERING:
            case VERIFYING_GATHERING:
                return GatheringQueue.getCurrentItem();

            case PROCESSING:
            case VERIFYING_PROCESSING:

                if (ProcessingQueue.getCurrentTask() == null) {
                    return null;
                }

                return ProcessingQueue
                        .getCurrentTask()
                        .output();

            case CRAFTING:

                if (CraftingQueue.getCurrentTask() == null) {
                    return null;
                }

                return CraftingQueue
                        .getCurrentTask()
                        .item();

            default:
                return null;
        }
    }

    public static int getRemainingTasks() {

        return switch (state) {

            case GATHERING,
                 VERIFYING_GATHERING ->
                    GatheringQueue.getRemainingCount();

            case PROCESSING,
                 VERIFYING_PROCESSING ->
                    ProcessingQueue.getRemainingTasks();

            case CRAFTING ->
                    CraftingQueue.getRemainingTasks();

            default ->
                    0;
        };
    }

    /* ========================================================= */
    /* Main tick                                                  */
    /* ========================================================= */

    public static void tick() {

        switch (state) {

            case IDLE:
            case COMPLETE:
            case BLOCKED:
            case PAUSED:
                return;

            case GATHERING:
                tickGathering();
                return;

            case VERIFYING_GATHERING:
                tickVerifyingGathering();
                return;

            case PROCESSING:
                tickProcessing();
                return;

            case VERIFYING_PROCESSING:
                tickVerifyingProcessing();
                return;

            case CRAFTING:
                tickCrafting();
                return;
        }
    }

    /* ========================================================= */
    /* Gathering                                                  */
    /* ========================================================= */

    private static void beginGathering() {

        Map<String, Long> gatheringTargets =
                buildGatheringTargets();

        if (gatheringTargets.isEmpty()) {

            state =
                    MaterialPipelineState.VERIFYING_GATHERING;

            return;
        }

        List<GatheringTask> tasks =
                GatheringPlanner.plan(
                        gatheringTargets
                );

        if (tasks.isEmpty()) {

            state =
                    MaterialPipelineState.VERIFYING_GATHERING;

            return;
        }

        GatheringQueue.start(tasks);

        if (GatheringQueue.isRunning()) {

            state =
                    MaterialPipelineState.GATHERING;

        } else {

            state =
                    MaterialPipelineState.VERIFYING_GATHERING;
        }
    }

    /**
     * Adds furnace-family outputs that are missing as intermediate crafting
     * ingredients to the processing target map. CraftingPlanner reports
     * deficits, while ProcessingPlanner expects absolute inventory targets.
     */
    private static Map<String, Long> buildProcessingTargets() {
        Map<String, Long> result = new LinkedHashMap<>();

        // Only send targets to processing when direct gathering is not a
        // viable plan, or when gathering has already failed for that item.
        for (Map.Entry<String, Long> entry : targets.entrySet()) {
            String item = normalize(entry.getKey());
            if (item.isBlank()
                    || entry.getValue() <= InventoryUtils.count(item)
                    || !ProcessingPlanner.hasProcessingRecipe(item)) {
                continue;
            }
            if (!REJECTED_GATHERING_ITEMS.contains(item)
                    && MiningSourceResolver.hasMiningSource(item)) {
                continue;
            }
            result.put(item, entry.getValue());
        }

        CraftingPlan craftingPlan = CraftingPlanner.plan(targets, REJECTED_GATHERING_ITEMS);

        for (Map.Entry<String, Long> entry : craftingPlan.missing().entrySet()) {
            String item = normalize(entry.getKey());
            long missing = Math.max(0L, entry.getValue());

            if (item.isBlank()
                    || missing <= 0L
                    || (!REJECTED_GATHERING_ITEMS.contains(item)
                        && MiningSourceResolver.hasMiningSource(item))
                    || !ProcessingPlanner.hasProcessingRecipe(item)) {
                continue;
            }

            long have = InventoryUtils.count(item);
            long requiredTotal = missing > Long.MAX_VALUE - have
                    ? Long.MAX_VALUE
                    : have + missing;

            // If the same item is also a final target, the larger absolute
            // target is the correct total requirement; don't double-count it.
            result.merge(item, requiredTotal, Math::max);
        }

        /*
         * Recursively expose intermediate processing outputs. For example,
         * smooth_stone needs stone, and stone itself may need to be smelted
         * from cobblestone. Add such intermediate outputs to the processing
         * target set until no new dependency can be discovered.
         */
        for (int pass = 0; pass < 32; pass++) {
            ProcessingPlan processingPlan =
                    ProcessingPlanner.plan(result, REJECTED_GATHERING_ITEMS);

            boolean changed = false;

            for (Map.Entry<String, Long> entry
                    : processingPlan.missing().entrySet()) {
                String item = normalize(entry.getKey());
                long missing = Math.max(0L, entry.getValue());

                if (item.isBlank()
                        || missing <= 0L
                        || (!REJECTED_GATHERING_ITEMS.contains(item)
                            && MiningSourceResolver.hasMiningSource(item))
                        || !ProcessingPlanner.hasProcessingRecipe(item)) {
                    continue;
                }

                long have = InventoryUtils.count(item);
                long requiredTotal = missing > Long.MAX_VALUE - have
                        ? Long.MAX_VALUE
                        : have + missing;
                long existing = result.getOrDefault(item, 0L);

                if (requiredTotal > existing) {
                    result.put(item, requiredTotal);
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }
        }

        return result;
    }

    /**
     * Adds workstation items as intermediate crafting targets when the
     * pipeline needs them but neither a nearby block nor an inventory item
     * exists. The coordinator can then craft the workstation and place it.
     */
    private static Map<String, Long> buildCraftingTargets() {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<String, Long> workstationTargets = new LinkedHashMap<>();

        Map<String, Long> processingTargets = buildProcessingTargets();
        for (Map.Entry<String, Long> entry : processingTargets.entrySet()) {
            String item = normalize(entry.getKey());
            long required = Math.max(0L, entry.getValue());

            if (item.isBlank()
                    || required <= InventoryUtils.count(item)
                    || !ProcessingPlanner.hasProcessingRecipe(item)) {
                continue;
            }

            RecipeType type = ProcessingPlanner.getProcessingRecipeType(item, REJECTED_GATHERING_ITEMS);
            if (type == null) {
                continue;
            }

            String processorItem = processorItem(type);
            if (!ProcessingCoordinator.hasProcessorNearby(type)
                    && !WorkstationUtils.hasItem(client, processorItem)) {
                workstationTargets.merge(processorItem, 1L, Math::max);
            }
        }

        /*
         * Put workstation prerequisites before final targets. CraftingPlanner
         * respects map order for independent tasks, so the table must be
         * crafted before a furnace or final 3x3 recipe can start.
         */
        Map<String, Long> plannedTargets = new LinkedHashMap<>(workstationTargets);
        plannedTargets.putAll(targets);

        CraftingPlan craftingPlan = CraftingPlanner.plan(plannedTargets, REJECTED_GATHERING_ITEMS);
        boolean needsCraftingTable = craftingPlan.tasks().stream()
                .anyMatch(CraftingTask::requiresCraftingTable);

        Map<String, Long> result = new LinkedHashMap<>();
        if (needsCraftingTable
                && !CraftingCoordinator.hasCraftingTableNearby()
                && !WorkstationUtils.hasItem(client, "minecraft:crafting_table")) {
            result.put("minecraft:crafting_table", 1L);
        }

        result.putAll(workstationTargets);
        result.putAll(targets);
        return result;
    }

    private static String processorItem(RecipeType type) {
        if (type == RecipeType.BLASTING) {
            return "minecraft:blast_furnace";
        }
        if (type == RecipeType.SMOKING) {
            return "minecraft:smoker";
        }
        return "minecraft:furnace";
    }

    /**
     * Ensures required processors exist before launching processing. If one
     * is missing, craft it (and its crafting table) first, gathering any
     * mineable ingredients needed for those workstation recipes.
     */
    private static boolean startWorkstationPrerequisites(ProcessingPlan plan) {
        MinecraftClient client = MinecraftClient.getInstance();

        for (ProcessingTask task : plan.tasks()) {
            RecipeType type = task.type();
            String item = processorItem(type);

            if (ProcessingCoordinator.hasProcessorNearby(type)
                    || WorkstationUtils.hasItem(client, item)) {
                continue;
            }

            Map<String, Long> craftingTargets = buildCraftingTargets();
            CraftingPlan craftingPlan = CraftingPlanner.plan(craftingTargets, REJECTED_GATHERING_ITEMS);

            Map<String, Long> mineableMissing =
                    getMineableMissing(craftingPlan.missing());
            if (!mineableMissing.isEmpty()) {
                List<GatheringTask> gatheringTasks =
                        GatheringPlanner.plan(mineableMissing);
                if (!gatheringTasks.isEmpty()) {
                    GatheringQueue.start(gatheringTasks);
                    state = MaterialPipelineState.GATHERING;
                    return true;
                }
            }

            if (!craftingPlan.tasks().isEmpty()) {
                CraftingQueue.start(craftingTargets, REJECTED_GATHERING_ITEMS);
                state = MaterialPipelineState.CRAFTING;
                return true;
            }

            return false;
        }

        return false;
    }

    private static final List<String> GATHERABLE_FUELS = List.of(
            "minecraft:coal",
            "minecraft:oak_log",
            "minecraft:spruce_log",
            "minecraft:birch_log",
            "minecraft:jungle_log",
            "minecraft:acacia_log",
            "minecraft:dark_oak_log",
            "minecraft:mangrove_log",
            "minecraft:cherry_log",
            "minecraft:crimson_stem",
            "minecraft:warped_stem"
    );

    private static final List<String> ALL_FUELS = List.of(
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
    );

    /**
     * Plans a dedicated reserve of fuel for every pending processing output.
     * The target is cached for this pipeline run so repeated replanning does
     * not keep increasing the fuel requirement as inventory changes.
     */
    private static Map<String, Long> buildFuelTargets() {
        long requiredTicks = 0L;
        Map<String, Long> processingTargets = buildProcessingTargets();

        for (Map.Entry<String, Long> entry : processingTargets.entrySet()) {
            String item = normalize(entry.getKey());
            long required = Math.max(0L, entry.getValue());
            long have = InventoryUtils.count(item);

            if (item.isBlank()
                    || required <= have
                    || (!REJECTED_GATHERING_ITEMS.contains(item)
                        && MiningSourceResolver.hasMiningSource(item))
                    || !ProcessingPlanner.hasProcessingRecipe(item)) {
                continue;
            }

            RecipeType type =
                    ProcessingPlanner.getProcessingRecipeType(
                            item,
                            REJECTED_GATHERING_ITEMS
                    );
            if (type == null) {
                continue;
            }

            long remaining = required - have;
            long ticksPerOutput =
                    type == RecipeType.SMELTING ? 200L : 100L;
            requiredTicks = saturatedAdd(
                    requiredTicks,
                    saturatedMultiply(remaining, ticksPerOutput)
            );
        }

        if (requiredTicks <= 0L) {
            FUEL_TARGETS.clear();
            fuelPlanningFailure = "";
            return Map.of();
        }

        // A modest reserve covers rounding and small dependency-estimate
        // changes between processing stages.
        requiredTicks = saturatedAdd(
                requiredTicks,
                Math.max(400L, requiredTicks / 4L)
        );

        boolean cachedFuelRejected = FUEL_TARGETS.keySet().stream()
                .anyMatch(REJECTED_GATHERING_ITEMS::contains);
        if (!FUEL_TARGETS.isEmpty() && !cachedFuelRejected) {
            fuelPlanningFailure = "";
            return new LinkedHashMap<>(FUEL_TARGETS);
        }

        FUEL_TARGETS.clear();
        fuelPlanningFailure = "";

        String candidate = null;
        List<String> fuelCandidates = GATHERABLE_FUELS;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null
                && "minecraft:the_nether".equals(
                        client.world.getRegistryKey().getValue().toString())) {
            fuelCandidates = List.of(
                    "minecraft:crimson_stem",
                    "minecraft:warped_stem",
                    "minecraft:coal",
                    "minecraft:oak_log",
                    "minecraft:spruce_log",
                    "minecraft:birch_log",
                    "minecraft:jungle_log",
                    "minecraft:acacia_log",
                    "minecraft:dark_oak_log",
                    "minecraft:mangrove_log",
                    "minecraft:cherry_log"
            );
        }

        for (String fuel : fuelCandidates) {
            if (REJECTED_GATHERING_ITEMS.contains(fuel)) {
                continue;
            }

            if (MiningSourceResolver.hasMiningSource(fuel)) {
                candidate = fuel;
                break;
            }
        }

        if (candidate == null) {
            long availableTicks = getAvailableFuelTicks();
            if (availableTicks < requiredTicks) {
                fuelPlanningFailure =
                        "Processing requires more fuel, but no gatherable fuel source "
                                + "remains and the inventory fuel reserve is insufficient.";
            }
            return Map.of();
        }

        long burnTicks = fuelBurnTicks(candidate);
        long fuelCount = saturatedAdd(
                ceilDivide(requiredTicks, burnTicks),
                1L
        );

        // Existing items may also be reserved for the schematic or crafting
        // inputs. Add a separate reserve instead of assuming all current fuel
        // can be consumed by processing.
        long targetCount = saturatedAdd(
                InventoryUtils.count(candidate),
                fuelCount
        );
        FUEL_TARGETS.put(candidate, targetCount);
        return new LinkedHashMap<>(FUEL_TARGETS);
    }

    private static boolean expandFuelReserve() {
        Map<String, Long> plannedFuel = buildFuelTargets();
        if (plannedFuel.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, Long> entry
                : new LinkedHashMap<>(FUEL_TARGETS).entrySet()) {
            FUEL_TARGETS.put(
                    entry.getKey(),
                    saturatedAdd(entry.getValue(), 2L)
            );
        }

        return !FUEL_TARGETS.isEmpty();
    }

    private static boolean startFuelGatheringIfNeeded() {
        Map<String, Long> fuelTargets = buildFuelTargets();

        // Let the processor check its own fuel slot before failing. A
        // furnace may already contain fuel even when none remains in the
        // player's inventory; tickProcessing handles a true fuel shortage.
        if (fuelTargets.isEmpty()) {
            return false;
        }

        List<GatheringTask> tasks = GatheringPlanner.plan(fuelTargets);
        if (tasks.isEmpty()) {
            return false;
        }

        GatheringQueue.start(tasks);
        state = GatheringQueue.isRunning()
                ? MaterialPipelineState.GATHERING
                : MaterialPipelineState.VERIFYING_GATHERING;
        return true;
    }

    private static long getAvailableFuelTicks() {
        long ticks = 0L;
        for (String fuel : ALL_FUELS) {
            ticks = saturatedAdd(
                    ticks,
                    saturatedMultiply(
                            InventoryUtils.count(fuel),
                            fuelBurnTicks(fuel)
                    )
            );
        }
        return ticks;
    }

    private static long fuelBurnTicks(String fuel) {
        if ("minecraft:coal_block".equals(fuel)) {
            return 16000L;
        }
        if ("minecraft:coal".equals(fuel)
                || "minecraft:charcoal".equals(fuel)) {
            return 1600L;
        }
        return 300L;
    }

    private static long ceilDivide(long value, long divisor) {
        if (value <= 0L) {
            return 0L;
        }
        return value / divisor + (value % divisor == 0L ? 0L : 1L);
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static long saturatedMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    /**
     * Builds all currently mineable physical requirements.
     *
     * This intentionally includes:
     *
     * 1. Directly mineable final targets.
     * 2. Missing processing inputs.
     * 3. Missing crafting dependencies.
     *
     * Example:
     *
     * iron_ingot x10
     *
     * becomes:
     *
     * raw_iron x10
     *
     * which becomes:
     *
     * iron_ore / deepslate_iron_ore
     */
    private static Map<String, Long>
    buildGatheringTargets() {

        Map<String, Long> result =
                new LinkedHashMap<>();

        /*
         * Direct targets.
         *
         * GatheringPlanner will reject targets that do not have
         * a validated mining source.
         */
        for (Map.Entry<String, Long> entry
                : targets.entrySet()) {

            mergePositive(
                    result,
                    entry.getKey(),
                    entry.getValue()
            );
        }

        /*
         * Processing dependencies.
         */
        ProcessingPlan processingPlan =
                ProcessingPlanner.plan(buildProcessingTargets(), REJECTED_GATHERING_ITEMS);

        for (Map.Entry<String, Long> entry
                : processingPlan.missing().entrySet()) {

            mergePositive(
                    result,
                    entry.getKey(),
                    entry.getValue()
            );
        }

        /*
         * Crafting dependencies.
         */
        CraftingPlan craftingPlan =
                CraftingPlanner.plan(buildCraftingTargets(), REJECTED_GATHERING_ITEMS);

        for (Map.Entry<String, Long> entry
                : craftingPlan.missing().entrySet()) {

            mergePositive(
                    result,
                    entry.getKey(),
                    entry.getValue()
            );
        }

        /*
         * Fuel is a separate consumable requirement. Merge it with the
         * material requirements so fuel reserved for crafting/final targets
         * is not accidentally counted twice as available fuel.
         */
        for (Map.Entry<String, Long> entry : buildFuelTargets().entrySet()) {
            mergePositive(result, entry.getKey(), entry.getValue());
        }

        return result;
    }

    private static void tickGathering() {

        if (!GatheringQueue.isRunning()) {

            state =
                    MaterialPipelineState.VERIFYING_GATHERING;

            return;
        }

        /*
         * GatheringCoordinator uses BLOCKED as a recoverable retry state
         * (including waiting for inventory space), not a terminal failure.
         * Let its recovery loop run; once it exhausts/skips tasks the queue
         * becomes idle and the verification stage reports what is still
         * missing. Also, COMPLETE is only a per-material transition.
         */
    }

    /* ========================================================= */
    /* Verify gathering                                          */
    /* ========================================================= */

    private static void tickVerifyingGathering() {
        // Remember failed materials before a later gathering run resets its
        // per-run report. Otherwise the pipeline can re-explore the same
        // impossible target forever after each queue finishes.
        REJECTED_GATHERING_ITEMS.addAll(
                GatheringQueue.getSkippedMaterials()
        );

        /*
         * If everything is already present, skip processing and
         * crafting entirely.
         */
        if (allTargetsSatisfied()) {

            state =
                    MaterialPipelineState.COMPLETE;

            return;
        }

        /*
         * First determine whether processing inputs exist.
         */
        if (startFuelGatheringIfNeeded()) {
            return;
        }

        ProcessingPlan processingPlan =
                ProcessingPlanner.plan(buildProcessingTargets(), REJECTED_GATHERING_ITEMS);

        /*
         * Gather any missing physical processing inputs.
         */
        Map<String, Long> mineableMissing =
                getMineableMissing(
                        processingPlan.missing()
                );

        if (!mineableMissing.isEmpty()) {

            List<GatheringTask> tasks =
                    GatheringPlanner.plan(
                            mineableMissing
                    );

            if (!tasks.isEmpty()) {

                GatheringQueue.start(tasks);

                state =
                        MaterialPipelineState.GATHERING;

                return;
            }
        }

        /*
         * Run any processing tasks that are currently executable before
         * declaring the stage blocked. A plan can contain both available
         * work and a missing intermediate output (for example, smelt
         * cobblestone into stone before smelting that stone into smooth
         * stone). The next inventory-driven pass will resolve the next layer.
         */
        if (!processingPlan.tasks().isEmpty()) {
            if (startWorkstationPrerequisites(processingPlan)) {
                return;
            }

            ProcessingQueue.start(buildProcessingTargets());
            state = MaterialPipelineState.PROCESSING;
            return;
        }

        /*
         * Nothing can be processed right now. If inputs remain missing and
         * none of them are mineable, this is a genuine dependency failure.
         */
        if (!processingPlan.missing().isEmpty()) {
            block(
                    "Processing inputs are missing: "
                            + formatMissing(processingPlan.missing())
            );
            return;
        }

        state = MaterialPipelineState.VERIFYING_PROCESSING;
    }

    /* ========================================================= */
    /* Processing                                                 */
    /* ========================================================= */

    private static void tickProcessing() {

        ProcessingTaskState processingState =
                ProcessingQueue.getState();

        /*
         * ProcessingCoordinator marks itself not-running when blocked.
         * Check the terminal error before the generic stopped-queue case,
         * otherwise the pipeline would restart the same impossible task
         * indefinitely.
         */
        if (processingState
                == ProcessingTaskState.BLOCKED) {

            String reason = ProcessingQueue.getBlockedReason();
            boolean fuelShortage = reason != null
                    && reason.contains("No furnace fuel was found");

            if (fuelShortage
                    && fuelRecoveryAttempts < 3
                    && expandFuelReserve()) {
                fuelRecoveryAttempts++;
                ProcessingQueue.stop();
                state = MaterialPipelineState.VERIFYING_PROCESSING;
                return;
            }

            if (fuelShortage
                    && fuelPlanningFailure != null
                    && !fuelPlanningFailure.isBlank()) {
                block(fuelPlanningFailure);
                return;
            }

            block(
                    reason == null
                            || reason.isBlank()
                            ? "Processing blocked."
                            : reason
            );

            return;
        }

        if (!ProcessingQueue.isRunning()
                || processingState
                    == ProcessingTaskState.COMPLETE) {

            state =
                    MaterialPipelineState.VERIFYING_PROCESSING;
        }
    }

    /* ========================================================= */
    /* Verify processing                                         */
    /* ========================================================= */

    private static void tickVerifyingProcessing() {

        /*
         * Always re-plan against the CURRENT inventory.
         */
        ProcessingPlan processingPlan =
                ProcessingPlanner.plan(buildProcessingTargets(), REJECTED_GATHERING_ITEMS);

        /*
         * More processing is still required.
         */
        if (!processingPlan.tasks().isEmpty()) {

            if (startFuelGatheringIfNeeded()) {
                return;
            }

            if (startWorkstationPrerequisites(processingPlan)) {
                return;
            }

            ProcessingQueue.start(buildProcessingTargets());

            state =
                    MaterialPipelineState.PROCESSING;

            return;
        }

        /*
         * Processing inputs disappeared or were insufficient.
         * Attempt to gather them again.
         */
        if (!processingPlan.missing().isEmpty()) {

            Map<String, Long> mineableMissing =
                    getMineableMissing(
                            processingPlan.missing()
                    );

            if (!mineableMissing.isEmpty()) {

                List<GatheringTask> tasks =
                        GatheringPlanner.plan(
                                mineableMissing
                        );

                if (!tasks.isEmpty()) {

                    GatheringQueue.start(tasks);

                    state =
                            MaterialPipelineState.GATHERING;

                    return;
                }

                block(
                        "Required processing inputs could not be gathered: "
                                + formatMissing(
                                mineableMissing
                        )
                );

                return;
            }

            /*
             * Missing inputs that are not mineable must be supplied
             * by some other dependency. Recalculate crafting before
             * declaring failure.
             */
            CraftingPlan craftingPlan =
                    CraftingPlanner.plan(buildCraftingTargets(), REJECTED_GATHERING_ITEMS);

            if (!craftingPlan.tasks().isEmpty()) {

                /*
                 * This is uncommon, but prevents the pipeline from
                 * getting stuck simply because another dependency
                 * became available.
                 */
                CraftingQueue.start(buildCraftingTargets(), REJECTED_GATHERING_ITEMS);

                state =
                        MaterialPipelineState.CRAFTING;

                return;
            }

            block(
                    "Processing could not satisfy: "
                            + formatMissing(
                            processingPlan.missing()
                    )
            );

            return;
        }

        /*
         * Processing is completely satisfied.
         *
         * Move to crafting.
         */
        CraftingQueue.start(buildCraftingTargets(), REJECTED_GATHERING_ITEMS);

        if (CraftingQueue.isRunning()) {

            state =
                    MaterialPipelineState.CRAFTING;

        } else {

            verifyCrafting();
        }
    }

    /* ========================================================= */
    /* Crafting                                                   */
    /* ========================================================= */

    private static void tickCrafting() {

        CraftingTaskState craftingState =
                CraftingQueue.getState();

        /*
         * CraftingCoordinator also marks itself not-running on failure.
         * Handle that terminal state before interpreting !isRunning() as
         * normal completion, or the pipeline can repeatedly retry a blocked
         * recipe without surfacing the actual reason.
         */
        if (craftingState
                == CraftingTaskState.BLOCKED) {

            String reason =
                    CraftingQueue.getBlockedReason();

            block(
                    reason == null
                            || reason.isBlank()
                            ? "Crafting blocked."
                            : reason
            );

            return;
        }

        if (!CraftingQueue.isRunning()
                || craftingState
                    == CraftingTaskState.COMPLETE) {

            verifyCrafting();
        }
    }

    /* ========================================================= */
    /* Final verification                                        */
    /* ========================================================= */

    private static void verifyCrafting() {

        /*
         * This is the most important check in the entire pipeline.
         *
         * Never report COMPLETE merely because CraftingQueue
         * finished. Check the actual final inventory.
         */
        if (allTargetsSatisfied()) {

            state =
                    MaterialPipelineState.COMPLETE;

            return;
        }

        /*
         * Something is still missing.
         *
         * First check whether processing needs to happen again.
         */
        ProcessingPlan processingPlan =
                ProcessingPlanner.plan(buildProcessingTargets(), REJECTED_GATHERING_ITEMS);

        if (!processingPlan.tasks().isEmpty()) {

            if (startFuelGatheringIfNeeded()) {
                return;
            }

            if (startWorkstationPrerequisites(processingPlan)) {
                return;
            }

            ProcessingQueue.start(buildProcessingTargets());

            state =
                    MaterialPipelineState.PROCESSING;

            return;
        }

        /*
         * If processing is missing physical inputs, gather them.
         */
        if (!processingPlan.missing().isEmpty()) {

            Map<String, Long> mineableMissing =
                    getMineableMissing(
                            processingPlan.missing()
                    );

            if (!mineableMissing.isEmpty()) {

                List<GatheringTask> tasks =
                        GatheringPlanner.plan(
                                mineableMissing
                        );

                if (!tasks.isEmpty()) {

                    GatheringQueue.start(tasks);

                    state =
                            MaterialPipelineState.GATHERING;

                    return;
                }
            }
        }

        /*
         * Recalculate crafting against the actual inventory.
         */
        CraftingPlan craftingPlan =
                CraftingPlanner.plan(buildCraftingTargets(), REJECTED_GATHERING_ITEMS);

        if (!craftingPlan.tasks().isEmpty()) {

            CraftingQueue.start(buildCraftingTargets(), REJECTED_GATHERING_ITEMS);

            state =
                    MaterialPipelineState.CRAFTING;

            return;
        }

        /*
         * Finally inspect the final target requirements directly.
         *
         * This gives us an accurate error rather than a generic
         * "crafting failed".
         */
        Map<String, Long> missing =
                getFinalMissing();

        /*
         * One last attempt to gather directly mineable final items.
         */
        Map<String, Long> mineableMissing =
                getMineableMissing(missing);

        if (!mineableMissing.isEmpty()) {

            List<GatheringTask> tasks =
                    GatheringPlanner.plan(
                            mineableMissing
                    );

            if (!tasks.isEmpty()) {

                GatheringQueue.start(tasks);

                state =
                        MaterialPipelineState.GATHERING;

                return;
            }
        }

        /*
         * Nothing can make progress anymore.
         */
        block(
                "Final material requirements are still missing: "
                        + formatMissing(missing)
        );
    }

    /* ========================================================= */
    /* Inventory planning helpers                                */
    /* ========================================================= */

    private static boolean allTargetsSatisfied() {

        for (Map.Entry<String, Long> entry
                : targets.entrySet()) {

            long have =
                    InventoryUtils.count(
                            entry.getKey()
                    );

            if (have < entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    private static Map<String, Long> getFinalMissing() {

        Map<String, Long> missing =
                new LinkedHashMap<>();

        for (Map.Entry<String, Long> entry
                : targets.entrySet()) {

            long required =
                    entry.getValue();

            long have =
                    InventoryUtils.count(
                            entry.getKey()
                    );

            if (have < required) {

                missing.put(
                        entry.getKey(),
                        required - have
                );
            }
        }

        return missing;
    }

    /**
     * Filters a missing-material map down to materials for which
     * we have an explicitly validated physical mining source.
     *
     * This is deliberately NOT:
     *
     *     "can Baritone mine this?"
     *
     * It is:
     *
     *     "does our material resolver explicitly say that this
     *      requested inventory item can be obtained by mining?"
     */
    private static Map<String, Long>
    getMineableMissing(
            Map<String, Long> missing
    ) {

        Map<String, Long> result =
                new LinkedHashMap<>();

        if (missing == null
                || missing.isEmpty()) {

            return result;
        }

        for (Map.Entry<String, Long> entry
                : missing.entrySet()) {

            String item =
                    normalize(entry.getKey());

            long required =
                    Math.max(
                            0L,
                            entry.getValue()
                    );

            if (required <= 0L) {
                continue;
            }

            if (REJECTED_GATHERING_ITEMS.contains(item)
                    || !MiningSourceResolver.hasMiningSource(item)) {
                continue;
            }

            long have =
                    InventoryUtils.count(item);

            /*
             * The planner maps passed here contain quantities STILL
             * MISSING, not absolute inventory targets. GatheringTask,
             * however, uses an absolute inventory target. Convert the
             * deficit into the total quantity that must be present.
             *
             * Example: 8 raw iron present, 2 still needed -> gather until
             * inventory reaches 10, not until it reaches 2.
             */
            if (required > 0L) {
                long target = required > Long.MAX_VALUE - have
                        ? Long.MAX_VALUE
                        : have + required;

                result.put(item, target);
            }
        }

        return result;
    }

    /* ========================================================= */
    /* Failure handling                                          */
    /* ========================================================= */

    private static void block(String reason) {

        GatheringQueue.stop();
        ProcessingQueue.stop();
        CraftingQueue.stop();

        blockedReason =
                reason == null
                        || reason.isBlank()
                        ? "Unknown pipeline failure."
                        : reason;

        state =
                MaterialPipelineState.BLOCKED;
    }

    /* ========================================================= */
    /* Utility                                                    */
    /* ========================================================= */

    private static void mergePositive(
            Map<String, Long> map,
            String item,
            long amount
    ) {

        if (item == null
                || item.isBlank()
                || amount <= 0L) {

            return;
        }

        map.merge(
                normalize(item),
                amount,
                Long::sum
        );
    }

    private static String formatMissing(
            Map<String, Long> missing
    ) {

        if (missing == null
                || missing.isEmpty()) {

            return "";
        }

        List<String> parts =
                new ArrayList<>();

        for (Map.Entry<String, Long> entry
                : missing.entrySet()) {

            parts.add(
                    entry.getKey()
                            + " x"
                            + Math.max(
                            0L,
                            entry.getValue()
                    )
            );
        }

        return String.join(
                ", ",
                parts
        );
    }

    private static String normalize(String id) {

        if (id == null
                || id.isBlank()) {

            return "";
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}