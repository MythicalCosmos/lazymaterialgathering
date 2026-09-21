package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import net.lucy.calc.ItemClassifier;
import net.lucy.data.DataManager;
import net.lucy.model.SourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What you actually have to gather: the material list broken down into raw materials.
 */
public class RawMaterialsScreen extends TableScreen
{
    public RawMaterialsScreen()
    {
        super("Raw Materials");
    }

    @Override
    protected List<TableRow> buildRows()
    {
        List<TableRow> rows = new ArrayList<>();

        for (Map.Entry<String, Long> entry : sortedByCount(DataManager.getRawMaterials()))
        {
            String name = entry.getKey();
            long count = entry.getValue();

            SourceType sourceType = ItemClassifier.getSourceType(name);
            String source = sourceType == SourceType.OTHER ? "" : TableRow.prettify(sourceType.name());

            rows.add(new TableRow(name, name + ": " + count,
                    TableRow.displayName(name), String.valueOf(count), TableRow.stacks(count), source));
        }

        return rows;
    }

    @Override
    protected String[] getColumnTitles()
    {
        return new String[] { "Item", "Amount", "Stacks", "Source" };
    }

    @Override
    protected int[] getColumnPercents()
    {
        return new int[] { 0, 45, 60, 76 };
    }

    @Override
    protected String getEmptyMessage()
    {
        return "No schematic loaded yet. Use \"Load Schematic\" to pick one.";
    }

    @Override
    protected void addNavigationButtons(int x, int y)
    {
        x += this.addNavButton(x, y, "Preferred Recipes", () -> GuiBase.openGui(new RecipeSelectorScreen()));
        this.addNavButton(x, y, "Material List", () -> GuiBase.openGui(new MaterialListScreen()));
    }
}