package net.lucy.gui;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.calc.IPath;
import baritone.api.utils.BetterBlockPos;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.baritone.DepositLocations;
import net.lucy.baritone.GatheringQueue;
import net.lucy.data.DataManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Where you actually kick off Baritone gathering the raw materials list, set where they
 * should be dropped off, and watch it work.
 *
 * On coordinates and time estimates: Baritone doesn't know where an unfound ore actually
 * is until it scans the area for it, so there's no way to show "everything it's going to
 * do" before it starts -- that information doesn't exist yet. What this screen shows
 * instead, once gathering is running, is real and live: the path Baritone has actually
 * planned right now (its next several moves' coordinates), and its own time estimate for
 * finishing that (via the same numbers behind Baritone's "#eta" command). Both update
 * continuously and only cover what's currently in progress, not the whole run in advance.
 */
public class GatheringPlanScreen extends GuiBase
{
    private static final int MAX_PATH_COORDS_SHOWN = 12;

    @Override
    public void initGui()
    {
        super.initGui();

        int y = 30;

        this.addLabel(12, y, 300, 12, 0xFFFFFFFF, "Deposit location: " + describeDeposit());
        y += 16;

        String setDepositLabel = "Use My Current Position";
        int setDepositWidth = this.getStringWidth(setDepositLabel) + 10;
        ButtonGeneric setDepositButton = new ButtonGeneric(12, y, setDepositWidth, 20, setDepositLabel);
        this.addButton(setDepositButton, (b, mb) -> this.setDepositToCurrentPosition());
        y += 26;

        Map<String, Long> materials = DataManager.getRawMaterials();
        String summary = materials.size() + " raw material(s) on the list (see Raw Materials for the full breakdown)";
        this.addLabel(12, y, this.getStringWidth(summary) + 2, 12, 0xFFAAAAAA, summary);
        y += 20;

        if (GatheringQueue.isRunning())
        {
            String stopLabel = "Stop Gathering";
            int stopWidth = this.getStringWidth(stopLabel) + 10;
            ButtonGeneric stopButton = new ButtonGeneric(12, y, stopWidth, 20, stopLabel);
            this.addButton(stopButton, (b, mb) -> { GatheringQueue.stop(); GuiBase.openGui(new GatheringPlanScreen()); });
        }
        else
        {
            String startLabel = "Start Gathering";
            int startWidth = this.getStringWidth(startLabel) + 10;
            ButtonGeneric startButton = new ButtonGeneric(12, y, startWidth, 20, startLabel);
            this.addButton(startButton, (b, mb) -> this.startGathering(materials));
        }

        String mainMenuLabel = "Main Menu";
        int mainMenuWidth = this.getStringWidth(mainMenuLabel) + 20;
        ButtonGeneric mainMenuButton = new ButtonGeneric(this.width - mainMenuWidth - 10, this.height - 26, mainMenuWidth, 20, mainMenuLabel);
        this.addButton(mainMenuButton, (b, mb) -> GuiBase.openGui(new MainScreen()));
    }

    private String describeDeposit()
    {
        Optional<BetterBlockPos> pos = DepositLocations.get();
        return pos.map(p -> p.x + ", " + p.y + ", " + p.z).orElse("not set");
    }

    private void setDepositToCurrentPosition()
    {
        var player = MinecraftClient.getInstance().player;

        if (player == null)
        {
            this.addMessage(MessageType.ERROR, "You need to be in a world for this.");
            return;
        }

        DepositLocations.set(new BetterBlockPos(player.getBlockPos()));
        GuiBase.openGui(new GatheringPlanScreen()); // refresh the label
    }

    private void startGathering(Map<String, Long> materials)
    {
        if (materials.isEmpty())
        {
            this.addMessage(MessageType.ERROR, "Nothing on the Raw Materials list yet \u2014 load a schematic first.");
            return;
        }

        GatheringQueue.start(materials);
        GuiBase.openGui(new GatheringPlanScreen());
    }

    // Live status: redrawn every frame, unlike the buttons/labels above which are only
    // built once in initGui(). This is where the current item, ETA, and planned path
    // coordinates actually update in real time.
    //
    // NOTE: GuiBase.drawString takes (drawContext, text, x, y, color) -- NOT
    // (x, y, color, text, drawContext) like the list-entry widgets (WidgetBase) elsewhere
    // in this mod use. Screens and widgets have different drawString signatures in
    // malilib; mixing them up here was the actual bug.
    @Override
    protected void drawContents(DrawContext drawContext, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(drawContext, mouseX, mouseY, partialTicks);

        int y = this.height - 100;

        if (GatheringQueue.isRunning() == false)
        {
            this.drawString(drawContext, "Not currently gathering.", 12, y, 0xFFAAAAAA);
            return;
        }

        this.drawString(drawContext, "Gathering: " + GatheringQueue.getCurrentItem()
                + "  (" + GatheringQueue.getRemainingCount() + " item(s) left on the list)", 12, y, 0xFF55FF55);
        y += 12;

        Optional<Double> eta = GatheringQueue.getEstimatedSecondsRemaining();
        String etaText = eta.map(seconds -> String.format("~%.0fs remaining on this step", seconds))
                .orElse("Still working out a route...");
        this.drawString(drawContext, etaText, 12, y, 0xFFFFFFFF);
        y += 14;

        List<BetterBlockPos> positions = BaritoneAPI.getProvider().getPrimaryBaritone()
                .getPathingBehavior().getPath().map(IPath::positions).orElse(List.of());

        if (positions.isEmpty() == false)
        {
            this.drawString(drawContext, "Next planned steps:", 12, y, 0xFFAAAAAA);
            y += 11;

            for (int i = 0; i < Math.min(MAX_PATH_COORDS_SHOWN, positions.size()); i++)
            {
                BetterBlockPos pos = positions.get(i);
                this.drawString(drawContext, pos.x + ", " + pos.y + ", " + pos.z, 16, y, 0xFFDDDDDD);
                y += 10;
            }
        }
    }
}