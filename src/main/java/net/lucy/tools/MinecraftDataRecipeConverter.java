package net.lucy.tools;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MinecraftDataRecipeConverter {

    public static void main(String[] args) throws IOException {
        String itemsPath = "items.json";
        String recipesPath = "recipes.json";
        String outputPath = "recipes.txt";

        Map<Integer, String> idToName = loadItemNames(itemsPath);
        JSONObject recipesJson = loadJsonObject(recipesPath);

        List<String> outputLines = new ArrayList<>();
        int recipeCounter = 0;

        for (String itemIdKey : recipesJson.keySet()) {
            JSONArray recipesForItem = recipesJson.getJSONArray(itemIdKey);

            for (int i = 0; i < recipesForItem.length(); i++) {
                JSONObject recipe = recipesForItem.getJSONObject(i);

                Object resultRaw = recipe.get("result");
                Integer resultId = extractId(resultRaw);
                int outputCount = extractCount(resultRaw);

                if (resultId == null) {
                    continue; // shouldn't happen for a result, but skip defensively
                }

                String resultName = idToName.get(resultId);
                if (resultName == null) {
                    continue; // unknown id, skip rather than write a bad line
                }

                Map<Integer, Integer> ingredientCounts = new HashMap<>();

                if (recipe.has("ingredients")) {
                    JSONArray ingredients = recipe.getJSONArray("ingredients");
                    for (int j = 0; j < ingredients.length(); j++) {
                        Integer id = extractId(ingredients.get(j));
                        if (id != null) {
                            ingredientCounts.merge(id, 1, Integer::sum);
                        }
                    }
                } else if (recipe.has("inShape")) {
                    JSONArray rows = recipe.getJSONArray("inShape");
                    for (int r = 0; r < rows.length(); r++) {
                        JSONArray row = rows.getJSONArray(r);
                        for (int c = 0; c < row.length(); c++) {
                            Integer id = extractId(row.get(c));
                            if (id != null) {
                                ingredientCounts.merge(id, 1, Integer::sum);
                            }
                        }
                    }
                } else {
                    continue; // recipe has neither shape, skip it
                }

                if (ingredientCounts.isEmpty()) {
                    continue;
                }

                StringBuilder ingredientsText = new StringBuilder();
                boolean first = true;
                boolean allNamesResolved = true;

                for (Map.Entry<Integer, Integer> entry : ingredientCounts.entrySet()) {
                    String ingredientName = idToName.get(entry.getKey());
                    if (ingredientName == null) {
                        allNamesResolved = false;
                        break;
                    }
                    if (!first) ingredientsText.append(",");
                    ingredientsText.append(ingredientName).append(":").append(entry.getValue());
                    first = false;
                }

                if (!allNamesResolved) {
                    continue; // couldn't resolve every ingredient's name, skip this recipe
                }

                recipeCounter++;
                String recipeId = "mcdata_" + recipeCounter;
                String line = resultName + "|" + recipeId + "|CRAFTING|" + outputCount + "|" + ingredientsText;
                outputLines.add(line);
            }
        }

        try (FileWriter writer = new FileWriter(outputPath)) {
            for (String line : outputLines) {
                writer.write(line + "\n");
            }
        }

        System.out.println("Converted " + outputLines.size() + " recipes to " + outputPath);
    }

    private static Map<Integer, String> loadItemNames(String path) throws IOException {
        Map<Integer, String> idToName = new HashMap<>();
        JSONArray items = loadJsonArray(path);

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            int id = item.getInt("id");
            String name = item.getString("name");
            idToName.put(id, name);
        }

        return idToName;
    }

    private static Integer extractId(Object itemRepresentation) {
        if (itemRepresentation == null || itemRepresentation == JSONObject.NULL) {
            return null;
        }
        if (itemRepresentation instanceof Integer) {
            return (Integer) itemRepresentation;
        }
        if (itemRepresentation instanceof JSONArray) {
            JSONArray arr = (JSONArray) itemRepresentation;
            return arr.getInt(0);
        }
        if (itemRepresentation instanceof JSONObject) {
            JSONObject obj = (JSONObject) itemRepresentation;
            return obj.getInt("id");
        }
        return null;
    }

    private static int extractCount(Object itemRepresentation) {
        if (itemRepresentation instanceof JSONObject) {
            JSONObject obj = (JSONObject) itemRepresentation;
            if (obj.has("count")) {
                return obj.getInt("count");
            }
        }
        return 1;
    }

    private static JSONObject loadJsonObject(String path) throws IOException {
        try (FileReader reader = new FileReader(path)) {
            return new JSONObject(new JSONTokener(reader));
        }
    }

    private static JSONArray loadJsonArray(String path) throws IOException {
        try (FileReader reader = new FileReader(path)) {
            return new JSONArray(new JSONTokener(reader));
        }
    }
}