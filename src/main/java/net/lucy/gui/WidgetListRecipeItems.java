package net.lucy.gui;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;

import java.util.Collection;
import java.util.List;

public class WidgetListRecipeItems extends WidgetListBase<RecipeItemEntry, WidgetRecipeItemEntry>
{
    private final List<RecipeItemEntry> entries;
    private final GuiListBase<?, ?, ?> screen;

    public WidgetListRecipeItems(int x, int y, int width, int height,
                                 List<RecipeItemEntry> entries, GuiListBase<?, ?, ?> screen)
    {
        super(x, y, width, height, null);

        this.entries = entries;
        this.screen = screen;

        this.browserEntryHeight = 22; // the collapsed row height; expanded rows override this per-entry
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, BrowserIcons.SEARCH, LeftRight.LEFT);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
    }

    public GuiListBase<?, ?, ?> getScreen()
    {
        return this.screen;
    }

    @Override
    protected Collection<RecipeItemEntry> getAllEntries()
    {
        return this.entries;
    }

    @Override
    protected List<String> getEntryStringsForFilter(RecipeItemEntry entry)
    {
        return ImmutableList.of(entry.itemName);
    }

    @Override
    protected int getBrowserEntryHeightFor(RecipeItemEntry entry)
    {
        return WidgetRecipeItemEntry.heightFor(entry, this.browserEntryWidth);
    }

    @Override
    protected WidgetRecipeItemEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, RecipeItemEntry entry)
    {
        return new WidgetRecipeItemEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, entry, listIndex, this);
    }
}