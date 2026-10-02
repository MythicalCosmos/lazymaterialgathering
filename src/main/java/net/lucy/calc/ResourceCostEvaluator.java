package net.lucy.calc;

import net.lucy.data.WorldKnowledge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public final class ResourceCostEvaluator {
    private static final double UNKNOWN_RESOURCE_COST = 1_000_000.0;
    private ResourceCostEvaluator() {
    }

    /**
     * Calculates the estimated travel/resource cost for obtaining an item.
     *
     * Lower values mean the resource is considered easier/closer to obtain.
     */
    public static double calculate(String item) {
        return estimate(item);
    }

    /**
     * Calculates the estimated cost of reaching the nearest known
     * source of the requested resource.
     */
    public static double estimate(String item) {
        if (item == null || item.isBlank()) {
            return UNKNOWN_RESOURCE_COST;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return UNKNOWN_RESOURCE_COST;
        }

        BlockPos player = client.player.getBlockPos();
        String dimension = client.world.getRegistryKey().getValue().toString();
        Optional<BlockPos> target = WorldKnowledge.findNearestResource(dimension, item, player, client.world);
        if (target.isEmpty()) {
            return UNKNOWN_RESOURCE_COST;
        }

        BlockPos resource = target.get();
        double horizontalDistance = Math.sqrt(Math.pow(player.getX() - resource.getX(), 2) + Math.pow(player.getZ() - resource.getZ(), 2));
        double verticalDistance = Math.abs(player.getY() - resource.getY());
        /*
         * Vertical movement is somewhat more expensive for
         * practical gathering/pathing purposes, so give it
         * a modest additional weight.
         */
        return horizontalDistance + (verticalDistance * 1.5);
    }

    /**
     * Returns true when the resource is currently known
     * to WorldKnowledge.
     */
    public static boolean isKnown(String item) {
        return calculate(item) < UNKNOWN_RESOURCE_COST;
    }
}