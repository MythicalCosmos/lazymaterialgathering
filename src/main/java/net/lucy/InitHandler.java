package net.lucy;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.lucy.config.Configs;
import net.lucy.data.DataManager;
import net.lucy.events.HotKeyCallBacks;
import net.lucy.events.InputHandler;
import net.minecraft.client.MinecraftClient;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        HotKeyCallBacks.init(MinecraftClient.getInstance());
        DataManager.load();
    }
}