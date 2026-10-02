package net.lucy.data.world;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.math.BlockPos;

public class WorldResourceCache {
    private static final WorldResourceCache INSTANCE = new WorldResourceCache();
    public static WorldResourceCache get() {
        return INSTANCE;
    }

    private final Map<String,List<ResourceLocation>> resources = new ConcurrentHashMap<>();
    public void add(ResourceLocation resource){
        resources.computeIfAbsent(
                        resource.getItem(), x -> new ArrayList<>()).add(resource);
    }

    public ResourceLocation findBest(String item, BlockPos player){
        return resources.getOrDefault(item, Collections.emptyList()).stream().min(Comparator.comparingDouble(r -> r.getEfficiency(player))).orElse(null);
    }
}