package net.lucy.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

public class Hotkeys {
    public static final ConfigHotkey OPEN_GUI_MAIN_MENU = new ConfigHotkey("openGuiMainMenu","RIGHT_SHIFT", KeybindSettings.RELEASE_EXCLUSIVE, "Opens the  main menu");
    public static final ConfigHotkey OPEN_GUI_MATERIAL_LIST = new ConfigHotkey("openGuiMaterialList", "RIGHT_SHIFT,L", "Opens the material List");
    public static final ConfigHotkey OPEN_GUI_SELECTION_MANAGER = new ConfigHotkey("openGuiSelectionManager", "RIGHT_SHIFT,S","Opens the litematic selection screen.");
    public static final ConfigHotkey OPEN_GUI_SETTINGS = new ConfigHotkey("openGuiSettings", "RIGHT_SHIFT,C", "Opens the settings menu");
    public static final ConfigHotkey OPEN_GUI_BARITONE_SETTINGS = new ConfigHotkey("openGuiBaritoneSettings", "RIGHT_SHIFT,D", "Opens the settings menu for Baritone");
    public static final ConfigHotkey TOGGLE_INFO_OVERLAY_RENDERING = new ConfigHotkey("toggleInfoOverlayRendering", "", "Toggles the information for progress on the curent task with other similar info.");


    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_GUI_MAIN_MENU,
            OPEN_GUI_MATERIAL_LIST,
            OPEN_GUI_SELECTION_MANAGER,
            OPEN_GUI_SETTINGS,
            OPEN_GUI_BARITONE_SETTINGS,
            TOGGLE_INFO_OVERLAY_RENDERING
    );
}
