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
        MINING,
        RETURNING_TO_DEPOSIT,
        PAUSED
    }

    private static final Deque<GatheringTask> QUEUE = new ArrayDeque<>();
    private static State state = State.IDLE;
    private static State stateBeforePause;
    private static GatheringTask currentTask;
    private static BlockPos currentResourceTarget;
    private static int failedAttempts;
    private GatheringQueue() {
    }

    public static void start(Map<String, Long> materials) {
        start(GatheringPlanner.plan(materials));
    }

    public static void start(List<GatheringTask> tasks) {
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
        failedAttempts = 0;
        QUEUE.clear();
        if (cancelBaritone) {
            getBaritone().getPathingBehavior().cancelEverything();
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

    public static Optional<Double> getEstimatedSecondsRemaining() {
        return getBaritone().getPathingBehavior().estimatedTicksToGoal().map(ticks -> ticks / 20.0);
    }

    public static BlockPos getCurrentResourceTarget() {
        return currentResourceTarget;
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
            currentResourceTarget = null;
            goToDepositOrNext();
            return;
        }

        switch (state) {
            case MOVING_TO_RESOURCE -> {
                if (!getBaritone().getPathingBehavior().isPathing()) {
                    startMiningCurrent();
                }
            }

            case MINING -> {
                if (!getBaritone().getMineProcess().isActive()) {
                    failedAttempts++;
                    if (failedAttempts >= 3) {
                        System.out.println("[LMG] Could not gather enough of " + currentTask.item() + " after 3 attempts. Stopping.");
                        state = State.IDLE;
                        currentResourceTarget = null;
                        return;
                    }
                    findAndGoToKnownResource();
                }
            }

            case RETURNING_TO_DEPOSIT -> {
                if (!getBaritone().getPathingBehavior().isPathing()) {
                    startNextItem();
                }
            }
            default -> {
            }
        }
    }

    private static void goToDepositOrNext() {
        Optional<BetterBlockPos> deposit = Configs.Generic.RETURN_TO_DEPOSIT_BETWEEN_ITEMS.getBooleanValue() ? DepositLocations.get() : Optional.empty();
        if (deposit.isPresent()) {
            currentResourceTarget = null;
            state = State.RETURNING_TO_DEPOSIT;
            getBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(deposit.get()));
        } else {
            startNextItem();
        }
    }

    private static void startNextItem() {
        currentTask = QUEUE.pollFirst();
        currentResourceTarget = null;
        failedAttempts = 0;
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

    private static void findAndGoToKnownResource() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            String dimension = client.world.getRegistryKey().getValue().toString();
            Optional<BlockPos> target = WorldKnowledge.findNearestResource(dimension, currentTask.item(), client.player.getBlockPos(), client.world);
            if (target.isPresent()) {
                currentResourceTarget = target.get().toImmutable();
                state = State.MOVING_TO_RESOURCE;
                getBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentResourceTarget));
                return;
            }
        }

        currentResourceTarget = null;
        startMiningCurrent();
    }

    private static void startMiningCurrent() {
        if (currentTask == null) {
            return;
        }

        state = State.MINING;
        getBaritone().getMineProcess().mineByName(currentTask.item());
    }

    private static IBaritone getBaritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }
}