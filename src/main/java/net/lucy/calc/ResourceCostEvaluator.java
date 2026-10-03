package net.lucy.calc;

import net.lucy.data.WorldKnowledge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Estimates the cost of acquiring a resource from the
 * player's current position.
 *
 * This class is deliberately about WORLD ACQUISITION.
 *
 * Recipe decomposition belongs to RawMaterials / RecipeHeuristics.
 */
public final class ResourceCostEvaluator {
    public static final double UNKNOWN_RESOURCE_COST = 1_000_000.0;
    private ResourceCostEvaluator() {
    }

    /**
     * Compatibility API.
     */
    public static double calculate(String item) {
        return estimate(item);
    }

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

        return WorldKnowledge.getResourceLocationScore(dimension, item, target.get(), player, client.world);
    }

    public static boolean isKnown(String item) {
        return estimate(item) < UNKNOWN_RESOURCE_COST;
    }

    public static double getBestBiomeScore(String item) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || item == null || item.isBlank()) {
            return 0.0;
        }

        String dimension = client.world.getRegistryKey().getValue().toString();
        return WorldKnowledge.getBestBiomeForResource(dimension, item).map(WorldKnowledge.BiomeScore::score).orElse(0.0);
    }
}