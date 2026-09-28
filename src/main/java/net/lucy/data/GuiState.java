package net.lucy.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.gui.interfaces.IDirectoryCache;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.lucy.Reference;
import net.lucy.config.Configs;
import net.lucy.gui.ConfigGuiTab;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class GuiState implements IDirectoryCache {
    private static final GuiState INSTANCE = new GuiState();
    private static final String STORAGE_FILE_NAME = Reference.MOD_ID + "_data.json";
    // File-browser context -> last directory
    private static final Map<String, File> LAST_DIRECTORIES = new HashMap<>();
    private static ConfigGuiTab configGuiTab = ConfigGuiTab.GENERIC;
    private static String baritoneTabName = "Movement";
    private GuiState() {
    }

    public static IDirectoryCache getDirectoryCache() {
        return INSTANCE;
    }

    @Override
    @Nullable
    public File getCurrentDirectoryForContext(String context) {
        return LAST_DIRECTORIES.get(context);
    }

    @Override
    public void setCurrentDirectoryForContext(String context, File dir) {
        LAST_DIRECTORIES.put(context, dir);
        save();
    }

    public static File getSchematicsDirectory() {
        String custom = Configs.Generic.SCHEMATIC_DIRECTORY.getStringValue().trim();
        if (!custom.isEmpty()) {
            File dir = new File(custom);
            if (dir.isDirectory() || dir.mkdirs()) {
                return dir;
            }
        }

        File defaultDir = new File(FileUtils.getMinecraftDirectory(), "schematics");
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }

        return defaultDir;
    }

    public static void resetDirectoryIfOutside(String context, File root) {
        File last = LAST_DIRECTORIES.get(context);
        if (last != null && !last.toPath().toAbsolutePath().normalize().startsWith(root.toPath().toAbsolutePath().normalize())) {
            LAST_DIRECTORIES.put(context, root);
        }
    }

    public static ConfigGuiTab getConfigGuiTab() {
        return configGuiTab;
    }

    public static void setConfigGuiTab(ConfigGuiTab tab) {
        configGuiTab = tab;
    }

    public static String getBaritoneTabName() {
        return baritoneTabName;
    }

    public static void setBaritoneTabName(String tabName) {
        baritoneTabName = tabName;
    }

    public static void load() {
        JsonElement element = JsonUtils.parseJsonFile(getStorageFile());
        if (element == null || !element.isJsonObject()) {
            return;
        }

        JsonObject root = element.getAsJsonObject();
        LAST_DIRECTORIES.clear();
        if (JsonUtils.hasObject(root, "last_directories")) {
            JsonObject saved = root.getAsJsonObject("last_directories");

            for (Map.Entry<String, JsonElement> entry : saved.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) {
                    continue;
                }

                File dir = new File(entry.getValue().getAsString());

                if (dir.isDirectory()) {
                    LAST_DIRECTORIES.put(entry.getKey(), dir);
                }
            }
        }
    }

    public static void save() {
        JsonObject directories = new JsonObject();
        for (Map.Entry<String, File> entry : LAST_DIRECTORIES.entrySet()) {
            directories.addProperty(entry.getKey(), entry.getValue().getAbsolutePath());
        }

        JsonObject root = new JsonObject();
        root.add("last_directories", directories);
        JsonUtils.writeJsonToFile(root, getStorageFile());
    }

    private static File getStorageFile() {
        File configDir = FileUtils.getConfigDirectory();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        return new File(configDir, STORAGE_FILE_NAME);
    }
}