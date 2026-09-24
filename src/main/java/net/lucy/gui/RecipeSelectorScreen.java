package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.calc.RecipeHeuristics;
import net.lucy.config.Configs;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Every craftable item, so you can pick which recipes Baritone is allowed to use for it
 * and which one it should prefer. Items are ordered the same way the creative inventory's
 * tabs are (building blocks, then redstone, then tools, and so on).
 *
 * Each row starts collapsed, showing only the item and how many recipes it has. Click a
 * row to expand it into a grid of recipe icons: left click an icon to turn that recipe on
 * or off, right click to make it the preferred one. At least one recipe per item always
 * stays on. Use the search box to jump to an item by name.
 */
public class RecipeSelectorScreen extends GuiListBase<RecipeItemEntry, WidgetRecipeItemEntry, WidgetListRecipeItems>
{
    private List<RecipeItemEntry> entries = new ArrayList<>();
    private int nextButtonY;

    public RecipeSelectorScreen()
    {
        super(SideButtonBar.WIDTH + 10, 30);

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
        // Same order as the creative inventory tabs; anything not in a creative tab
        // (most modded items included) falls back to alphabetical, at the very end.
        Comparator<Map.Entry<String, List<Recipe>>> byCreativeTab = Comparator
                .comparingInt((Map.Entry<String, List<Recipe>> e) -> CreativeTabOrder.getIndex(e.getKey()))
                .thenComparing(Map.Entry::getKey);

        List<Map.Entry<String, List<Recipe>>> sorted = new ArrayList<>(Recipes.recipes.entrySet());
        sorted.sort(byCreativeTab);

        List<RecipeItemEntry> entries = new ArrayList<>();
        for (Map.Entry<String, List<Recipe>> entry : sorted)
        {
            entries.add(new RecipeItemEntry(entry.getKey(), entry.getValue()));
        }

        return entries;
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

        if (this.entries.isEmpty())
        {
            String message = "No recipes yet. Join a world once so they can be read (they're then";
            String message2 = "remembered for next time, even offline).";
            this.addLabel(this.getListX() + 4, 18, this.getStringWidth(message) + 2, 12, 0xFFFFAA00, message);
            this.addLabel(this.getListX() + 4, 30, this.getStringWidth(message2) + 2, 12, 0xFFFFAA00, message2);
        }
        else if (Recipes.isFromCache())
        {
            String message = "Showing recipes saved from your last time in a world.";
            this.addLabel(this.getListX() + 4, 18, this.getStringWidth(message) + 2, 12, 0xFF55FFFF, message);
        }

        this.nextButtonY = SideButtonBar.START_Y;

        if (Configs.Generic.DEV_MODE_ENABLED.getBooleanValue())
        {
            this.addSideButton("Auto-Pick (Simplest)", this::runAutoPick);
        }

        this.addSideButton("Raw Materials", () -> GuiBase.openGui(new RawMaterialsScreen()));
        this.addSideButton("Main Menu", () -> GuiBase.openGui(new MainScreen()));
    }

    private void addSideButton(String label, Runnable action)
    {
        int width = Math.max(SideButtonBar.BUTTON_WIDTH, this.getStringWidth(label) + 10);
        ButtonGeneric button = new ButtonGeneric(SideButtonBar.X, this.nextButtonY, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());

        this.nextButtonY += SideButtonBar.SPACING;
    }

    // Dev-only: bulk-picks a recipe for every item with more than one option, using a
    // "fewest ingredients" heuristic (see RecipeHeuristics) rather than real popularity
    // data, which the game doesn't record.
    private void runAutoPick()
    {
        int changed = RecipeHeuristics.applyToAll();

        this.entries = buildEntries();
        this.getListWidget().refreshEntries();

        this.addMessage(MessageType.SUCCESS, "Updated the preferred recipe for %s item(s)", changed);
    }
}