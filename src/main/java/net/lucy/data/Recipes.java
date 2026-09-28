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

public class Recipes
{
    public static Map<String, List<Recipe>> recipes =
            new HashMap<>();

    private static boolean fromCache = false;

    /**
     * Refreshes the recipe database from the current Minecraft world.
     *
     * This should be called after the client has joined a world/server.
     */
    public static void refreshFromWorld()
    {
        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.world == null)
        {
            return;
        }

        recipes =
                RecipeExporter.collect();

        fromCache = false;

        saveCache();
    }

    /**
     * Makes sure recipes are available.
     */
    public static void ensureLoaded()
    {
        if (
                MinecraftClient.getInstance().world
                        != null
        )
        {
            refreshFromWorld();
            return;
        }

        if (recipes.isEmpty())
        {
            loadCache();
        }
    }

    public static boolean isFromCache()
    {
        return fromCache;
    }

    public static boolean hasCache()
    {
        return Files.isRegularFile(
                getCacheFile()
        );
    }

    private static void saveCache()
    {
        try
        {
            RecipeExporter.exportAll(
                    getCacheFile()
            );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void loadCache()
    {
        Path file =
                getCacheFile();

        if (!Files.isRegularFile(file))
        {
            return;
        }

        try
        {
            recipes =
                    RecipeFileLoader.loadRecipes(
                            file
                    );

            fromCache = true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static Path getCacheFile()
    {
        File dir =
                FileUtils.getConfigDirectory();

        Path dirs =
                dir.toPath()
                        .resolve(
                                "LazyMaterialGathering"
                        );

        try
        {
            Files.createDirectories(dirs);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return dirs.resolve(
                Reference.MOD_ID
                        + "_recipes.txt"
        );
    }
}