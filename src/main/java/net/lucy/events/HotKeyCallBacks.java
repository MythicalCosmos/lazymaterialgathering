package net.lucy.events;

import net.lucy.config.Configs;
import net.lucy.config.Hotkeys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.lucy.gui.SettingsScreen;
import net.lucy.gui.BaritoneSettingsScreen;
import net.lucy.gui.MainScreen;
import net.lucy.gui.MaterialListScreen;
import net.lucy.gui.RawMaterialsScreen;
import net.lucy.gui.RecipeSelectorScreen;
import net.lucy.gui.SchematicLoaderScreen;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerMode;

public class HotKeyCallBacks {
    public static void init(MinecraftClient mc) {
        IHotkeyCallback openScreenCallback = new OpenScreenCallback(mc);

        Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind().setCallback(openScreenCallback);
        Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind().setCallback(openScreenCallback);
        Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind().setCallback(openScreenCallback);
        Hotkeys.OPEN_GUI_SETTINGS.getKeybind().setCallback(openScreenCallback);
        Hotkeys.OPEN_GUI_BARITONE_SETTINGS.getKeybind().setCallback(openScreenCallback);

        Hotkeys.TOGGLE_INFO_OVERLAY_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.InfoOverlays.INFO_OVERLAY_ENABLED));
    }

    private static class OpenScreenCallback implements IHotkeyCallback {
        private final MinecraftClient mc;

        public OpenScreenCallback(MinecraftClient mc) {
            this.mc = mc;
        }

        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            if (this.mc.player == null || this.mc.world == null) {
                return false;
            }

            if (key == Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind()) {
                GuiBase.openGui(new MainScreen());
                return true;
            }

            if (key == Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind()) {
                GuiBase.openGui(new MaterialListScreen());
                return true;
            }

            if (key == Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind()) {
                GuiBase.openGui(new SchematicLoaderScreen());
                return true;
            }

            if (key == Hotkeys.OPEN_GUI_SETTINGS.getKeybind()) {
                GuiBase.openGui(new SettingsScreen());
                return true;
            }

            if (key == Hotkeys.OPEN_GUI_BARITONE_SETTINGS.getKeybind()) {
                GuiBase.openGui(new BaritoneSettingsScreen());
                return true;
            }
            return false;
        }
    }
}