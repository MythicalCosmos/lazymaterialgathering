package net.lucy;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.lucy.config.Configs;
import net.lucy.baritone.GatheringQueue;
import net.lucy.events.HotKeyCallBacks;
import net.lucy.events.InputHandler;
import net.minecraft.client.MinecraftClient;

/**
 * malilib calls registerModHandlers() once the game has finished starting up.
 * Everything that talks to malilib (configs, hotkeys) gets registered here.
 */
public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        // Loads/saves your config file and hotkey bindings
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());

        // Tells malilib which keys exist so it can listen for them
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());

        // Attaches the "what happens when pressed" code to each hotkey
        HotKeyCallBacks.init(MinecraftClient.getInstance());

        // Lets GatheringQueue check its progress once per game tick
        GatheringQueue.register();
    }
}