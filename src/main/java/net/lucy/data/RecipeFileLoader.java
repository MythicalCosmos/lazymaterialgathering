package net.lucy.data;

import net.lucy.model.ObtainSource;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class RecipeFileLoader {

    public static Map<String, List<Recipe>> loadRecipes(Path filePath) throws IOException {
        Map<String, List<Recipe>> recipes = new HashMap<>();
        List<String> lines = Files.readAllLines(filePath);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|", -1);
            String itemName = parts[0];
            String recipeId = parts[1];
            RecipeType type = RecipeType.valueOf(parts[2]);
            int outputCount = Integer.parseInt(parts[3]);
            String ingredientsRaw = parts[4];

            Map<String, Integer> ingredients = new HashMap<>();
            if (!ingredientsRaw.isEmpty()) {
                for (String pair : ingredientsRaw.split(",")) {
                    String[] keyValue = pair.split(":");
                    ingredients.put(keyValue[0], Integer.parseInt(keyValue[1]));
                }
            }

            Recipe recipe = new Recipe(recipeId, type, ingredients, outputCount);
            recipes.computeIfAbsent(itemName, k -> new ArrayList<>()).add(recipe);
        }

        return recipes;
    }

    public static Map<String, Set<ObtainSource>> loadObtainSources(Path filePath) throws IOException {
        Map<String, Set<ObtainSource>> sources = new HashMap<>();
        List<String> lines = Files.readAllLines(filePath);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|", 4);
            String itemName = parts[0];
            ObtainSource.Type type = ObtainSource.Type.valueOf(parts[1]);
            String modifier = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
            String description = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null;
            ObtainSource source = new ObtainSource(type, modifier, description);
            sources.computeIfAbsent(itemName, k -> new HashSet<>()).add(source);
        }

        return sources;
    }
}