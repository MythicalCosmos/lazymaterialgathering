package net.lucy.calc;

import net.lucy.data.Recipes;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import java.util.List;
import java.util.Map;

public class RecipeReport {

    public static String listByType(RecipeType type) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Recipe>> entry : Recipes.recipes.entrySet()) {
            for (Recipe recipe : entry.getValue()) {
                if (recipe.type == type) {
                    sb.append(entry.getKey())
                            .append(" [").append(recipe.id).append("] x")
                            .append(recipe.outputCount)
                            .append(" <- ").append(recipe.ingredients)
                            .append("\n");
                }
            }
        }
        return sb.toString();
    }
}