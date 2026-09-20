package net.lucy.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

public class Hotkeys {
    public static final ConfigHotkey OPEN_GUI_MAIN_MENU                 = new ConfigHotkey("openGuiMainMenu",                   "M",    KeybindSettings.RELEASE_EXCLUSIVE, "Open the Litematica main menu");

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_GUI_MAIN_MENU
    );
}
