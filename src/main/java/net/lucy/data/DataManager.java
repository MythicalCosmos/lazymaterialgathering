package net.lucy.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.gui.interfaces.IDirectoryCache;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.lucy.Reference;
import net.lucy.calc.MiningResolver;
import net.lucy.calc.RawMaterials;
import net.lucy.config.Configs;
import net.lucy.gui.ConfigGuiTab;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holds state that must outlive a single screen.
 * Screens are created fresh each time they open, so anything they need to
 * remember (last folder used, the parsed schematic results) lives here instead.
 */
public class DataManager implements IDirectoryCache
{
    private static final DataManager INSTANCE = new DataManager();
    private static final String STORAGE_FILE_NAME = Reference.MOD_ID + "_data.json";

    // context name (e.g. "schematic_load") -> last folder the file browser was in
    private static final Map<String, File> LAST_DIRECTORIES = new HashMap<>();

    // Which tab of the settings screen was open last (memory only, resets to Generic on restart)
    private static ConfigGuiTab configGuiTab = ConfigGuiTab.GENERIC;

    // Results of the last parsed schematic. Kept in memory only, not saved.
    private static Map<String, Long> blockCounts = new TreeMap<>();
    private static Map<String, Long> rawMaterials = new TreeMap<>();

    private DataManager()
    {
    }

    // ---------- Remembered folders (used by malilib's file browser widgets) ----------

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
    public void setCurrentDirectoryForContext(String context, File dir)
    {
        LAST_DIRECTORIES.put(context, dir);
        save(); // changes are rare, so saving immediately is fine
    }

    // ---------- Folders ----------

    // The folder schematics are loaded from. This is the "schematicDirectory" setting,
    // or <minecraft folder>/schematics when the setting is empty or can't be used.
    public static File getSchematicsDirectory()
    {
        String custom = Configs.Generic.SCHEMATIC_DIRECTORY.getStringValue().trim();

        if (custom.isEmpty() == false)
        {
            File dir = new File(custom);

            if (dir.isDirectory() || dir.mkdirs())
            {
                return dir;
            }
        }

        File defaultDir = new File(FileUtils.getMinecraftDirectory(), "schematics");

        if (defaultDir.exists() == false)
        {
            defaultDir.mkdirs();
        }

        return defaultDir;
    }

    // If the browser last stood in a folder that is outside the current root folder
    // (for example after changing the setting), send it back to the root.
    public static void resetDirectoryIfOutside(String context, File root)
    {
        File last = LAST_DIRECTORIES.get(context);

        if (last != null && last.toPath().toAbsolutePath().normalize().startsWith(root.toPath().toAbsolutePath().normalize()) == false)
        {
            LAST_DIRECTORIES.put(context, root);
        }
    }

    // ---------- Settings screen tab ----------

    public static ConfigGuiTab getConfigGuiTab()
    {
        return configGuiTab;
    }

    public static void setConfigGuiTab(ConfigGuiTab tab)
    {
        configGuiTab = tab;
    }

    // ---------- Parsed schematic results ----------

    public static void setResults(Map<String, Long> newBlockCounts, Map<String, Long> newRawMaterials)
    {
        blockCounts = new TreeMap<>(newBlockCounts);
        rawMaterials = new TreeMap<>(newRawMaterials);
    }

    public static boolean hasResults()
    {
        return blockCounts.isEmpty() == false;
    }

    public static Map<String, Long> getBlockCounts()
    {
        return Collections.unmodifiableMap(blockCounts);
    }

    public static Map<String, Long> getRawMaterials()
    {
        return Collections.unmodifiableMap(rawMaterials);
    }

    // The blocks turned into the items you get from mining them (uses the Silk Touch setting)
    public static Map<String, Long> getMinedItems()
    {
        return MiningResolver.resolveMinedItems(blockCounts, Configs.Generic.USE_SILK_TOUCH.getBooleanValue());
    }

    // Work out the raw materials again, for example after a different recipe was picked
    public static void recalculateRawMaterials()
    {
        rawMaterials = new TreeMap<>(RawMaterials.calculate(getMinedItems()));
    }

    // ---------- Saving and loading ----------

    public static void load()
    {
        JsonElement element = JsonUtils.parseJsonFile(getStorageFile());

        if (element == null || element.isJsonObject() == false)
        {
            return;
        }

        JsonObject root = element.getAsJsonObject();
        LAST_DIRECTORIES.clear();

        if (JsonUtils.hasObject(root, "last_directories"))
        {
            JsonObject saved = root.getAsJsonObject("last_directories");

            for (Map.Entry<String, JsonElement> entry : saved.entrySet())
            {
                if (entry.getValue().isJsonPrimitive())
                {
                    File dir = new File(entry.getValue().getAsString());

                    // Skip folders that have been deleted since last time
                    if (dir.isDirectory())
                    {
                        LAST_DIRECTORIES.put(entry.getKey(), dir);
                    }
                }
            }
        }
    }

    public static void save()
    {
        JsonObject directories = new JsonObject();

        for (Map.Entry<String, File> entry : LAST_DIRECTORIES.entrySet())
        {
            directories.addProperty(entry.getKey(), entry.getValue().getAbsolutePath());
        }

        JsonObject root = new JsonObject();
        root.add("last_directories", directories);

        JsonUtils.writeJsonToFile(root, getStorageFile());
    }

    private static File getStorageFile()
    {
        File configDir = FileUtils.getConfigDirectory();

        if (configDir.exists() == false)
        {
            configDir.mkdirs();
        }

        return new File(configDir, STORAGE_FILE_NAME);
    }
}