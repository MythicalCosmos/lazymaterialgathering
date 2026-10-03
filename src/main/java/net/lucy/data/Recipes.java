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

/**
 * Persistent recipe database.
 *
 * The live Minecraft RecipeManager is preferred while a world is open.
 * A previously exported database is retained when no world is available.
 *
 * Important:
 * We never overwrite a valid recipe cache with an empty collection.
 */
public final class Recipes {
    public static Map<String, List<Recipe>> recipes = new HashMap<>();
    private static boolean fromCache = false;
    private static boolean worldClosed = false;
    private Recipes() {
    }

    /**
     * Refreshes recipes from the currently loaded Minecraft world.
     */
    public static void refreshFromWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        Map<String, List<Recipe>> collected = RecipeExporter.collect();
        /*
         * Never destroy a good database because RecipeManager is
         * temporarily empty during world initialization/disconnect.
         */
        if (collected == null || collected.isEmpty()) {
            System.out.println("[LMG] Recipe refresh returned no recipes; keeping existing recipe database.");
            return;
        }

        recipes = collected;
        fromCache = false;
        worldClosed = false;
        saveCache();
    }

    /**
     * Called when the client leaves a world.
     *
     * We intentionally do not clear the recipes here.
     */
    public static void markWorldClosed() {
        worldClosed = true;
    }

    public static boolean isWorldClosed() {
        return worldClosed;
    }

    /**
     * Makes sure a usable recipe database exists.
     *
     * If a world is currently open, use its live recipes.
     * Otherwise use the persistent cache.
     */
    public static void ensureLoaded() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            if (recipes.isEmpty()) {
                refreshFromWorld();
            }
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
        return Files.isRegularFile(getCacheFile());
    }

    private static void saveCache() {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        try {
            RecipeExporter.exportAll(getCacheFile());
        } catch (IOException e) {
            System.err.println("[LMG] Could not save recipe cache: " + e.getMessage());
        }
    }

    private static void loadCache() {
        Path file = getCacheFile();
        if (!Files.isRegularFile(file)) {
            return;
        }

        try {
            Map<String, List<Recipe>> loaded = RecipeFileLoader.loadRecipes(file);
            if (loaded == null || loaded.isEmpty()) {
                System.err.println("[LMG] Recipe cache was empty; ignoring it.");
                return;
            }
            recipes = loaded;
            fromCache = true;
        } catch (IOException e) {
            System.err.println("[LMG] Could not load recipe cache: " + e.getMessage());
        }
    }

    private static Path getCacheFile() {
        File configDirectory = FileUtils.getConfigDirectory();
        Path directory = configDirectory.toPath().resolve("LazyMaterialGathering");
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            System.err.println("[LMG] Could not create recipe cache directory: " + e.getMessage());
        }

        return directory.resolve(Reference.MOD_ID + "_recipes.txt");
    }
}