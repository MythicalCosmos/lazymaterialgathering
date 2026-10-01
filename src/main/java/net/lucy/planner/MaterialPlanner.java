package net.lucy.planner;


import java.util.*;



public class MaterialPlanner {



    private final Map<String,MaterialCost> materials =
            new HashMap<>();



    private final RecipeOptimizer recipes;



    private final DependencyGraph graph;



    public MaterialPlanner(
            RecipeOptimizer recipes,
            DependencyGraph graph
    ){

        this.recipes = recipes;
        this.graph = graph;

    }





    public void calculate(
            String item,
            long amount
    ){

        calculateRecursive(
                item,
                amount
        );

    }





    private void calculateRecursive(
            String item,
            long amount
    ){


        RecipeOptimizer.Recipe recipe =
                recipes.findBest(item);



        /*
         * No recipe means:
         *
         * this is a base material
         *
         */


        if(recipe == null){


            materials
                    .computeIfAbsent(
                            item,
                            k ->
                                    new MaterialCost(
                                            item,
                                            0
                                    )
                    )
                    .increase(amount);


            return;

        }




        long crafts =
                (long)Math.ceil(
                        (double)amount
                                /
                                recipe.amount
                );



        for(
                Map.Entry<String,Integer> entry :
                recipe.ingredients.entrySet()
        ){


            calculateRecursive(
                    entry.getKey(),
                    entry.getValue()
                            *
                            crafts
            );

        }

    }






    public Collection<MaterialCost> getMaterials(){

        return materials.values();

    }





    public MaterialCost get(
            String item
    ){

        return materials.get(item);

    }



}