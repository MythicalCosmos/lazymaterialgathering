package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import net.lucy.SchemParser;
import net.lucy.data.DataManager;
import net.sandrohc.schematic4j.SchematicLoader;
import net.sandrohc.schematic4j.schematic.Schematic;

import java.io.File;

/**
 * Lets the player pick a schematic file and load it.
 * The list of files is a SchematicBrowserWidget; the buttons sit along the bottom, the
 * same layout Litematica's own schematic loader screen uses.
 */
public class SchematicLoaderScreen extends GuiListBase<DirectoryEntry, WidgetDirectoryEntry, SchematicBrowserWidget>
{
    private static final String BROWSER_CONTEXT = "schematic_load";

    public SchematicLoaderScreen()
    {
        super(12, 24); // where the file list starts (x, y)

        this.title = "Load Schematic";
    }

    @Override
    protected SchematicBrowserWidget createListWidget(int listX, int listY)
    {
        File root = DataManager.getSchematicsDirectory();

        DataManager.resetDirectoryIfOutside(BROWSER_CONTEXT, root);

        return new SchematicBrowserWidget(listX, listY, 100, 100, BROWSER_CONTEXT,
                root, this.getSelectionListener());
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 70;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 12;
        int y = this.height - 26;

        x += this.addNavButton(x, y, "Load Schematic", this::loadSelectedSchematic);
        x += this.addNavButton(x, y, "Material List", () -> GuiBase.openGui(new MaterialListScreen()));

        String mainMenuLabel = "Main Menu";
        int mainMenuWidth = this.getStringWidth(mainMenuLabel) + 20;
        ButtonGeneric mainMenuButton = new ButtonGeneric(this.width - mainMenuWidth - 10, y, mainMenuWidth, 20, mainMenuLabel);
        this.addButton(mainMenuButton, (button, mouseButton) -> GuiBase.openGui(new MainScreen()));
    }

    private int addNavButton(int x, int y, String label, Runnable action)
    {
        int width = this.getStringWidth(label) + 10;
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());

        return width + 4;
    }

    private void loadSelectedSchematic()
    {
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry == null || entry.getType() != DirectoryEntryType.FILE)
        {
            this.addMessage(MessageType.ERROR, "Select a schematic file first.");
            return;
        }

        File file = entry.getFullPath();

        if (file.isFile() == false || file.canRead() == false)
        {
            this.addMessage(MessageType.ERROR, "Can't read the file %s", file.getName());
            return;
        }

        try
        {
            Schematic schematic = SchematicLoader.load(file);
            SchemParser.parse(schematic);

            this.addMessage(MessageType.SUCCESS, "Loaded %s", file.getName());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.addMessage(MessageType.ERROR, "Failed to load %s: %s", file.getName(), e.getMessage());
        }
    }
}