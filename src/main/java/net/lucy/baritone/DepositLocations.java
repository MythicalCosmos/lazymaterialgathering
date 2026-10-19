package net.lucy.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.cache.IWaypoint;
import baritone.api.cache.Waypoint;
import baritone.api.utils.BetterBlockPos;

import java.util.Comparator;
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
        var worldData = BaritoneAPI.getProvider().getPrimaryBaritone()
                .getWorldProvider()
                .getCurrentWorld();

        if (worldData == null) {
            return;
        }

        var waypoints = worldData.getWaypoints();
        // Replace only our own deposit marker; do not disturb the player's
        // ordinary Baritone HOME waypoints.
        waypoints.getAllWaypoints().stream()
                .filter(waypoint -> WAYPOINT_NAME.equalsIgnoreCase(waypoint.getName()))
                .toList()
                .forEach(waypoints::removeWaypoint);

        waypoints.addWaypoint(
                new Waypoint(WAYPOINT_NAME, IWaypoint.Tag.HOME, pos)
        );
    }

    public static Optional<BetterBlockPos> get()
    {
        var worldData = BaritoneAPI.getProvider().getPrimaryBaritone().getWorldProvider().getCurrentWorld();

        if (worldData == null)
        {
            return Optional.empty();
        }

        IWaypoint waypoint = worldData.getWaypoints().getAllWaypoints().stream()
                .filter(candidate -> candidate.getTag() == IWaypoint.Tag.HOME)
                .filter(candidate -> WAYPOINT_NAME.equalsIgnoreCase(candidate.getName()))
                .max(Comparator.comparingLong(IWaypoint::getCreationTimestamp))
                .orElse(null);

        return waypoint == null
                ? Optional.empty()
                : Optional.of(waypoint.getLocation());
    }

    public static boolean isSet()
    {
        return get().isPresent();
    }
}