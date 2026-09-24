package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base for the screens that show a searchable table: title, column headings,
 * the list of rows, and a row of buttons along the bottom.
 * A screen only has to say what its rows are and which extra buttons it wants.
 */
public abstract class TableScreen extends GuiListBase<TableRow, TableRowWidget, TableListWidget>
{
    protected List<TableRow> rows = new ArrayList<>();

    protected TableScreen(String title)
    {
        super(12, 46); // where the list starts (x, y)

        this.title = title;
    }

    // ---------- What each screen provides ----------

    protected abstract List<TableRow> buildRows();

    protected abstract String[] getColumnTitles();

    /** Where each column starts, as a percentage of the row width. The first one is always 0. */
    protected abstract int[] getColumnPercents();

    protected abstract String getEmptyMessage();

    /** Add this screen's own buttons, starting at x. Use addNavButton() for each one. */
    protected abstract void addNavigationButtons(int x, int y);

    protected boolean hasCopyButton()
    {
        return true;
    }

    // ---------- List plumbing ----------

    @Override
    protected TableListWidget createListWidget(int listX, int listY)
    {
        return new TableListWidget(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(),
                () -> this.rows, this.getColumnPercents());
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 80;
    }

    private void reloadRows()
    {
        this.rows = this.buildRows();
    }

    /** Rebuild the rows and redraw the list (call this after the data changed). */
    protected void refreshRows()
    {
        this.reloadRows();
        this.getListWidget().refreshEntries();
    }

    // ---------- Layout ----------

    @Override
    public void initGui()
    {
        this.reloadRows(); // must happen before super.initGui(), which creates the list

        super.initGui();

        // Column headings above the list. They use the same maths as the rows so they line up.
        String[] titles = this.getColumnTitles();
        int[] percents = this.getColumnPercents();
        int entryX = this.getListX() + 2;
        int entryWidth = this.getBrowserWidth() - 14;

        for (int i = 0; i < titles.length; i++)
        {
            int labelX = (i == 0) ? entryX + 22 : entryX + entryWidth * percents[i] / 100;
            this.addLabel(labelX, 34, this.getStringWidth(titles[i]) + 2, 12, 0xFFAAAAAA, titles[i]);
        }

        if (this.rows.isEmpty())
        {
            String message = this.getEmptyMessage();
            this.addLabel(entryX + 4, 80, this.getStringWidth(message) + 2, 12, 0xFFFFAA00, message);
        }

        // Buttons along the bottom
        int y = this.height - 26;
        int x = 12;

        if (this.hasCopyButton())
        {
            x += this.addNavButton(x, y, "Copy List", this::copyList);
        }

        this.addNavigationButtons(x, y);

        String mainMenuLabel = "Main Menu";
        int mainMenuWidth = this.getStringWidth(mainMenuLabel) + 20;
        ButtonGeneric mainMenuButton = new ButtonGeneric(this.width - mainMenuWidth - 10, y, mainMenuWidth, 20, mainMenuLabel);
        this.addButton(mainMenuButton, (button, mouseButton) -> GuiBase.openGui(new MainScreen()));
    }

    /** Adds a button and returns how much space it used, so the next one can start after it. */
    protected int addNavButton(int x, int y, String label, Runnable action)
    {
        int width = this.getStringWidth(label) + 10;
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());

        return width + 4;
    }

    private void copyList()
    {
        String text = this.rows.stream().map(row -> row.copyText).collect(Collectors.joining("\n"));
        MinecraftClient.getInstance().keyboard.setClipboard(text);

        this.addMessage(MessageType.SUCCESS, "Copied %s lines to the clipboard", this.rows.size());
    }

    // ---------- Shared by the material screens ----------

    /** Biggest amount first, then alphabetical. */
    protected static List<Map.Entry<String, Long>> sortedByCount(Map<String, Long> counts)
    {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(counts.entrySet());

        entries.sort(Comparator
                .comparing((Map.Entry<String, Long> entry) -> entry.getValue()).reversed()
                .thenComparing(Map.Entry::getKey));

        return entries;
    }
}