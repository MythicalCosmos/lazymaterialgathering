package net.lucy.calc;

import net.lucy.model.RecipeType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecipeExporter {

    /**
     * Reads every crafting and smelting recipe from the world the player is in.
     * Returns them keyed by the item they make, using the same plain names as the schematic
     * ("oak_planks", not "minecraft:oak_planks"). Empty if the player isn't in a world.
     */
    public static Map<String, List<net.lucy.model.Recipe>> collect() {
        Map<String, List<net.lucy.model.Recipe>> result = new HashMap<>();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return result;
        }

        RecipeManager recipeManager = client.world.getRecipeManager();

        for (Recipe<?> recipe : recipeManager.values()) {
            RecipeType type = classify(recipe);
            if (type == null) continue;

            ItemStack output = recipe.getOutput(client.world.getRegistryManager());
            if (output.isEmpty()) continue;

            // Count the ingredients. When an ingredient accepts several items (like "any planks"),
            // the first one it lists is used.
            Map<String, Integer> ingredients = new LinkedHashMap<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                ItemStack[] matching = ingredient.getMatchingStacks();
                if (matching.length == 0) continue;

                ingredients.merge(plainName(Registries.ITEM.getId(matching[0].getItem())), 1, Integer::sum);
            }
            if (ingredients.isEmpty()) continue;

            String outputName = plainName(Registries.ITEM.getId(output.getItem()));
            net.lucy.model.Recipe entry = new net.lucy.model.Recipe(plainName(recipe.getId()), type, ingredients, output.getCount());

            result.computeIfAbsent(outputName, name -> new ArrayList<>()).add(entry);
        }

        // Sort by recipe id so the first recipe (the default) is always the same one
        for (List<net.lucy.model.Recipe> options : result.values()) {
            options.sort(Comparator.comparing((net.lucy.model.Recipe option) -> option.id));
        }

        return result;
    }

    // Writes the recipes to a text file that RecipeFileLoader can read back
    public static void exportAll(Path outputFile) throws IOException {
        List<String> lines = new ArrayList<>();

        for (Map.Entry<String, List<net.lucy.model.Recipe>> entry : collect().entrySet()) {
            for (net.lucy.model.Recipe recipe : entry.getValue()) {
                StringBuilder ingredientsText = new StringBuilder();
                for (Map.Entry<String, Integer> ingredient : recipe.ingredients.entrySet()) {
                    if (ingredientsText.length() > 0) ingredientsText.append(",");
                    ingredientsText.append(ingredient.getKey()).append(":").append(ingredient.getValue());
                }

                lines.add(entry.getKey() + "|" + recipe.id + "|" + recipe.type + "|" + recipe.outputCount + "|" + ingredientsText);
            }
        }

        Files.write(outputFile, lines);
    }

    // "minecraft:oak_planks" -> "oak_planks". Items from other mods keep their prefix.
    private static String plainName(Identifier id) {
        return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
    }

    private static RecipeType classify(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
            return RecipeType.CRAFTING;
        }
        if (recipe instanceof AbstractCookingRecipe) {
            return RecipeType.SMELTING;
        }
        return null;
    }
}