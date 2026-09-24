package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import net.lucy.data.DataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every block the loaded schematic needs, with how many of each.
 */
public class MaterialListScreen extends TableScreen
{
    public MaterialListScreen()
    {
        super("Material List");
    }

    @Override
    protected List<TableRow> buildRows()
    {
        List<TableRow> rows = new ArrayList<>();

        for (Map.Entry<String, Long> entry : sortedByCount(DataManager.getBlockCounts()))
        {
            String name = entry.getKey();
            long count = entry.getValue();

            rows.add(new TableRow(name, name + ": " + count,
                    TableRow.displayName(name), String.valueOf(count), TableRow.stacks(count)));
        }

        return rows;
    }

    @Override
    protected String[] getColumnTitles()
    {
        return new String[] { "Item", "Amount", "Stacks" };
    }

    @Override
    protected int[] getColumnPercents()
    {
        return new int[] { 0, 55, 72 };
    }

    @Override
    protected String getEmptyMessage()
    {
        return "No schematic loaded yet. Use \"Load Schematic\" to pick one.";
    }

    @Override
    protected void addNavigationButtons()
    {
        this.addSideButton("Raw Materials", () -> GuiBase.openGui(new RawMaterialsScreen()));
        this.addSideButton("Load Schematic", () -> GuiBase.openGui(new SchematicLoaderScreen()));
    }
}