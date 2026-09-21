package net.lucy;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;

public class LazyMaterialGatheringClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}