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

import java.io.File;

public final class Configs implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";
    public static class Generic {
        public static final ConfigInteger MAX_RISK_AMOUNT = new ConfigInteger("maxRiskAmount", 7, 1, 10, "How risky you want Bariton to be.\n For example how often you want it to parkour versus mine the block etc.");
        public static final ConfigBoolean MULTI_DIMENSIONAL_SIMULTANIOUSLY = new ConfigBoolean("multiDimensionalSimultainiously", false, "Do you want to get all the materials from a single dimension and then move to the next or all at the same time.");


        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                MAX_RISK_AMOUNT,
                MULTI_DIMENSIONAL_SIMULTANIOUSLY
        );
    }
    public static class Visuals {
        public static final ConfigBoolean PLACEHOLDER = new ConfigBoolean("placeHolder", false, "Placeholder Example");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PLACEHOLDER
        );
    }
    public static class InfoOverlays {
        public static final ConfigOptionList PLACEHOLDER = new ConfigOptionList("placeholder", HudAlignment.TOP_RIGHT, "placeholder overlay");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PLACEHOLDER
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

        if (configFile.exists() && configFile.isFile() && configFile.canRead())
        {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();

                ConfigUtils.readConfigBase(root, "Colors", Colors.OPTIONS);
                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
                ConfigUtils.readConfigBase(root, "InfoOverlays", InfoOverlays.OPTIONS);
                ConfigUtils.readConfigBase(root, "Visuals", Visuals.OPTIONS);
            }
        }
        //DataManager.setToolItem(Generic.TOOL_ITEM.getStringValue());
    }
    public static void saveToFile()
    {
        File dir = FileUtils.getConfigDirectory();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs())
        {
            JsonObject root = new JsonObject();

            ConfigUtils.writeConfigBase(root, "Colors", Colors.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            ConfigUtils.writeConfigBase(root, "InfoOverlays", InfoOverlays.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Visuals", Visuals.OPTIONS);

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