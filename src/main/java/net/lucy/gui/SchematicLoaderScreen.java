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
 * The list of files is a SchematicBrowserWidget; this class adds the buttons under it.
 */
public class SchematicLoaderScreen extends GuiListBase<DirectoryEntry, WidgetDirectoryEntry, SchematicBrowserWidget>
{
    // Name used to remember the last folder this screen was in (see DataManager)
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

        // If the schematic folder setting changed, don't reopen in a folder from the old location
        DataManager.resetDirectoryIfOutside(BROWSER_CONTEXT, root);

        // The real width and height are set in initGui() using the two methods below
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

        int y = this.height - 26;

        // Load button, bottom left
        String loadLabel = "Load Schematic";
        int loadWidth = this.getStringWidth(loadLabel) + 10;
        ButtonGeneric loadButton = new ButtonGeneric(12, y, loadWidth, 20, loadLabel);
        this.addButton(loadButton, (button, mouseButton) -> this.loadSelectedSchematic());

        // Opens the material list for the schematic that was loaded last
        String materialsLabel = "Material List";
        int materialsWidth = this.getStringWidth(materialsLabel) + 10;
        ButtonGeneric materialsButton = new ButtonGeneric(12 + loadWidth + 4, y, materialsWidth, 20, materialsLabel);
        this.addButton(materialsButton, (button, mouseButton) -> GuiBase.openGui(new MaterialListScreen()));

        // Back button, bottom right
        String backLabel = "Main Menu";
        int backWidth = this.getStringWidth(backLabel) + 20;
        ButtonGeneric backButton = new ButtonGeneric(this.width - backWidth - 10, y, backWidth, 20, backLabel);
        this.addButton(backButton, (button, mouseButton) -> GuiBase.openGui(new MainScreen()));
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