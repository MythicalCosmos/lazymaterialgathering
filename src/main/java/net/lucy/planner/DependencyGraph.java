package net.lucy.planner;


import java.util.*;



public class DependencyGraph {



    private final Map<String,List<String>> dependencies =
            new HashMap<>();




    public void addDependency(
            String output,
            String input
    ){

        dependencies
                .computeIfAbsent(
                        output,
                        k -> new ArrayList<>()
                )
                .add(input);

    }




    public List<String> getDependencies(
            String item
    ){

        return dependencies
                .getOrDefault(
                        item,
                        Collections.emptyList()
                );
    }




    public boolean hasDependencies(
            String item
    ){

        return dependencies.containsKey(item);
    }



}