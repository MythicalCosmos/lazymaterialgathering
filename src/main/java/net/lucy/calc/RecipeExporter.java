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

            if (type == RecipeType.CRAFTING) {
                entry.gridSlots = buildGridSlots(recipe);
            }

            result.computeIfAbsent(outputName, name -> new ArrayList<>()).add(entry);
        }

        // Sort by recipe id so the first recipe (the default) is always the same one
        for (List<net.lucy.model.Recipe> options : result.values()) {
            options.sort(Comparator.comparing((net.lucy.model.Recipe option) -> option.id));
        }

        return result;
    }

    // The 9 crafting-grid slots (top-left to bottom-right), so a recipe can be drawn the
    // way it actually looks in a crafting table instead of just as a plain ingredient list.
    private static String[] buildGridSlots(Recipe<?> recipe) {
        String[] slots = new String[9];

        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();
            List<Ingredient> pattern = shaped.getIngredients();

            // Placed top-left aligned within the 3x3 grid; the game itself doesn't record
            // where within the grid a shaped recipe was actually placed, only its own
            // width/height, so this is the same "top-left" convention most recipe viewers use.
            for (int row = 0; row < height && row < 3; row++) {
                for (int col = 0; col < width && col < 3; col++) {
                    int patternIndex = row * width + col;
                    if (patternIndex >= pattern.size()) continue;

                    slots[row * 3 + col] = firstMatchName(pattern.get(patternIndex));
                }
            }
        }
        else {
            // Shapeless: the game doesn't record any layout at all, so this just packs the
            // ingredients in order, left to right, top to bottom (an approximation).
            int index = 0;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (index >= 9) break;
                String name = firstMatchName(ingredient);
                if (name != null) {
                    slots[index++] = name;
                }
            }
        }

        return slots;
    }

    private static String firstMatchName(Ingredient ingredient) {
        if (ingredient.isEmpty()) return null;
        ItemStack[] matching = ingredient.getMatchingStacks();
        if (matching.length == 0) return null;
        return plainName(Registries.ITEM.getId(matching[0].getItem()));
    }

    // Writes the recipes to a text file that RecipeFileLoader can read back.
    // Grid layout isn't saved here (only needed live, to keep this simple text format);
    // recipes loaded from this file still work everywhere else, they just show as a plain
    // ingredient list instead of a crafting grid on the Raw Materials screen.
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