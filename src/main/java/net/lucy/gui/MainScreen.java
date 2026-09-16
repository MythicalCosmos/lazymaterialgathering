package net.lucy.gui;


import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.data.Main;

public class MainScreen extends GuiBase {
    @Override
    public void initGui()
    {
        super.initGui();
        int x = 12;
        int y = 30;
        int width = 180;
        this.createButton(x, y, width, "Materials");
        y += 22;

        this.createButton(x, y, width, "Settings");
    }

    private void createButton(
            int x,
            int y,
            int width,
            String label)
    {
        ButtonGeneric button =
                new ButtonGeneric(
                        x,
                        y,
                        width,
                        20,
                        label
                );

        this.addButton(button, new ButtonListener(label, this));
    }
    private static class ButtonListener implements IButtonActionListener {
        private final String action;
        private final MainScreen parent;

        public ButtonListener(
                String action,
                MainScreen parent)
        {
            this.action = action;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(
                ButtonBase button,
                int mouseButton)
        {
            switch (this.action)
            {
                case "Load Schematic":
                    GuiBase.openGui(
                            new SchematicLoaderScreen()
                    );
                    break;

                case "Materials List":
                    GuiBase.openGui(
                            new MaterialListScreen()

                    );
                    break;
                case "Raw Materials":
                    GuiBase.openGui(
                            new RawMaterialsScreen()
                    );
                    break;
                case "Preferred Recipes":
                    GuiBase.openGui(
                            new RecipeSelectorScreen()

                    );
                    break;

                case "Baritone Config":
                    GuiBase.openGui(
                            new BaritoneSettingsScreen()

                            );
                    break;
                case "Configuration":
                    GuiBase.openGui(
                            new SettingsScreen()
                    );
                    break;
            }
        }
    }


}
