package net.lucy.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.gui.interfaces.IDirectoryCache;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.lucy.Reference;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class DataManager implements IDirectoryCache {
    private static final DataManager INSTANCE = new DataManager();
    private static final String STORAGE_FILE_NAME = Reference.MOD_ID + "_data.json";

    private static final Map<String, File> LAST_DIRECTORIES = new HashMap<>();

    private static Map<String, Long> blockCounts = new TreeMap<>();
    private static Map<String, Long> rawMaterials = new TreeMap<>();

    private DataManager() {
    }


    public static IDirectoryCache getDirectoryCache()
    {
        return INSTANCE;
    }

    @Override
    @Nullable
    public File getCurrentDirectoryForContext(String context)
    {
        return LAST_DIRECTORIES.get(context);
    }

    @Override
    public void setCurrentDirectoryForContext(String context, File dir) {
        LAST_DIRECTORIES.put(context, dir);
        save();
    }

    public static void setResults(Map<String, Long> newBlockCounts, Map<String, Long> newRawMaterials) {
        blockCounts = new TreeMap<>(newBlockCounts);
        rawMaterials = new TreeMap<>(newRawMaterials);
    }

    public static boolean hasResults() {
        return blockCounts.isEmpty() == false;
    }

    public static Map<String, Long> getBlockCounts() {
        return Collections.unmodifiableMap(blockCounts);
    }

    public static Map<String, Long> getRawMaterials() {
        return Collections.unmodifiableMap(rawMaterials);
    }

    public static void load() {
        JsonElement element = JsonUtils.parseJsonFile(getStorageFile());

        if (element == null || element.isJsonObject() == false) {
            return;
        }

        JsonObject root = element.getAsJsonObject();
        LAST_DIRECTORIES.clear();

        if (JsonUtils.hasObject(root, "last_directories")) {
            JsonObject saved = root.getAsJsonObject("last_directories");

            for (Map.Entry<String, JsonElement> entry : saved.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    File dir = new File(entry.getValue().getAsString());

                    if (dir.isDirectory()) {
                        LAST_DIRECTORIES.put(entry.getKey(), dir);
                    }
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

        if (configDir.exists() == false) {
            configDir.mkdirs();
        }

        return new File(configDir, STORAGE_FILE_NAME);
    }
}