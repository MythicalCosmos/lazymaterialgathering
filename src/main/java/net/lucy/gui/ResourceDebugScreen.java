package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import net.lucy.data.WorldKnowledge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ResourceDebugScreen extends TableScreen {
    public ResourceDebugScreen() {
        super("Resource Debug");
    }

    @Override
    protected List<TableRow> buildRows() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return List.of();
        }

        String dimension = client.world.getRegistryKey().getValue().toString();
        BlockPos player = client.player.getBlockPos();
        List<WorldKnowledge.ResourceInfo> resources = WorldKnowledge.getKnownResources(dimension);
        resources = new ArrayList<>(resources);
        resources.sort(Comparator.comparingDouble(resource -> resource.position().getSquaredDistance(player)));
        List<TableRow> rows = new ArrayList<>();
        for (WorldKnowledge.ResourceInfo resource : resources) {
            BlockPos position = resource.position();
            double distance = Math.sqrt(position.getSquaredDistance(player));
            String item = TableRow.displayName(resource.item());
            String source = TableRow.displayName(resource.source());
            String coordinates = position.getX() + ", " + position.getY() + ", " + position.getZ();
            String distanceText = String.format("%.0f m", distance);
            TableRow row = new TableRow(resource.item(), resource.item() + " | " + resource.source() + " | " + coordinates, item, source, coordinates, distanceText);
            row.hoverLines.add("Resource: " + resource.item());
            row.hoverLines.add("Source block: " + resource.source());
            row.hoverLines.add("Position: " + coordinates);
            row.hoverLines.add("Distance: " + distanceText);
            row.hoverLines.add("Biome: " + WorldKnowledge.getBiomeAt(client.world, position));
            rows.add(row);
        }
        return rows;
    }

    @Override
    protected String[] getColumnTitles() {
        return new String[] {"Resource", "Source", "Position", "Distance"};
    }

    @Override
    protected int[] getColumnPercents() {
        return new int[] {0, 36, 57, 82};
    }

    @Override
    protected String getEmptyMessage() {
        return "No resources have been discovered yet.";
    }

    @Override
    protected void addNavigationButtons(int x, int y) {
        x += this.addNavButton(x, y, "Raw Materials", () -> GuiBase.openGui(new RawMaterialsScreen()));
        x += this.addNavButton(x, y, "Gathering", () -> GuiBase.openGui(new GatheringPlanScreen()));
        this.addNavButton(x, y, "Refresh", () -> GuiBase.openGui(new ResourceDebugScreen()));
    }
}