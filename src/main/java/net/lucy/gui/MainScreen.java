package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.baritone.MaterialPipelineQueue;
import net.lucy.config.Configs;

public class MainScreen extends GuiBase {
    @Override
    public void initGui() {
        super.initGui();
        int width = 140;
        int x = 12;
        int y = 30;
        y += this.addMenuButton(x, y, width, "Load Schematic", () -> GuiBase.openGui(new SchematicLoaderScreen()));
        y += this.addMenuButton(x, y, width, "Material List", () -> GuiBase.openGui(new MaterialListScreen()));
        y += this.addMenuButton(x, y, width, "Raw Materials", () -> GuiBase.openGui(new RawMaterialsScreen()));
        y += 22;
        y += this.addMenuButton(x, y, width, "Preferred Recipes", () -> GuiBase.openGui(new RecipeSelectorScreen()));
        y += this.addMenuButton(x, y, width, "Start Gathering", () -> GuiBase.openGui(new GatheringPlanScreen()));
        y += this.addMenuButton(x, y, width, "Gather -> Process -> Craft", MaterialPipelineQueue::startFromCalculation);
        this.addMenuButton(
                x,
                y,
                width,
                "Pipeline Control",
                () -> GuiBase.openGui(new MaterialPipelineControlScreen())
        );
        x += width + 20;
        y = 30;
        y += this.addMenuButton(x, y, width, "Baritone Config", () -> GuiBase.openGui(new BaritoneSettingsScreen()));
        this.addMenuButton(x, y, width, "Configuration", () -> GuiBase.openGui(new SettingsScreen()));
        if (Configs.Generic.DEV_MODE_ENABLED.getBooleanValue()) {
            y += 30;
            this.addMenuButton(x, y, width, "Resource Debug [DEV_CONSOLE_ENABLED]", () -> GuiBase.openGui(new ResourceDebugScreen()));
        }
        if (Configs.Generic.DEV_MODE_ENABLED.getBooleanValue()) {
            y += 30;

            this.addMenuButton(
                    x,
                    y,
                    width,
                    "Developer Tools",
                    () -> GuiBase.openGui(
                            new DeveloperToolsScreen()
                    )
            );
        }

    }

    private int addMenuButton(int x, int y, int width, String label, Runnable action) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());
        return 22;
    }
}