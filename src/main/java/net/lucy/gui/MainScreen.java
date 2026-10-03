package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.config.Configs;

/**
 * The main menu: one button per screen, laid out the same way Litematica's own main menu
 * (GuiMainMenu) is -- two columns starting at x=12, y=30, 22px apart, with a wider gap
 * between logical groups within a column.
 */
public class MainScreen extends GuiBase
{
    @Override
    public void initGui()
    {
        super.initGui();

        int width = 140;
        int x = 12;
        int y = 30;

        y += this.addMenuButton(x, y, width, "Load Schematic", () -> GuiBase.openGui(new SchematicLoaderScreen()));
        y += this.addMenuButton(x, y, width, "Material List", () -> GuiBase.openGui(new MaterialListScreen()));
        y += this.addMenuButton(x, y, width, "Raw Materials", () -> GuiBase.openGui(new RawMaterialsScreen()));
        y += 22; // gap before the next group

        y += this.addMenuButton(x, y, width, "Preferred Recipes", () -> GuiBase.openGui(new RecipeSelectorScreen()));
        this.addMenuButton(x, y, width, "Start Gathering", () -> GuiBase.openGui(new GatheringPlanScreen()));

        x += width + 20;
        y = 30;

        y += this.addMenuButton(x, y, width, "Baritone Config", () -> GuiBase.openGui(new BaritoneSettingsScreen()));
        this.addMenuButton(x, y, width, "Configuration", () -> GuiBase.openGui(new SettingsScreen()));
        if (Configs.Generic.DEV_MODE_ENABLED.getBooleanValue()) {
            this.addMenuButton(x, y, width, "Resource Debug", () -> GuiBase.openGui(new ResourceDebugScreen()));
        }
    }

    private int addMenuButton(int x, int y, int width, String label, Runnable action)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());

        return 22;
    }
}