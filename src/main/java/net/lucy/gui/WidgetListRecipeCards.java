package net.lucy.gui;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import net.lucy.model.Recipe;

import java.util.Collection;
import java.util.List;

/** A scrolling list of RecipeCardWidget, one per known way to craft or smelt one item. */
public class WidgetListRecipeCards extends WidgetListBase<Recipe, RecipeCardWidget>
{
    private final List<Recipe> recipes;
    private final String outputName;

    public WidgetListRecipeCards(int x, int y, int width, int height, List<Recipe> recipes, String outputName)
    {
        super(x, y, width, height, null);

        this.recipes = recipes;
        this.outputName = outputName;

        this.browserEntryHeight = RecipeCardWidget.HEIGHT;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, BrowserIcons.SEARCH, LeftRight.LEFT);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
    }

    @Override
    protected Collection<Recipe> getAllEntries()
    {
        return this.recipes;
    }

    @Override
    protected List<String> getEntryStringsForFilter(Recipe recipe)
    {
        return ImmutableList.of(recipe.id);
    }

    @Override
    protected RecipeCardWidget createListEntryWidget(int x, int y, int listIndex, boolean isOdd, Recipe recipe)
    {
        return new RecipeCardWidget(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(recipe), isOdd, recipe, listIndex, this.outputName);
    }
}