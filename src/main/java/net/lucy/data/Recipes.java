package net.lucy.data;

import fi.dy.masa.malilib.util.FileUtils;
import net.lucy.Reference;
import net.lucy.calc.RecipeExporter;
import net.lucy.model.Recipe;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Recipes {
    public static Map<String, List<Recipe>> recipes = new HashMap<>();

    // True once "recipes" holds data read from a cached file rather than the live world
    // (used by the Preferred Recipes screen to say so).
    private static boolean fromCache = false;

    /**
     * Makes sure a recipe list is available, so recipes can be reviewed and preferences
     * changed even without a world open:
     *  - In a world: always re-reads from the world (picks up datapacks), and saves a
     *    copy to the config folder for next time.
     *  - No world, but a copy was saved before: reads that copy.
     *  - No world, and nothing saved yet: recipes stays empty. Join a world once first.
     */
    public static void ensureLoaded() {
        if (MinecraftClient.getInstance().world != null) {
            recipes = RecipeExporter.collect();
            fromCache = false;
            saveCache();
            return;
        }

        if (recipes.isEmpty()) {
            loadCache();
        }
    }

    public static boolean isFromCache() {
        return fromCache;
    }

    public static boolean hasCache() {
        return getCacheFile().toFile().isFile();
    }

    private static void saveCache() {
        try {
            RecipeExporter.exportAll(getCacheFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadCache() {
        Path file = getCacheFile();

        if (Files.isRegularFile(file)) {
            try {
                recipes = RecipeFileLoader.loadRecipes(file);
                fromCache = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Path getCacheFile() {
        File dir = FileUtils.getConfigDirectory();
        return new File(dir, Reference.MOD_ID + "_recipes.txt").toPath();
    }
}