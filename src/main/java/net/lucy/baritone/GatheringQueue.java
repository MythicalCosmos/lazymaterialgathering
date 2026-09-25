package net.lucy.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.process.IBaritoneProcess;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lucy.data.DataManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * STARTER SKETCH, not a finished feature: runs Baritone's "mine" command once for every
 * raw material on your list, one after another, instead of you typing a command per item.
 *
 * How it works: Baritone's mining process reports isActive() while it's still working.
 * Each client tick, if nothing is active, this pulls the next raw material off the queue
 * and starts mining it. When the list runs out, it stops.
 *
 * What this does NOT do, and would need adding before it's genuinely useful:
 *  - Know when "enough" of an item has been gathered and stop that item early (mineByName
 *    runs until told otherwise, or until nothing matching is left in range).
 *  - Walk back to a chest/base and deposit between items.
 *  - Skip items you already have enough of in your inventory.
 *  - Handle a quantity per item (Baritone's mine command doesn't take a target count on
 *    its own -- see IMineProcess for the options it does support, like a Y-level range).
 *  - Recover from Baritone losing control to another process (a hostile mob attacking,
 *    for example) rather than just re-checking isActive() next tick.
 */
public class GatheringQueue {
    private static final Deque<String> queue = new ArrayDeque<>();
    private static boolean running = false;

    public static void start() {
        queue.clear();
        for (Map.Entry<String, Long> entry : DataManager.getRawMaterials().entrySet()) {
            queue.add(entry.getKey());
        }
        running = true;
        startNext();
    }

    public static void stop() {
        running = false;
        queue.clear();
        getBaritone().getPathingBehavior().cancelEverything();
    }

    // Called once, from your mod's client init, to hook this into the game loop
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (running == false) {
                return;
            }

            IBaritoneProcess mineProcess = getBaritone().getMineProcess();
            if (mineProcess.isActive() == false) {
                startNext();
            }
        });
    }

    private static void startNext() {
        String next = queue.poll();

        if (next == null) {
            running = false; // ran out of items
            return;
        }

        getBaritone().getMineProcess().mineByName(next);
    }

    private static IBaritone getBaritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }
}