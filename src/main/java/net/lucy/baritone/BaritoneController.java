package net.lucy.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.math.BlockPos;

public final class BaritoneController {

    private BaritoneController() {
    }

    public static IBaritone get() {
        return BaritoneAPI
                .getProvider()
                .getPrimaryBaritone();
    }

    public static void goTo(
            BlockPos position
    ) {
        if (position == null) {
            return;
        }

        goTo(
                new BetterBlockPos(position)
        );
    }

    public static void goTo(
            BetterBlockPos position
    ) {
        if (position == null) {
            return;
        }

        get()
                .getCustomGoalProcess()
                .setGoalAndPath(
                        new GoalBlock(position)
                );
    }

    /**
     * Mine a PHYSICAL BLOCK.
     *
     * Example:
     *
     * minecraft:iron_ore
     *
     * NOT:
     *
     * minecraft:raw_iron
     */
    public static void mine(
            String sourceBlock
    ) {
        if (sourceBlock == null
                || sourceBlock.isBlank()) {
            return;
        }

        get()
                .getMineProcess()
                .mineByName(sourceBlock);
    }

    public static void cancelMining() {
        get()
                .getMineProcess()
                .cancel();
    }

    public static void cancelAll() {

        get()
                .getPathingBehavior()
                .cancelEverything();

        get()
                .getMineProcess()
                .cancel();
    }

    public static void pause() {
        get()
                .getCommandManager()
                .execute("pause");
    }

    public static void resume() {
        get()
                .getCommandManager()
                .execute("resume");
    }

    public static boolean isPathing() {
        return get()
                .getPathingBehavior()
                .isPathing();
    }

    public static boolean isMining() {
        return get()
                .getMineProcess()
                .isActive();
    }
}