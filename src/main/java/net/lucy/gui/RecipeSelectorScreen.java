package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Every craftable item, so you can pick which recipes Baritone is allowed to use for it
 * and which one it should prefer. Click an item's row to expand it into a grid of recipe
 * icons: left click an icon to turn that recipe on or off, right click to make it the
 * preferred one. At least one recipe per item always stays on.
 */
public class RecipeSelectorScreen extends GuiListBase<RecipeItemEntry, WidgetRecipeItemEntry, WidgetListRecipeItems>
{
    private List<RecipeItemEntry> entries = new ArrayList<>();

    public RecipeSelectorScreen()
    {
        super(12, 46);

        this.title = "Preferred Recipes";
        Recipes.ensureLoaded();
    }

    @Override
    protected WidgetListRecipeItems createListWidget(int listX, int listY)
    {
        this.entries = buildEntries();
        return new WidgetListRecipeItems(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this.entries, this);
    }

    private static List<RecipeItemEntry> buildEntries()
    {
        // Alphabetical, and stable across screen refreshes (same Recipe objects, so
        // recipe.id.equals() still works for RawMaterials' comparisons)
        Map<String, List<Recipe>> sorted = new TreeMap<>(Recipes.recipes);

        List<RecipeItemEntry> entries = new ArrayList<>();
        for (Map.Entry<String, List<Recipe>> entry : sorted.entrySet())
        {
            entries.add(new RecipeItemEntry(entry.getKey(), entry.getValue()));
        }

        return entries;
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 60;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        if (this.entries.isEmpty())
        {
            String message = "No recipes found. Load a schematic once, or join a world, so recipes can be read.";
            this.addLabel(this.getListX() + 4, 80, this.getStringWidth(message) + 2, 12, 0xFFFFAA00, message);
        }

        int y = this.height - 26;
        int x = 12;

        String rawLabel = "Raw Materials";
        int rawWidth = this.getStringWidth(rawLabel) + 10;
        ButtonGeneric rawButton = new ButtonGeneric(x, y, rawWidth, 20, rawLabel);
        this.addButton(rawButton, (button, mouseButton) -> GuiBase.openGui(new RawMaterialsScreen()));

        String mainMenuLabel = "Main Menu";
        int mainMenuWidth = this.getStringWidth(mainMenuLabel) + 20;
        ButtonGeneric mainMenuButton = new ButtonGeneric(this.width - mainMenuWidth - 10, y, mainMenuWidth, 20, mainMenuLabel);
        this.addButton(mainMenuButton, (button, mouseButton) -> GuiBase.openGui(new MainScreen()));
    }
}