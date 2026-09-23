package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import net.lucy.calc.ItemClassifier;
import net.lucy.data.DataManager;
import net.lucy.model.SourceType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * What you actually have to gather: the material list broken down into raw materials.
 * Hover a row to see which crafted items need it and how much of it they need; click a
 * row to copy that same breakdown to your clipboard.
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

            TableRow row = new TableRow(name, name + ": " + count,
                    TableRow.displayName(name), String.valueOf(count), TableRow.stacks(count), source);

            row.hoverLines.addAll(describeUsage(name));
            row.onClick = mouseButton -> copyUsageToClipboard(name, count);

            rows.add(row);
        }

        return rows;
    }

    // "Used in: <item> (needs <n>)", biggest use first. Falls back to a note when nothing
    // uses it directly (it's a leaf ingredient straight from mining, for example).
    private static List<String> describeUsage(String rawMaterialName)
    {
        Map<String, Long> usage = DataManager.getUsageFor(rawMaterialName);
        List<String> lines = new ArrayList<>();

        if (usage.isEmpty())
        {
            lines.add(GuiBase.TXT_GRAY + "Not needed by any recipe directly \u2014 this is a base material.");
            lines.add(GuiBase.TXT_GRAY + "Click to copy this row.");
            return lines;
        }

        lines.add(GuiBase.TXT_GOLD + "Used in:");

        usage.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .forEach(entry -> lines.add("  " + TableRow.displayName(entry.getKey()) + "  (needs " + entry.getValue() + ")"));

        lines.add("");
        lines.add(GuiBase.TXT_GRAY + "Click to copy this breakdown.");
        return lines;
    }

    private static void copyUsageToClipboard(String rawMaterialName, long totalCount)
    {
        StringBuilder text = new StringBuilder();
        text.append(TableRow.displayName(rawMaterialName)).append(": ").append(totalCount).append('\n');

        Map<String, Long> usage = DataManager.getUsageFor(rawMaterialName);

        if (usage.isEmpty())
        {
            text.append("(base material, not needed by any recipe directly)");
        }
        else
        {
            text.append("Used in:\n");
            usage.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                            .thenComparing(Map.Entry::getKey))
                    .forEach(entry -> text.append("  ")
                            .append(TableRow.displayName(entry.getKey()))
                            .append(": needs ").append(entry.getValue()).append('\n'));
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.keyboard.setClipboard(text.toString());

        if (client.player != null)
        {
            client.player.sendMessage(Text.literal("Copied " + TableRow.displayName(rawMaterialName) + "'s usage to the clipboard"), true);
        }
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