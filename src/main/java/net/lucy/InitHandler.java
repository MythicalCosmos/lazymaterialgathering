package net.lucy;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.lucy.baritone.GatheringQueue;
import net.lucy.config.Configs;
import net.lucy.data.Recipes;
import net.lucy.events.HotKeyCallBacks;
import net.lucy.events.InputHandler;
import net.minecraft.client.MinecraftClient;

public class InitHandler
        implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager
                .getInstance()
                .registerConfigHandler(
                        Reference.MOD_ID,
                        new Configs()
                );

        InputEventHandler
                .getKeybindManager()
                .registerKeybindProvider(
                        InputHandler.getInstance()
                );

        HotKeyCallBacks.init(
                MinecraftClient.getInstance()
        );

        GatheringQueue.register();

        /*
         * Minecraft has its RecipeManager populated when the
         * client joins a world/server.
         *
         * At that point we can safely read every recipe that
         * Minecraft knows about.
         */
        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) ->
                {
                    client.execute(
                            Recipes::refreshFromWorld
                    );
                }
        );
    }
}