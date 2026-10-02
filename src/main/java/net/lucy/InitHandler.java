package net.lucy;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.lucy.baritone.GatheringQueue;
import net.lucy.config.Configs;
import net.lucy.data.BiomeChunkCache;
import net.lucy.data.Recipes;
import net.lucy.data.WorldKnowledge;
import net.lucy.events.HotKeyCallBacks;
import net.lucy.events.InputHandler;
import net.lucy.overlay.GatheringOverlay;
import net.minecraft.client.MinecraftClient;

public class InitHandler
        implements IInitializationHandler {

    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        HotKeyCallBacks.init(MinecraftClient.getInstance());
        GatheringQueue.register();
        GatheringOverlay.register();
        /*
         * Existing biome memory.
         */
        BiomeChunkCache.load();
        BiomeChunkCache.register();
        /*
         * New persistent chunk/resource knowledge.
         *
         * This sits above Baritone's own world cache.
         */
        WorldKnowledge.load();
        WorldKnowledge.register();
        /*
         * Minecraft's RecipeManager is populated when
         * the client joins a world/server.
         *
         * Refresh our recipe database at that point.
         */
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                    client.execute(Recipes::refreshFromWorld);
                });
    }
}