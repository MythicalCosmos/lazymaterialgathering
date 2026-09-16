package net.lucy;

import fi.dy.masa.malilib.gui.GuiBase;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.lucy.config.Config;
import net.lucy.gui.MainScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

public class LazyMaterialGatheringClient implements ClientModInitializer {

    private static KeyBinding openGuiKey;

    public static Path getConfigFile() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("lazymaterialgathering.properties");
    }

    @Override
    public void onInitializeClient() {

        try {
            Config.load(getConfigFile());
        } catch (Exception e) {
            System.err.println("Failed to load Lazy Material Gathering config.");
            e.printStackTrace();
        }

        openGuiKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.lazymaterialgathering.open_gui",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_P,
                        "category.lazymaterialgathering"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                GuiBase.openGui(new MainScreen());
            }
        });
    }
}