package net.lucy.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.math.BlockPos;

/**
 * Centralised Baritone control for Lazy Material Gathering.
 *
 * GatheringQueue decides WHAT should happen.
 * This class decides HOW the request is sent to Baritone.
 */
public final class BaritoneController {
    private BaritoneController() {
    }

    public static IBaritone get() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    public static void goTo(BlockPos position) {
        if (position == null) {
            return;
        }

        goTo(new BetterBlockPos(position));
    }

    public static void goTo(BetterBlockPos position) {
        if (position == null) {
            return;
        }
        get().getCustomGoalProcess().setGoalAndPath(new GoalBlock(position));
    }

    public static void mine(String target) {

        if (target == null || target.isBlank()) {
            return;
        }

        get().getMineProcess().mineByName(target);
    }

    public static void cancelMining() {
        get().getMineProcess().cancel();
    }

    public static boolean isMining() {
        return get().getMineProcess().isActive();
    }

    public static void cancelAll() {
        get().getPathingBehavior().cancelEverything();
        get().getMineProcess().cancel();
    }

    public static void pause() {
        get().getCommandManager().execute("pause");
    }

    public static void resume() {
        get().getCommandManager().execute("resume");
    }

    public static boolean isPathing() {
        return get().getPathingBehavior().isPathing();
    }
}