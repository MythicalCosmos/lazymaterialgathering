package net.lucy.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.cache.IWaypoint;
import baritone.api.cache.Waypoint;
import baritone.api.utils.BetterBlockPos;

import java.util.Optional;

/**
 * Where gathered materials should be dropped off. Stored as a Baritone "home" waypoint
 * (IWaypoint.Tag.HOME) rather than this mod's own config, so it also works with Baritone's
 * own commands (#home, #come) and survives exactly the way Baritone's other waypoints do.
 */
public class DepositLocations
{
    private static final String WAYPOINT_NAME = "lazymaterialgathering_deposit";

    public static void set(BetterBlockPos pos)
    {
        // Baritone only reports the single MOST RECENT waypoint per tag (see get() below),
        // so re-adding one under the same tag naturally replaces the old one for our purposes
        // without needing to track or remove the previous entry ourselves.
        BaritoneAPI.getProvider().getPrimaryBaritone()
                .getWorldProvider()
                .getCurrentWorld()
                .getWaypoints()
                .addWaypoint(new Waypoint(WAYPOINT_NAME, IWaypoint.Tag.HOME, pos));
    }

    public static Optional<BetterBlockPos> get()
    {
        var worldData = BaritoneAPI.getProvider().getPrimaryBaritone().getWorldProvider().getCurrentWorld();

        if (worldData == null)
        {
            return Optional.empty();
        }

        IWaypoint waypoint = worldData.getWaypoints().getMostRecentByTag(IWaypoint.Tag.HOME);
        return waypoint == null ? Optional.empty() : Optional.of(waypoint.getLocation());
    }

    public static boolean isSet()
    {
        return get().isPresent();
    }
}