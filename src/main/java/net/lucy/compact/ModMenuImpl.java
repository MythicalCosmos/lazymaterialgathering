package net.lucy.compact;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.lucy.gui.SettingsScreen;

public class ModMenuImpl implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return (screen) -> {
            SettingsScreen gui = new SettingsScreen();
            gui.setParent(screen);
            return gui;
        };
    }
}