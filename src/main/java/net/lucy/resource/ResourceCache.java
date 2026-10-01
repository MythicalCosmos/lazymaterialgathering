package net.lucy.resource;

import net.minecraft.util.math.BlockPos;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceCache {
    private static final ResourceCache INSTANCE = new ResourceCache();
    public static ResourceCache get(){
        return INSTANCE;
    }
    private final Map<String,List<ResourceNode>> resources = new ConcurrentHashMap<>();
    public void add(ResourceNode node){
        resources.computeIfAbsent(node.getItem(), k -> new ArrayList<>()).add(node);
    }

    public void remove(String item, BlockPos pos){
        List<ResourceNode> nodes = resources.get(item);
        if(nodes == null)
            return;
        nodes.removeIf(node -> node.getPosition().equals(pos));
    }

    public List<ResourceNode> get(String item){
        cleanup();
        return resources.getOrDefault(item, Collections.emptyList());
    }

    public ResourceNode findBest(String item){
        return get(item).stream().min(Comparator.comparingDouble(ResourceNode::getMiningCost)).orElse(null);
    }

    private void cleanup(){
        resources.values().forEach(list -> list.removeIf(ResourceNode::isExpired));
    }

}