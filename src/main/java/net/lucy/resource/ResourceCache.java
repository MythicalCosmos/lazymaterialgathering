package net.lucy.resource;

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


    public List<ResourceNode> get(String item){
        cleanup();
        return resources.getOrDefault(item, Collections.emptyList());
    }

    private void cleanup(){
        resources.values().forEach(list -> list.removeIf(ResourceNode::isExpired));
    }

}