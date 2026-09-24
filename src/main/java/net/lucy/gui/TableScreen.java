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
 * Base for the screens that show a searchable table: title, column headings, the list of
 * rows, and its navigation buttons in a column down the left edge.
 * A screen only has to say what its rows are and which extra buttons it wants.
 */
public abstract class TableScreen extends GuiListBase<TableRow, TableRowWidget, TableListWidget>
{
    protected List<TableRow> rows = new ArrayList<>();
    private int nextButtonY;

    protected TableScreen(String title)
    {
        super(SideButtonBar.WIDTH + 10, 30); // where the list starts (x, y), right of the button column

        this.title = title;
    }

    // ---------- What each screen provides ----------

    protected abstract List<TableRow> buildRows();

    protected abstract String[] getColumnTitles();

    /** Where each column starts, as a percentage of the row width. The first one is always 0. */
    protected abstract int[] getColumnPercents();

    protected abstract String getEmptyMessage();

    /** Add this screen's own buttons, below the standard ones. Call addSideButton() for each. */
    protected abstract void addNavigationButtons();

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
        return this.width - SideButtonBar.WIDTH - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 44;
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
            this.addLabel(labelX, 18, this.getStringWidth(titles[i]) + 2, 12, 0xFFAAAAAA, titles[i]);
        }

        if (this.rows.isEmpty())
        {
            String message = this.getEmptyMessage();
            this.addLabel(entryX + 4, 64, this.getStringWidth(message) + 2, 12, 0xFFFFAA00, message);
        }

        // Buttons in a column down the left edge
        this.nextButtonY = SideButtonBar.START_Y;

        if (this.hasCopyButton())
        {
            this.addSideButton("Copy List", this::copyList);
        }

        this.addNavigationButtons();
        this.addSideButton("Main Menu", () -> GuiBase.openGui(new MainScreen()));
    }

    /** Adds the next button in the left-hand column. */
    protected void addSideButton(String label, Runnable action)
    {
        int width = Math.max(SideButtonBar.BUTTON_WIDTH, this.getStringWidth(label) + 10);
        ButtonGeneric button = new ButtonGeneric(SideButtonBar.X, this.nextButtonY, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());

        this.nextButtonY += SideButtonBar.SPACING;
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