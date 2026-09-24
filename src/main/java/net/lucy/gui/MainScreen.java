package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;

/**
 * The main menu: one button per screen, in a column down the left edge.
 */
public class MainScreen extends GuiBase
{
    @Override
    public void initGui()
    {
        super.initGui();

        int y = SideButtonBar.START_Y;

        y = this.addMenuButton(y, "Raw Materials", () -> GuiBase.openGui(new RawMaterialsScreen()));
        y = this.addMenuButton(y, "Material List", () -> GuiBase.openGui(new MaterialListScreen()));
        y = this.addMenuButton(y, "Load Schematic", () -> GuiBase.openGui(new SchematicLoaderScreen()));
        y = this.addMenuButton(y, "Preferred Recipes", () -> GuiBase.openGui(new RecipeSelectorScreen()));
        y = this.addMenuButton(y, "Baritone Config", () -> GuiBase.openGui(new BaritoneSettingsScreen()));
        this.addMenuButton(y, "Configuration", () -> GuiBase.openGui(new SettingsScreen()));
    }

    private int addMenuButton(int y, String label, Runnable action)
    {
        int width = Math.max(SideButtonBar.BUTTON_WIDTH, this.getStringWidth(label) + 10);
        ButtonGeneric button = new ButtonGeneric(SideButtonBar.X, y, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());

        return y + SideButtonBar.SPACING;
    }
}