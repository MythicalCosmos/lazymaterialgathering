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
 * The list of files is a SchematicBrowserWidget; the buttons sit in a column on the left.
 */
public class SchematicLoaderScreen extends GuiListBase<DirectoryEntry, WidgetDirectoryEntry, SchematicBrowserWidget>
{
    // Name used to remember the last folder this screen was in (see DataManager)
    private static final String BROWSER_CONTEXT = "schematic_load";

    private int nextButtonY;

    public SchematicLoaderScreen()
    {
        super(SideButtonBar.WIDTH + 10, 24); // where the file list starts (x, y)

        this.title = "Load Schematic";
    }

    @Override
    protected SchematicBrowserWidget createListWidget(int listX, int listY)
    {
        File root = DataManager.getSchematicsDirectory();

        // If the schematic folder setting changed, don't reopen in a folder from the old location
        DataManager.resetDirectoryIfOutside(BROWSER_CONTEXT, root);

        // The real width and height are set in initGui() using the two methods below
        return new SchematicBrowserWidget(listX, listY, 100, 100, BROWSER_CONTEXT,
                root, this.getSelectionListener());
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - SideButtonBar.WIDTH - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 40;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.nextButtonY = SideButtonBar.START_Y;

        this.addSideButton("Load Schematic", this::loadSelectedSchematic);
        this.addSideButton("Material List", () -> GuiBase.openGui(new MaterialListScreen()));
        this.addSideButton("Main Menu", () -> GuiBase.openGui(new MainScreen()));
    }

    private void addSideButton(String label, Runnable action)
    {
        int width = Math.max(SideButtonBar.BUTTON_WIDTH, this.getStringWidth(label) + 10);
        ButtonGeneric button = new ButtonGeneric(SideButtonBar.X, this.nextButtonY, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());

        this.nextButtonY += SideButtonBar.SPACING;
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
            SchemParser.parse(schematic); // counts the blocks and writes the reports (also stores them in DataManager)

            this.addMessage(MessageType.SUCCESS, "Loaded %s", file.getName());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.addMessage(MessageType.ERROR, "Failed to load %s: %s", file.getName(), e.getMessage());
        }
    }
}