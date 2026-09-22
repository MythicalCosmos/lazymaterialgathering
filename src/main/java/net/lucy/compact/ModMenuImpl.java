package net.lucy.compact;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.lucy.gui.MainScreen;

public class ModMenuImpl implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return (screen) -> {
            MainScreen gui = new MainScreen();
            gui.setParent(screen);
            return gui;
        };
    }
}