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

public final class Recipes {
    public static Map<String, List<Recipe>> recipes = new HashMap<>();
    private static boolean fromCache = false;
    private static boolean worldClosed = false;
    private Recipes() {
    }

    public static void refreshFromWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        Map<String, List<Recipe>> collected = RecipeExporter.collect();
        /*
         * Critical protection:
         *
         * Never replace a good cache with an empty recipe map.
         *
         * This can happen during disconnect/world shutdown when Minecraft's
         * RecipeManager is temporarily empty.
         */
        if (collected == null || collected.isEmpty()) {
            System.out.println("[LMG] Recipe refresh returned no recipes; keeping existing cache.");
            return;
        }

        recipes = collected;
        fromCache = false;
        worldClosed = false;
        saveCache();
    }

    /**
     * Called when the client disconnects from a world.
     *
     * We deliberately do not export recipes here. The last known recipe
     * database remains intact.
     */
    public static void markWorldClosed() {
        worldClosed = true;
    }

    public static boolean isWorldClosed() {
        return worldClosed;
    }

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
        File dir = FileUtils.getConfigDirectory();
        Path dirs = dir.toPath().resolve("LazyMaterialGathering");
        try {
            Files.createDirectories(dirs);
        } catch (IOException e) {
            System.err.println("[LMG] Could not create recipe cache directory: " + e.getMessage());
        }

        return dirs.resolve(Reference.MOD_ID + "_recipes.txt");
    }
}