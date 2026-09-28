package net.lucy.calc;

import net.lucy.mixin.BrewingRecipeAccessor;
import net.lucy.mixin.BrewingRecipeRegistryAccessor;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
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

public final class RecipeExporter {
    private RecipeExporter() {
    }

    public static Map<String, List<Recipe>> collect() {
        Map<String, List<Recipe>> result = new HashMap<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return result;
        }

        RecipeManager recipeManager = client.world.getRecipeManager();
        /*
         * Use recipe IDs directly from RecipeManager.
         *
         * RecipeManager.keys() is public in Minecraft 1.20.1 and returns
         * every loaded recipe ID, including datapack/mod recipes.
         */
        recipeManager.keys().sorted().forEach(recipeId -> {recipeManager.get(recipeId).ifPresent(recipe -> {exportMinecraftRecipe(result, recipeId, recipe, client);});});
        /*
         * Brewing recipes are not stored in RecipeManager in 1.20.1.
         * They live in BrewingRecipeRegistry instead.
         */
        exportBrewingRecipes(result);
        for (List<Recipe> options : result.values()) {
            options.sort(Comparator.comparing(recipe -> recipe.id));
        }
        return result;
    }

    private static void exportMinecraftRecipe(Map<String, List<Recipe>> result, Identifier recipeId, net.minecraft.recipe.Recipe<?> minecraftRecipe, MinecraftClient client) {
        RecipeType type = classify(minecraftRecipe);
        if (type == null) {
            return;
        }

        ItemStack output = minecraftRecipe.getOutput(client.world.getRegistryManager());
        if (output == null || output.isEmpty()) {
            return;
        }

        List<List<String>> ingredientChoices = buildIngredientChoices(minecraftRecipe);
        /*
         * Some special/dynamic recipes have no normal ingredient list.
         * Do not create a recipe that magically produces an item for free.
         */
        if (ingredientChoices.isEmpty()) {
            return;
        }

        Map<String, Integer> ingredients = countFirstChoices(ingredientChoices);
        if (ingredients.isEmpty()) {
            return;
        }

        String outputName = plainName(Registries.ITEM.getId(output.getItem()));
        Recipe entry = new Recipe(recipeId.toString(), type, ingredients, ingredientChoices, Math.max(1, output.getCount()));
        if (type == RecipeType.CRAFTING) {
            entry.gridSlots = buildGridSlots(minecraftRecipe);
        }

        result.computeIfAbsent(outputName, ignored -> new ArrayList<>()).add(entry);
    }

    private static List<List<String>> buildIngredientChoices(net.minecraft.recipe.Recipe<?> recipe) {
        List<List<String>> result = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient == null || ingredient.isEmpty()) {
                continue;
            }

            List<String> choices = new ArrayList<>();
            for (ItemStack stack : ingredient.getMatchingStacks()) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                String name = plainName(Registries.ITEM.getId(stack.getItem()));
                if (!choices.contains(name)) {
                    choices.add(name);
                }
            }
            if (!choices.isEmpty()) {
                result.add(choices);
            }
        }
        return result;
    }

    private static Map<String, Integer> countFirstChoices(List<List<String>> choices) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (List<String> slot : choices) {
            if (slot == null || slot.isEmpty()) {
                continue;
            }

            result.merge(slot.get(0), 1, Integer::sum);
        }
        return result;
    }

    private static String[] buildGridSlots(net.minecraft.recipe.Recipe<?> recipe) {
        String[] slots = new String[9];
        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();
            List<Ingredient> pattern = shaped.getIngredients();
            for (int row = 0; row < height && row < 3; row++) {
                for (int col = 0; col < width && col < 3; col++) {
                    int index = row * width + col;
                    if (index >= pattern.size()) {
                        continue;
                    }
                    slots[row * 3 + col] = firstMatchName(pattern.get(index));
                }
            }
            return slots;
        }

        /*
         * Shapeless recipes do not have a real layout.
         * Pack their ingredients in reading order.
         */
        int index = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (index >= 9) {
                break;
            }

            String name = firstMatchName(ingredient);
            if (name != null) {
                slots[index++] = name;
            }
        }
        return slots;
    }

    private static String firstMatchName(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return null;
        }

        ItemStack[] matching = ingredient.getMatchingStacks();
        if (matching.length == 0) {
            return null;
        }

        ItemStack stack = matching[0];
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        return plainName(Registries.ITEM.getId(stack.getItem()));
    }

    private static RecipeType classify(net.minecraft.recipe.Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe
                || recipe instanceof ShapelessRecipe) {
            return RecipeType.CRAFTING;
        }

        if (recipe instanceof BlastingRecipe) {
            return RecipeType.BLASTING;
        }

        if (recipe instanceof SmokingRecipe) {
            return RecipeType.SMOKING;
        }

        if (recipe instanceof CampfireCookingRecipe) {
            return RecipeType.CAMPFIRE;
        }

        if (recipe instanceof SmeltingRecipe) {
            return RecipeType.SMELTING;
        }

        if (recipe instanceof StonecuttingRecipe) {
            return RecipeType.STONECUTTING;
        }

        if (recipe instanceof SmithingRecipe) {
            return RecipeType.SMITHING;
        }

        return RecipeType.OTHER;
    }

    /*
     * ----------------------------------------------------------------------
     * Brewing
     * ----------------------------------------------------------------------
     *
     * Minecraft 1.20.1 keeps brewing recipes in BrewingRecipeRegistry rather
     * than RecipeManager.
     *
     * Potion keys use:
     *
     *     potion/minecraft/awkward
     *
     * instead of "potion:minecraft:awkward" so the cache format remains
     * compatible with the existing colon-separated ingredient syntax.
     */
    private static void exportBrewingRecipes(Map<String, List<Recipe>> result) {
        exportPotionBrewingRecipes(result);
        exportItemBrewingRecipes(result);
    }

    private static void exportPotionBrewingRecipes(Map<String, List<Recipe>> result) {
        List<?> recipes = BrewingRecipeRegistryAccessor.getPotionRecipes();
        for (Object rawRecipe : recipes) {
            BrewingRecipeAccessor recipe = (BrewingRecipeAccessor) rawRecipe;
            Object inputObject = recipe.lmg$getInput();
            Object outputObject = recipe.lmg$getOutput();
            if (!(inputObject instanceof Potion input) || !(outputObject instanceof Potion output)) {
                continue;
            }

            String ingredient = firstMatchName(recipe.lmg$getIngredient());
            if (ingredient == null) {
                continue;
            }

            String inputKey = potionKey(input);
            String outputKey = potionKey(output);
            /*
             * One brewing operation consumes one ingredient and produces
             * three potion bottles.
             *
             * Three copies of the potion input are therefore represented
             * as three ingredient slots.
             */
            List<List<String>> choices = new ArrayList<>();
            choices.add(List.of(inputKey));
            choices.add(List.of(inputKey));
            choices.add(List.of(inputKey));
            choices.add(List.of(ingredient));
            Map<String, Integer> ingredients = countFirstChoices(choices);
            String id = "brewing/potion/" + inputKey + "_" + ingredient + "_to_" + outputKey;
            Recipe recipeEntry = new Recipe(id, RecipeType.BREWING, ingredients, choices, 3);
            result.computeIfAbsent(outputKey, ignored -> new ArrayList<>()).add(recipeEntry);
        }
    }

    private static void exportItemBrewingRecipes(Map<String, List<Recipe>> result) {
        List<?> recipes = BrewingRecipeRegistryAccessor.getItemRecipes();
        for (Object rawRecipe : recipes) {
            BrewingRecipeAccessor recipe = (BrewingRecipeAccessor) rawRecipe;
            Object inputObject = recipe.lmg$getInput();
            Object outputObject = recipe.lmg$getOutput();
            if (!(inputObject instanceof Item input) || !(outputObject instanceof Item output)) {
                continue;
            }

            String ingredient = firstMatchName(recipe.lmg$getIngredient());
            if (ingredient == null) {
                continue;
            }

            String inputName = plainName(Registries.ITEM.getId(input));
            String outputName = plainName(Registries.ITEM.getId(output));
            List<List<String>> choices = List.of(List.of(inputName), List.of(ingredient));
            Map<String, Integer> ingredients = countFirstChoices(choices);
            String id = "brewing/item/" + inputName + "_" + ingredient + "_to_" + outputName;
            Recipe recipeEntry = new Recipe(id, RecipeType.BREWING, ingredients, choices, 1);
            result.computeIfAbsent(outputName, ignored -> new ArrayList<>()).add(recipeEntry);
        }
    }

    private static String potionKey(Potion potion) {
        Identifier id = Registries.POTION.getId(potion);
        if (id == null) {
            return "potion/unknown";
        }

        return "potion/" + id.getNamespace() + "/" + id.getPath();
    }

    private static String plainName(Identifier id) {
        if (id == null) {
            return null;
        }

        if ("minecraft".equals(id.getNamespace())) {
            return id.getPath();
        }
        return id.toString();
    }

    public static void exportAll(Path outputFile) throws IOException {
        Map<String, List<Recipe>> recipes = collect();
        List<String> lines = new ArrayList<>();
        List<String> itemNames = new ArrayList<>(recipes.keySet());
        itemNames.sort(String::compareTo);
        for (String itemName : itemNames) {
            List<Recipe> options = recipes.get(itemName);
            for (Recipe recipe : options) {
                lines.add(serializeRecipe(itemName, recipe));
            }
        }
        Files.write(outputFile, lines);
    }

    private static String serializeRecipe(String itemName, Recipe recipe) {
        StringBuilder ingredients = new StringBuilder();
        for (Map.Entry<String, Integer> entry : recipe.ingredients.entrySet()) {
            if (ingredients.length() > 0) {
                ingredients.append(",");
            }

            ingredients.append(entry.getKey()).append(":").append(entry.getValue());
        }

        StringBuilder choices = new StringBuilder();
        for (List<String> slot : recipe.getEffectiveIngredientChoices()) {
            if (choices.length() > 0) {
                choices.append(",");
            }

            for (int i = 0; i < slot.size(); i++) {if (i > 0) {
                    choices.append("~");
                }
                choices.append(slot.get(i));
            }
        }

        return itemName + "|" + recipe.id + "|" + recipe.type + "|" + recipe.outputCount + "|" + ingredients + "|" + choices;
    }
}