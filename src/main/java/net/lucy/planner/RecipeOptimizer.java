package net.lucy.planner;

import java.util.*;

public class RecipeOptimizer {
    public static class Recipe {
        public String output;
        public int amount;
        public Map<String,Integer> ingredients = new HashMap<>();
        public Recipe(String output, int amount){
            this.output = output;
            this.amount = amount;
        }
    }

    private final List<Recipe> recipes = new ArrayList<>();
    public void addRecipe(Recipe recipe){
        recipes.add(recipe);
    }

    public Recipe findBest(String item){
        return recipes.stream().filter(r -> r.output.equals(item)).min(Comparator.comparingInt(r -> r.ingredients.size())).orElse(null);
    }
}