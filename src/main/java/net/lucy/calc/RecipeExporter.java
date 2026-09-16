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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecipeExporter {

    public static void exportAll(Path outputFile) throws IOException {
        MinecraftClient client = MinecraftClient.getInstance();
        RecipeManager recipeManager = client.world.getRecipeManager();

        List<String> lines = new ArrayList<>();

        for (Recipe<?> recipe : recipeManager.values()) {
            RecipeType lineType = classify(recipe);
            if (lineType == null) continue;

            Identifier id = recipe.getId();

            ItemStack output = recipe.getOutput(client.world.getRegistryManager());
            if (output.isEmpty()) continue;

            String outputName = Registries.ITEM.getId(output.getItem()).toString();
            int outputCount = output.getCount();

            Map<String, Integer> ingredientCounts = new LinkedHashMap<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                ItemStack[] matching = ingredient.getMatchingStacks();
                if (matching.length == 0) continue;

                String ingredientName = Registries.ITEM.getId(matching[0].getItem()).toString();
                ingredientCounts.merge(ingredientName, 1, Integer::sum);
            }

            if (ingredientCounts.isEmpty()) continue;

            StringBuilder ingredientsText = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, Integer> ing : ingredientCounts.entrySet()) {
                if (!first) ingredientsText.append(",");
                ingredientsText.append(ing.getKey()).append(":").append(ing.getValue());
                first = false;
            }

            lines.add(outputName + "|" + id + "|" + lineType + "|" + outputCount + "|" + ingredientsText);
        }

        Files.write(outputFile, lines);
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