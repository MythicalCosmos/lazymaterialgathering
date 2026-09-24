package net.lucy.gui;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * The scrolling list with a search box on top. It just shows whatever rows it is given.
 */
public class TableListWidget extends WidgetListBase<TableRow, TableRowWidget>
{
    private final Supplier<List<TableRow>> rowSupplier;
    private final int[] columnPercents;

    public TableListWidget(int x, int y, int width, int height,
                           Supplier<List<TableRow>> rowSupplier, int[] columnPercents)
    {
        super(x, y, width, height, null);

        this.rowSupplier = rowSupplier;
        this.columnPercents = columnPercents;

        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, BrowserIcons.SEARCH, LeftRight.LEFT);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
    }

    @Override
    protected Collection<TableRow> getAllEntries()
    {
        return this.rowSupplier.get();
    }

    @Override
    protected List<String> getEntryStringsForFilter(TableRow row)
    {
        return ImmutableList.of(row.filterText);
    }

    @Override
    protected TableRowWidget createListEntryWidget(int x, int y, int listIndex, boolean isOdd, TableRow row)
    {
        return new TableRowWidget(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(row),
                isOdd, row, listIndex, this.columnPercents);
    }
}