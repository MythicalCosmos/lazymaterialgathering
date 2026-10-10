package net.lucy;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.lucy.baritone.GatheringQueue;
import net.lucy.baritone.CraftingQueue;
import net.lucy.baritone.MaterialPipelineQueue;
import net.lucy.baritone.ProcessingQueue;
import net.lucy.config.Configs;
import net.lucy.data.BiomeChunkCache;
import net.lucy.data.Recipes;
import net.lucy.data.WorldKnowledge;
import net.lucy.events.HotKeyCallBacks;
import net.lucy.events.InputHandler;
import net.lucy.gui.MaterialPipelineOverlay;
import net.minecraft.client.MinecraftClient;

public class InitHandler implements IInitializationHandler {

    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        HotKeyCallBacks.init(MinecraftClient.getInstance());
        GatheringQueue.register();
        CraftingQueue.register();
        ProcessingQueue.register();
        MaterialPipelineQueue.register();
        MaterialPipelineOverlay.register();

        // Restore and start the persistent resource/chunk knowledge system.
        // Registration is required for chunk-load and client-tick scanning
        // to run; loading alone would leave gathering without new discoveries.
        WorldKnowledge.load();
        WorldKnowledge.register();

        BiomeChunkCache.load();
        BiomeChunkCache.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(Recipes::refreshFromWorld));
    }
}
