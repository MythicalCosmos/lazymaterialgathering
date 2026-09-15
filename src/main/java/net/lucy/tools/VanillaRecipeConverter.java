package net.lucy.tools;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class VanillaRecipeConverter {

    public static void main(String[] args) throws IOException {
        File recipeFolder = new File("data/minecraft/recipe");
        String outputPath = "recipes.txt";

        File[] files = recipeFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            System.out.println("Recipe folder not found: " + recipeFolder.getPath());
            return;
        }

        List<String> outputLines = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (File file : files) {
            try {
                JSONObject json;
                try (FileReader reader = new FileReader(file)) {
                    json = new JSONObject(new JSONTokener(reader));
                }

                String type = json.optString("type", "");
                String recipeId = file.getName().replace(".json", "");

                if (type.equals("minecraft:crafting_shaped") || type.equals("minecraft:crafting_shapeless")) {
                    convertCrafting(json, recipeId, outputLines, skipped);
                } else if (isCookingType(type)) {
                    convertCooking(json, type, recipeId, outputLines, skipped);
                }
                // other types (stonecutting, smithing, etc.) intentionally skipped for now

            } catch (Exception e) {
                skipped.add(file.getName() + " (parse error: " + e.getMessage() + ")");
            }
        }

        try (FileWriter writer = new FileWriter(outputPath)) {
            for (String line : outputLines) {
                writer.write(line + "\n");
            }
        }

        System.out.println("Converted " + outputLines.size() + " recipes to " + outputPath);
        System.out.println("Skipped " + skipped.size() + " recipes (tag-based ingredients or unrecognized shape):");
        for (String s : skipped) {
            System.out.println("  " + s);
        }
    }

    private static boolean isCookingType(String type) {
        return type.equals("minecraft:smelting")
                || type.equals("minecraft:blasting")
                || type.equals("minecraft:smoking")
                || type.equals("minecraft:campfire_cooking");
    }

    private static void convertCooking(JSONObject json, String type, String recipeId,
                                       List<String> outputLines, List<String> skipped) {
        Object ingredientRaw = json.opt("ingredient");
        String ingredientName = extractSingleItemName(ingredientRaw);

        if (ingredientName == null) {
            skipped.add(recipeId + " (tag-based or multi-option ingredient)");
            return;
        }

        Object resultRaw = json.opt("result");
        String resultName = extractResultName(resultRaw);
        int outputCount = extractResultCount(resultRaw);

        if (resultName == null) {
            skipped.add(recipeId + " (unrecognized result format)");
            return;
        }

        String recipeType = "SMELTING"; // treat blasting/smoking/campfire as smelting-equivalent for now
        String line = resultName + "|" + recipeId + "|" + recipeType + "|" + outputCount + "|" + ingredientName + ":1";
        outputLines.add(line);
    }

    private static void convertCrafting(JSONObject json, String recipeId,
                                        List<String> outputLines, List<String> skipped) {
        Object resultRaw = json.opt("result");
        String resultName = extractResultName(resultRaw);
        int outputCount = extractResultCount(resultRaw);

        if (resultName == null) {
            skipped.add(recipeId + " (unrecognized result format)");
            return;
        }

        Map<String, Integer> ingredientCounts = new LinkedHashMap<>();
        boolean hadTagIngredient = false;

        if (json.has("ingredients")) {
            // shapeless: could be a flat list, or (older format) a key->ingredient map
            Object ingredientsRaw = json.get("ingredients");

            if (ingredientsRaw instanceof JSONArray) {
                JSONArray arr = (JSONArray) ingredientsRaw;
                for (int i = 0; i < arr.length(); i++) {
                    String name = extractSingleItemName(arr.get(i));
                    if (name == null) {
                        hadTagIngredient = true;
                        continue;
                    }
                    ingredientCounts.merge(name, 1, Integer::sum);
                }
            }
        } else if (json.has("key") && json.has("pattern")) {
            // shaped: key maps a letter to an ingredient, pattern is rows of letters
            JSONObject key = json.getJSONObject("key");
            JSONArray pattern = json.getJSONArray("pattern");

            Map<String, Integer> letterCounts = new HashMap<>();
            for (int r = 0; r < pattern.length(); r++) {
                String row = pattern.getString(r);
                for (char c : row.toCharArray()) {
                    if (c != ' ') {
                        letterCounts.merge(String.valueOf(c), 1, Integer::sum);
                    }
                }
            }

            for (String letter : letterCounts.keySet()) {
                if (!key.has(letter)) continue;
                String name = extractSingleItemName(key.get(letter));
                if (name == null) {
                    hadTagIngredient = true;
                    continue;
                }
                ingredientCounts.merge(name, letterCounts.get(letter), Integer::sum);
            }
        } else {
            skipped.add(recipeId + " (unrecognized crafting shape)");
            return;
        }

        if (ingredientCounts.isEmpty()) {
            skipped.add(recipeId + (hadTagIngredient ? " (only tag-based ingredients)" : " (no ingredients found)"));
            return;
        }

        StringBuilder ingredientsText = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : ingredientCounts.entrySet()) {
            if (!first) ingredientsText.append(",");
            ingredientsText.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }

        String line = resultName + "|" + recipeId + "|CRAFTING|" + outputCount + "|" + ingredientsText;
        outputLines.add(line);

        if (hadTagIngredient) {
            skipped.add(recipeId + " (partially converted — some tag-based ingredients were dropped, double check)");
        }
    }

    private static String extractSingleItemName(Object itemRepresentation) {
        if (itemRepresentation == null || itemRepresentation == JSONObject.NULL) {
            return null;
        }
        if (itemRepresentation instanceof JSONObject) {
            JSONObject obj = (JSONObject) itemRepresentation;
            if (obj.has("item")) {
                return obj.getString("item");
            }
            return null; // has "tag" instead, or unrecognized
        }
        if (itemRepresentation instanceof JSONArray) {
            // some versions allow a list of acceptable items for one slot; just take the first
            JSONArray arr = (JSONArray) itemRepresentation;
            if (arr.length() > 0) {
                return extractSingleItemName(arr.get(0));
            }
        }
        return null;
    }

    private static String extractResultName(Object resultRaw) {
        if (resultRaw instanceof String) {
            return (String) resultRaw;
        }
        if (resultRaw instanceof JSONObject) {
            JSONObject obj = (JSONObject) resultRaw;
            if (obj.has("id")) return obj.getString("id");
            if (obj.has("item")) return obj.getString("item");
        }
        return null;
    }

    private static int extractResultCount(Object resultRaw) {
        if (resultRaw instanceof JSONObject) {
            JSONObject obj = (JSONObject) resultRaw;
            if (obj.has("count")) return obj.getInt("count");
        }
        return 1;
    }
}