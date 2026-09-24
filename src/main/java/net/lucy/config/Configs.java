package net.lucy.config;

import com.google.common.collect.ImmutableList;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.lucy.Reference;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.File;

public final class Configs implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

    // item name -> id of the recipe used for that item's raw materials, when more than one is enabled
    public static final Map<String, String> recipePreferences = new HashMap<>();

    // item name -> ids of recipes the player has turned OFF for that item.
    // A recipe with no entry here (or an empty set) counts as enabled: new recipes start enabled.
    public static final Map<String, Set<String>> disabledRecipes = new HashMap<>();

    // Is this recipe currently enabled for this item?
    public static boolean isRecipeEnabled(String itemName, String recipeId) {
        Set<String> disabled = disabledRecipes.get(itemName);
        return disabled == null || disabled.contains(recipeId) == false;
    }

    // Turn a recipe on or off. Refuses to disable the last enabled recipe for an item.
    public static boolean setRecipeEnabled(String itemName, String recipeId, boolean enabled) {
        if (enabled) {
            Set<String> disabled = disabledRecipes.get(itemName);
            if (disabled != null) {
                disabled.remove(recipeId);
            }
            return true;
        }

        Set<String> disabled = disabledRecipes.computeIfAbsent(itemName, key -> new HashSet<>());
        disabled.add(recipeId);
        return true;
    }

    public static class Generic {
        public static final ConfigInteger MAX_RISK_AMOUNT = new ConfigInteger("maxRiskAmount", 7, 1, 10, "How risky you want Bariton to be.\n For example how often you want it to parkour versus mine the block etc.");
        public static final ConfigBoolean MULTI_DIMENSIONAL_SIMULTANIOUSLY = new ConfigBoolean("multiDimensionalSimultainiously", false, "Do you want to get all the materials from a single dimension and then move to the next or all at the same time.");
        public static final ConfigString OUTPUT_DIRECTORY = new ConfigString("outputDirectory", "", "Folder where the block and material reports are saved. Leave empty to use the game folder.");
        public static final ConfigString SCHEMATIC_DIRECTORY = new ConfigString("schematicDirectory", "", "Folder the schematic browser opens in. Leave empty to use the 'schematics' folder in the game folder.");
        public static final ConfigBoolean USE_SILK_TOUCH = new ConfigBoolean("useSilkTouch", true, "Assume you have a Silk Touch tool. If off, blocks that need it (glass, ice) are left out and grass blocks give dirt.");



        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                MAX_RISK_AMOUNT,
                MULTI_DIMENSIONAL_SIMULTANIOUSLY,
                OUTPUT_DIRECTORY,
                SCHEMATIC_DIRECTORY,
                USE_SILK_TOUCH
        );
    }
    public static class Visuals {
        public static final ConfigBoolean PLACEHOLDER = new ConfigBoolean("placeHolder", false, "Placeholder Example");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PLACEHOLDER
        );
    }
    public static class InfoOverlays {
        public static final ConfigBoolean INFO_OVERLAY_ENABLED = new ConfigBoolean("infoOverlayEnabled", true, "Show the progress info overlay");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                INFO_OVERLAY_ENABLED
        );

    }
    public static class Colors {
        public static final ConfigColor PLACEHOLDER_COLOR= new ConfigColor("placeholderColor", "#30FFFFFF", "placeholder or sum shit");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PLACEHOLDER_COLOR
        );
    }
    public static void loadFromFile() {
        File configFile = new File(FileUtils.getConfigDirectory(), CONFIG_FILE_NAME);

        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();

                ConfigUtils.readConfigBase(root, "Colors", Colors.OPTIONS);
                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
                ConfigUtils.readConfigBase(root, "InfoOverlays", InfoOverlays.OPTIONS);
                ConfigUtils.readConfigBase(root, "Visuals", Visuals.OPTIONS);

                recipePreferences.clear();
                if (root.has("RecipePreferences") && root.get("RecipePreferences").isJsonObject()) {
                    JsonObject saved = root.getAsJsonObject("RecipePreferences");
                    for (Map.Entry<String, JsonElement> entry : saved.entrySet()) {
                        recipePreferences.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }

                disabledRecipes.clear();
                if (root.has("DisabledRecipes") && root.get("DisabledRecipes").isJsonObject()) {
                    JsonObject saved = root.getAsJsonObject("DisabledRecipes");
                    for (Map.Entry<String, JsonElement> entry : saved.entrySet()) {
                        Set<String> ids = new HashSet<>();
                        for (JsonElement id : entry.getValue().getAsJsonArray()) {
                            ids.add(id.getAsString());
                        }
                        disabledRecipes.put(entry.getKey(), ids);
                    }
                }
            }
        }
    }
    public static void saveToFile() {
        File dir = FileUtils.getConfigDirectory();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            JsonObject root = new JsonObject();

            ConfigUtils.writeConfigBase(root, "Colors", Colors.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            ConfigUtils.writeConfigBase(root, "InfoOverlays", InfoOverlays.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Visuals", Visuals.OPTIONS);

            JsonObject preferences = new JsonObject();
            for (Map.Entry<String, String> entry : recipePreferences.entrySet()) {
                preferences.addProperty(entry.getKey(), entry.getValue());
            }
            root.add("RecipePreferences", preferences);

            JsonObject disabled = new JsonObject();
            for (Map.Entry<String, Set<String>> entry : disabledRecipes.entrySet()) {
                if (entry.getValue().isEmpty()) continue;
                com.google.gson.JsonArray ids = new com.google.gson.JsonArray();
                for (String id : entry.getValue()) {
                    ids.add(id);
                }
                disabled.add(entry.getKey(), ids);
            }
            root.add("DisabledRecipes", disabled);

            JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
        }
    }

    @Override
    public void load()
    {
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }
}