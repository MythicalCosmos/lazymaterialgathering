package net.lucy.data;

import net.lucy.model.ObtainSource;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecipeFileLoader {
    private RecipeFileLoader() {
    }

    public static Map<String, List<Recipe>> loadRecipes(Path filePath) throws IOException {
        Map<String, List<Recipe>> recipes = new HashMap<>();
        for (String line : Files.readAllLines(filePath)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|", -1);

            /*
             * Old format:
             *
             * item|id|type|output|ingredients
             *
             * New format:
             *
             * item|id|type|output|ingredients|choices
             */
            if (parts.length < 5) {
                continue;
            }

            String itemName = parts[0];
            String recipeId = parts[1];
            RecipeType type;
            try {
                type = RecipeType.valueOf(parts[2]);
            } catch (IllegalArgumentException e) {
                continue;
            }

            int outputCount;
            try {
                outputCount = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                continue;
            }

            Map<String, Integer> ingredients = parseIngredients(parts[4]);
            List<List<String>> choices = parts.length >= 6 ? parseChoices(parts[5]) : new ArrayList<>();
            /*
             * Old caches don't have choices.
             * Reconstruct them from the ingredient counts.
             */
            if (choices.isEmpty()) {
                choices = choicesFromIngredients(ingredients);
            }

            Recipe recipe = new Recipe(recipeId, type, ingredients, choices, outputCount);
            recipes.computeIfAbsent(itemName, ignored -> new ArrayList<>()).add(recipe);
        }
        return recipes;
    }

    private static Map<String, Integer> parseIngredients(String raw) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }

        for (String pair : raw.split(",")) {
            if (pair.isEmpty()) {
                continue;
            }

            /*
             * Split only on the final colon.
             * This keeps this parser safe for future namespaced keys.
             */
            int separator = pair.lastIndexOf(':');
            if (separator <= 0 || separator >= pair.length() - 1) {
                continue;
            }

            String name = pair.substring(0, separator);
            String countText = pair.substring(separator + 1);
            try {
                int count = Integer.parseInt(countText);
                result.put(name, count);
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static List<List<String>> parseChoices(String raw) {
        List<List<String>> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }

        for (String slotRaw : raw.split(",", -1)) {
            if (slotRaw.isEmpty()) {
                result.add(new ArrayList<>());
                continue;
            }

            List<String> slot = new ArrayList<>();
            for (String choice : slotRaw.split("~", -1)) {
                if (!choice.isEmpty() && !slot.contains(choice)) {
                    slot.add(choice);
                }
            }
            result.add(slot);
        }
        return result;
    }

    private static List<List<String>> choicesFromIngredients(Map<String, Integer> ingredients) {
        List<List<String>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            int count = Math.max(0, entry.getValue());
            for (int i = 0; i < count; i++) {
                result.add(List.of(entry.getKey()));
            }
        }
        return result;
    }

    public static Map<String, Set<ObtainSource>> loadObtainSources(Path filePath) throws IOException {
        Map<String, Set<ObtainSource>> sources = new HashMap<>();
        for (String line : Files.readAllLines(filePath)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|", 4);
            if (parts.length < 2) {
                continue;
            }

            String itemName = parts[0];
            ObtainSource.Type type;
            try {
                type = ObtainSource.Type.valueOf(parts[1]);
            } catch (IllegalArgumentException e) {
                continue;
            }

            String modifier = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
            String description = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null;
            ObtainSource source = new ObtainSource(type, modifier, description);
            sources.computeIfAbsent(itemName, ignored -> new HashSet<>()).add(source);
        }
        return sources;
    }
}