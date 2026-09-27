package net.lucy.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lucy.config.Configs;
import net.lucy.data.DataManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

/**
 * Runs Baritone's "mine" command once for every raw material on your list, one after
 * another -- optionally walking back to your deposit location (see DepositLocation)
 * between each one -- instead of you typing a command per item.
 *
 * What this does NOT do, and would need adding before it's a complete gathering loop:
 *  - Know when "enough" of an item has been gathered and stop that item early (this just
 *    lets Baritone's mine command run until nothing matching is left in range, the same
 *    as if you'd typed #mine yourself -- Baritone's own mine command has no built-in
 *    target quantity).
 *  - Actually put items into a container at the deposit location -- it walks there and
 *    stops, but doesn't open a chest or move items (that's regular container-slot packets,
 *    unrelated to Baritone, and not wired up here yet).
 *  - Skip items you already have enough of in your inventory.
 *  - Recover from Baritone losing control to another process (a hostile mob attacking,
 *    for example) rather than just re-checking state next tick.
 */
public class GatheringQueue {

    private enum State { IDLE, MINING, RETURNING_TO_DEPOSIT }

    private static final Deque<String> queue = new ArrayDeque<>();
    private static State state = State.IDLE;
    private static String currentItem = null;

    public static void start(Map<String, Long> materials) {
        queue.clear();
        queue.addAll(materials.keySet());
        startNextItem();
    }

    public static void stop() {
        state = State.IDLE;
        currentItem = null;
        queue.clear();
        getBaritone().getPathingBehavior().cancelEverything();
    }

    public static boolean isRunning() {
        return state != State.IDLE;
    }

    public static String getCurrentItem() {
        return currentItem;
    }

    public static int getRemainingCount() {
        return queue.size() + (state == State.IDLE ? 0 : 1);
    }

    /** Seconds left in the current step, if Baritone has an estimate for it yet. */
    public static Optional<Double> getEstimatedSecondsRemaining() {
        Optional<Double> ticks = getBaritone().getPathingBehavior().estimatedTicksToGoal();
        return ticks.map(t -> t / 20.0);
    }

    // Called once, from your mod's client init, to hook this into the game loop
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            switch (state) {
                case MINING -> {
                    if (getBaritone().getMineProcess().isActive() == false) {
                        goToDepositOrNext();
                    }
                }
                case RETURNING_TO_DEPOSIT -> {
                    if (getBaritone().getPathingBehavior().isPathing() == false) {
                        startNextItem();
                    }
                }
                case IDLE -> {
                    // nothing to do
                }
            }
        });
    }

    private static void goToDepositOrNext() {
        Optional<BetterBlockPos> deposit = Configs.Generic.RETURN_TO_DEPOSIT_BETWEEN_ITEMS.getBooleanValue()
                ? DepositLocations.get()
                : Optional.empty();

        if (deposit.isPresent()) {
            state = State.RETURNING_TO_DEPOSIT;
            getBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(deposit.get()));
        } else {
            startNextItem();
        }
    }

    private static void startNextItem() {
        String next = queue.poll();

        if (next == null) {
            state = State.IDLE;
            currentItem = null;
            return;
        }

        currentItem = next;
        state = State.MINING;
        getBaritone().getMineProcess().mineByName(next);
    }

    private static IBaritone getBaritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }
}