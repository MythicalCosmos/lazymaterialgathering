package net.lucy.gui;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.calc.IPath;
import baritone.api.utils.BetterBlockPos;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.baritone.DepositLocations;
import net.lucy.baritone.GatheringQueue;
import net.lucy.data.BiomeChunkCache;
import net.lucy.data.CalculationData;
import net.lucy.data.WorldKnowledge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Screen used to start/control the Baritone gathering process.
 *
 * The screen shows:
 *
 *  - deposit location
 *  - number of raw materials
 *  - biome memory
 *  - world/resource knowledge
 *  - current gathering item
 *  - current inventory
 *  - required quantity
 *  - remaining quantity
 *  - Baritone ETA
 *  - current planned path positions
 */
public class GatheringPlanScreen extends GuiBase {
    private static final int MAX_PATH_COORDS_SHOWN = 12;
    @Override
    public void initGui() {
        super.initGui();
        int y = 30;
        this.addLabel(12, y, 300, 12, 0xFFFFFFFF, "Deposit location: " + describeDeposit());
        y += 16;
        String setDepositLabel = "Use My Current Position";
        int setDepositWidth = this.getStringWidth(setDepositLabel) + 10;
        ButtonGeneric setDepositButton = new ButtonGeneric(12, y, setDepositWidth, 20, setDepositLabel);
        this.addButton(setDepositButton, (b, mb) -> this.setDepositToCurrentPosition());
        y += 26;
        Map<String, Long> materials = CalculationData.getRawMaterials();
        String summary = materials.size() + " raw material(s) on the list " + "(see Raw Materials for the full breakdown)";
        this.addLabel(12, y, this.getStringWidth(summary) + 2, 12, 0xFFAAAAAA, summary);
        y += 14;
        String biomeSummary = describeBiomeCache();
        this.addLabel(12, y, this.getStringWidth(biomeSummary) + 2, 12, 0xFFAAAAAA, biomeSummary);
        y += 14;
        String worldSummary = describeWorldKnowledge();
        this.addLabel(12, y, this.getStringWidth(worldSummary) + 2, 12, 0xFFAAAAAA, worldSummary);
        y += 20;
        /*
         * Start / Pause / Resume / Stop
         */
        if (!GatheringQueue.isRunning()) {
            String startLabel = "Start Gathering";
            int startWidth = this.getStringWidth(startLabel) + 10;
            ButtonGeneric startButton = new ButtonGeneric(12, y, startWidth, 20, startLabel);
            this.addButton(startButton, (b, mb) -> this.startGathering(materials));
        } else {
            int x = 12;
            if (GatheringQueue.isPaused()) {
                String resumeLabel = "Resume";
                int resumeWidth = this.getStringWidth(resumeLabel) + 10;
                ButtonGeneric resumeButton = new ButtonGeneric(x, y, resumeWidth, 20, resumeLabel);
                this.addButton(resumeButton, (b, mb) -> {
                            GatheringQueue.resume();
                            GuiBase.openGui(new GatheringPlanScreen());
                        });
                x += resumeWidth + 4;
            } else {
                String pauseLabel = "Pause";
                int pauseWidth = this.getStringWidth(pauseLabel) + 10;
                ButtonGeneric pauseButton = new ButtonGeneric(x, y, pauseWidth, 20, pauseLabel);
                this.addButton(pauseButton, (b, mb) -> {
                            GatheringQueue.pause();
                            GuiBase.openGui(new GatheringPlanScreen());
                        });
                x += pauseWidth + 4;
            }

            String stopLabel = "Stop Gathering";
            int stopWidth = this.getStringWidth(stopLabel) + 10;
            ButtonGeneric stopButton = new ButtonGeneric(x, y, stopWidth, 20, stopLabel);
            this.addButton(stopButton, (b, mb) -> {
                        GatheringQueue.stop();
                        GuiBase.openGui(new GatheringPlanScreen());
                    });
        }

        String mainMenuLabel = "Main Menu";
        int mainMenuWidth = this.getStringWidth(mainMenuLabel) + 20;
        ButtonGeneric mainMenuButton = new ButtonGeneric(this.width - mainMenuWidth - 10, this.height - 26, mainMenuWidth, 20, mainMenuLabel);
        this.addButton(mainMenuButton, (b, mb) -> GuiBase.openGui(new MainScreen()));
    }

    private String describeDeposit() {
        Optional<BetterBlockPos> pos = DepositLocations.get();
        return pos.map(p -> p.x + ", " + p.y + ", " + p.z).orElse("not set");
    }

    private String describeBiomeCache() {
        var world = MinecraftClient.getInstance().world;
        if (world == null) {
            return "Biome memory: not in a world";
        }

        int count = BiomeChunkCache.getRecordedChunkCount(world.getRegistryKey());
        return "Biome memory: " + count + " chunk(s) recorded in this dimension";
    }

    private String describeWorldKnowledge() {
        var world = MinecraftClient.getInstance().world;
        if (world == null) {
            return "World knowledge: not in a world";
        }

        String dimension = world.getRegistryKey()
                .getValue().toString();
        int chunks = WorldKnowledge.getKnownChunkCount(dimension);
        int resources = 0;
        for (String item : CalculationData.getRawMaterials().keySet()) {
            resources += WorldKnowledge.getKnownResourceCount(dimension, item);
        }
        return "World knowledge: " + chunks + " chunk(s), " + resources + " known resource position(s)";
    }

    private void setDepositToCurrentPosition() {
        var player = MinecraftClient.getInstance().player;
        if (player == null) {
            this.addMessage(MessageType.ERROR, "You need to be in a world for this.");
            return;
        }

        DepositLocations.set(new BetterBlockPos(player.getBlockPos()));
        /*
         * Refresh the label.
         */
        GuiBase.openGui(new GatheringPlanScreen());
    }

    private void startGathering(Map<String, Long> materials) {
        if (materials.isEmpty()) {
            this.addMessage(MessageType.ERROR, "Nothing on the Raw Materials list yet — load a schematic first.");
            return;
        }

        GatheringQueue.start(materials);
        GuiBase.openGui(new GatheringPlanScreen());
    }

    /**
     * Live status area.
     *
     * Unlike the labels created in initGui(), this is drawn
     * every frame so the current item/ETA/path updates live.
     */
    @Override
    protected void drawContents(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {

        super.drawContents(drawContext, mouseX, mouseY, partialTicks);
        int y = this.height - 100;
        if (!GatheringQueue.isRunning()) {
            this.drawString(drawContext, "Not currently gathering.", 12, y, 0xFFAAAAAA);
            return;
        }

        if (GatheringQueue.isPaused()) {
            this.drawString(drawContext, "Paused on: " + GatheringQueue.getCurrentItem() + "  (" + GatheringQueue.getRemainingCount() + " item(s) left on the list)", 12, y, 0xFFFFFF55);
            return;
        }

        this.drawString(drawContext, "Gathering: " + GatheringQueue.getCurrentItem() + "  (" + GatheringQueue.getRemainingCount() + " item(s) left)", 12, y, 0xFF55FF55);
        y += 12;
        this.drawString(drawContext, "Have: " + GatheringQueue.getCurrentInventory() + " / " + GatheringQueue.getCurrentRequired() + "  (need " + GatheringQueue.getCurrentStillNeeded() + " more)", 12, y, 0xFFFFFFFF);
        y += 12;
        Optional<Double> eta = GatheringQueue.getEstimatedSecondsRemaining();
        String etaText = eta.map(seconds -> String.format("~%.0fs remaining on this step", seconds)).orElse("Still working out a route...");
        this.drawString(drawContext, etaText, 12, y, 0xFFFFFFFF);
        y += 14;
        List<BetterBlockPos> positions = BaritoneAPI
                        .getProvider()
                        .getPrimaryBaritone()
                        .getPathingBehavior()
                        .getPath()
                        .map(IPath::positions)
                        .orElse(List.of());
        if (!positions.isEmpty()) {
            this.drawString(drawContext, "Next planned steps:", 12, y, 0xFFAAAAAA);
            y += 11;
            for (int i = 0; i < Math.min(MAX_PATH_COORDS_SHOWN, positions.size()); i++) {
                BetterBlockPos pos = positions.get(i);
                this.drawString(drawContext, pos.x + ", " + pos.y + ", " + pos.z, 16, y, 0xFFDDDDDD);
                y += 10;
            }
        }
    }
}