package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import net.lucy.calc.RawMaterials;
import net.lucy.config.Configs;
import net.lucy.data.DataManager;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Some items can be made in several ways (for example an iron ingot from raw iron or from a block).
 * This screen lists the ones your schematic runs into. Click a row to switch to the next recipe,
 * right-click to go back. The choice is saved and the raw materials are recalculated.
 */
public class RecipeSelectorScreen extends TableScreen
{
    public RecipeSelectorScreen()
    {
        super("Preferred Recipes");

        Recipes.ensureLoaded();
    }

    @Override
    protected List<TableRow> buildRows()
    {
        // Ask the calculator which items had more than one recipe to pick from
        Set<String> choices = new TreeSet<>();
        RawMaterials.calculate(DataManager.getMinedItems(), choices);

        List<TableRow> rows = new ArrayList<>();

        for (String itemName : choices)
        {
            List<Recipe> options = Recipes.recipes.get(itemName);
            Recipe selected = RawMaterials.selectRecipe(itemName, options);
            int position = options.indexOf(selected) + 1;

            TableRow row = new TableRow(itemName, itemName + ": " + selected.id,
                    TableRow.displayName(itemName), selected.id, position + " / " + options.size());

            row.hoverLines.add(GuiBase.TXT_GRAY + "Left click: next recipe.  Right click: previous recipe.");

            for (Recipe option : options)
            {
                String marker = (option == selected) ? GuiBase.TXT_GREEN + "> " : "   ";
                row.hoverLines.add(marker + option.id);
                row.hoverLines.add("      " + describe(option));
            }

            row.onClick = mouseButton -> this.cycleRecipe(itemName, options, mouseButton == 1 ? -1 : 1);
            rows.add(row);
        }

        return rows;
    }

    // Switch to the next (or previous) recipe, save it, and update the numbers
    private void cycleRecipe(String itemName, List<Recipe> options, int step)
    {
        int current = options.indexOf(RawMaterials.selectRecipe(itemName, options));
        int next = Math.floorMod(current + step, options.size());

        Configs.recipePreferences.put(itemName, options.get(next).id);
        Configs.saveToFile();

        DataManager.recalculateRawMaterials();
        this.refreshRows();
    }

    // "2x oak_planks, 1x stick -> makes 4"
    private static String describe(Recipe recipe)
    {
        StringBuilder text = new StringBuilder();

        for (Map.Entry<String, Integer> ingredient : recipe.ingredients.entrySet())
        {
            if (text.length() > 0)
            {
                text.append(", ");
            }

            text.append(ingredient.getValue()).append("x ").append(ingredient.getKey());
        }

        return text + " -> makes " + recipe.outputCount;
    }

    @Override
    protected String[] getColumnTitles()
    {
        return new String[] { "Item", "Recipe used", "Choice" };
    }

    @Override
    protected int[] getColumnPercents()
    {
        return new int[] { 0, 30, 82 };
    }

    @Override
    protected String getEmptyMessage()
    {
        return "No recipe choices to make. Load a schematic first, or every material has just one recipe.";
    }

    @Override
    protected boolean hasCopyButton()
    {
        return false;
    }

    @Override
    protected void addNavigationButtons(int x, int y)
    {
        this.addNavButton(x, y, "Raw Materials", () -> GuiBase.openGui(new RawMaterialsScreen()));
    }
}