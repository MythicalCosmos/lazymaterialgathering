package net.lucy.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class Config {

    private Config() {
    }

    public static String schematicPath = "";
    public static String outputDirectory = "";

    /**
     * Preferred recipes.
     *
     * Key   = item/block ID
     * Value = preferred recipe ID
     *
     * Example:
     * minecraft:stone=minecraft:smelting
     */
    public static final Map<String, String> recipePreferences =
            new LinkedHashMap<>();

    public static void load(Path file) throws IOException {
        recipePreferences.clear();

        if (!Files.exists(file)) {
            save(file);
            return;
        }

        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }

        schematicPath =
                properties.getProperty("schematic_path", "");

        outputDirectory =
                properties.getProperty("output_directory", "");

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("recipe.")) {
                String itemId = key.substring("recipe.".length());

                String recipeId =
                        properties.getProperty(key, "");

                if (!itemId.isBlank() && !recipeId.isBlank()) {
                    recipePreferences.put(itemId, recipeId);
                }
            }
        }
    }

    public static void save(Path file) throws IOException {
        Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Properties properties = new Properties();

        properties.setProperty(
                "schematic_path",
                schematicPath == null ? "" : schematicPath
        );

        properties.setProperty(
                "output_directory",
                outputDirectory == null ? "" : outputDirectory
        );

        for (Map.Entry<String, String> entry :
                recipePreferences.entrySet()) {

            properties.setProperty(
                    "recipe." + entry.getKey(),
                    entry.getValue()
            );
        }

        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(
                    output,
                    "Lazy Material Gathering configuration"
            );
        }
    }

    public static void reset() {
        schematicPath = "";
        outputDirectory = "";
        recipePreferences.clear();
    }
}
