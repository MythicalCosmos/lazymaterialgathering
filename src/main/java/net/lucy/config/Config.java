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

    private static Path configPath;

    public static String schematicPath = "";
    public static String outputDirectory = "";
    public static boolean silkTouch = false;
    public static int minWaterLevel = 8;

    public static final Map<String, String> recipePreferences = new LinkedHashMap<>();

    public static Path getConfigPath() {
        return configPath;
    }

    public static void load(Path file) throws IOException {
        configPath = file;
        recipePreferences.clear();

        if (!Files.exists(file)) {
            save(file);
            return;
        }

        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }

        schematicPath = properties.getProperty("schematic_path", "");
        outputDirectory = properties.getProperty("output_directory", "");
        silkTouch = Boolean.parseBoolean(properties.getProperty("silk_touch", "false"));

        try {
            minWaterLevel = Integer.parseInt(properties.getProperty("min_water_level", "8"));
        } catch (NumberFormatException e) {
            minWaterLevel = 8;
        }

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("recipe.")) {
                String itemId = key.substring("recipe.".length());
                String recipeId = properties.getProperty(key, "");

                if (!itemId.isBlank() && !recipeId.isBlank()) {
                    recipePreferences.put(itemId, recipeId);
                }
            }
        }
    }

    public static void save(Path file) throws IOException {
        configPath = file;
        Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Properties properties = new Properties();

        properties.setProperty("schematic_path", schematicPath == null ? "" : schematicPath);
        properties.setProperty("output_directory", outputDirectory == null ? "" : outputDirectory);
        properties.setProperty("silk_touch", String.valueOf(silkTouch));
        properties.setProperty("min_water_level", String.valueOf(minWaterLevel));

        for (Map.Entry<String, String> entry : recipePreferences.entrySet()) {
            properties.setProperty("recipe." + entry.getKey(), entry.getValue());
        }

        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output, "Lazy Material Gathering configuration");
        }
    }

    public static void reset() {
        schematicPath = "";
        outputDirectory = "";
        silkTouch = false;
        minWaterLevel = 8;
        recipePreferences.clear();
    }
}