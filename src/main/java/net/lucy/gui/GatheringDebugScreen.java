package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.baritone.GatheringQueue;
import net.lucy.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class GatheringDebugScreen extends GuiBase {

    private String message = "";
    private int messageColor = 0xFFFFFF;

    @Override
    public void initGui() {
        super.initGui();

        if (!Configs.Generic.DEV_MODE_ENABLED.getBooleanValue()) {
            return;
        }

        int width = 220;
        int x = this.width / 2 - width / 2;
        int y = 55;

        y += addMenuButton(
                x,
                y,
                width,
                "Pause / Resume",
                this::togglePause
        );

        y += addMenuButton(
                x,
                y,
                width,
                "Stop Gathering",
                this::stopGathering
        );

        y += 12;

        addMenuButton(
                x,
                y,
                width,
                "Close",
                this::closeScreen
        );
    }

    private int addMenuButton(
            int x,
            int y,
            int width,
            String label,
            Runnable action
    ) {
        ButtonGeneric button = new ButtonGeneric(
                x,
                y,
                width,
                20,
                label
        );

        this.addButton(
                button,
                (b, mouseButton) -> action.run()
        );

        return 24;
    }

    private void togglePause() {
        if (!GatheringQueue.isRunning()) {
            message = "Gathering is not running.";
            messageColor = 0xFFAA00;
            return;
        }

        if (GatheringQueue.isPaused()) {
            GatheringQueue.resume();

            message = "Gathering resumed.";
            messageColor = 0x55FF55;
        } else {
            GatheringQueue.pause();

            message = "Gathering paused.";
            messageColor = 0xFFAA00;
        }
    }

    private void stopGathering() {
        GatheringQueue.stop();

        message = "Gathering stopped.";
        messageColor = 0xFF5555;
    }

    private void closeScreen() {
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public void render(
            DrawContext drawContext,
            int mouseX,
            int mouseY,
            float delta
    ) {
        this.renderBackground(drawContext);

        MinecraftClient client =
                MinecraftClient.getInstance();

        int centerX = this.width / 2;

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal("Gathering Debug"),
                centerX,
                20,
                0xFFFFFF
        );

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal("Source-aware Baritone gathering"),
                centerX,
                38,
                0xAAAAAA
        );

        super.render(
                drawContext,
                mouseX,
                mouseY,
                delta
        );

        renderGatheringStatus(
                drawContext,
                client,
                centerX,
                165
        );

        if (!message.isBlank()) {
            drawContext.drawCenteredTextWithShadow(
                    client.textRenderer,
                    Text.literal(message),
                    centerX,
                    this.height - 28,
                    messageColor
            );
        }
    }

    private void renderGatheringStatus(
            DrawContext drawContext,
            MinecraftClient client,
            int centerX,
            int y
    ) {
        int left = centerX - 220;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "State: "
                        + GatheringQueue.getState(),
                left,
                y,
                0xFFFFFF
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Running: "
                        + GatheringQueue.isRunning(),
                left,
                y,
                0xCCCCCC
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Exploring: "
                        + GatheringQueue.isExploring(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        String item =
                GatheringQueue.getCurrentItem();

        if (item == null || item.isBlank()) {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Item: -",
                    left,
                    y,
                    0xAAAAAA
            );
        } else {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Item: "
                            + formatItem(item),
                    left,
                    y,
                    0xFFFFFF
            );

            y += 16;

            long required =
                    GatheringQueue.getCurrentRequired();

            long inventory =
                    GatheringQueue.getCurrentInventory();

            long stillNeeded =
                    GatheringQueue.getCurrentStillNeeded();

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Inventory: "
                            + inventory
                            + " / "
                            + required,
                    left,
                    y,
                    0xCCCCCC
            );

            y += 16;

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Still needed: "
                            + stillNeeded,
                    left,
                    y,
                    stillNeeded > 0L
                            ? 0xFFCC55
                            : 0x55FF55
            );
        }

        y += 20;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Tasks remaining: "
                        + GatheringQueue.getRemainingCount(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        String source =
                GatheringQueue.getCurrentResourceSource();

        if (source == null || source.isBlank()) {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Source: -",
                    left,
                    y,
                    0xAAAAAA
            );
        } else {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Source: "
                            + formatItem(source),
                    left,
                    y,
                    0xBBD8FF
            );
        }

        y += 18;

        BlockPos resourceTarget =
                GatheringQueue.getCurrentResourceTarget();

        if (resourceTarget != null) {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Resource target: "
                            + formatPosition(resourceTarget),
                    left,
                    y,
                    0xBBD8FF
            );

            y += 18;
        }

        BlockPos explorationTarget =
                GatheringQueue.getCurrentExplorationTarget();

        if (explorationTarget != null) {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Exploration target: "
                            + formatPosition(explorationTarget),
                    left,
                    y,
                    0xAAAAFF
            );

            y += 18;
        }

        Optional<Double> estimatedSeconds =
                GatheringQueue.getEstimatedSecondsRemaining();

        if (estimatedSeconds.isPresent()) {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Path ETA: "
                            + formatDuration(
                            estimatedSeconds.get()
                    ),
                    left,
                    y,
                    0xAAAAAA
            );

            y += 18;
        }

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Baritone pathing: "
                        + net.lucy.baritone.BaritoneController.isPathing(),
                left,
                y,
                0xCCCCCC
        );
    }

    private String formatItem(String item) {
        if (item == null || item.isBlank()) {
            return "-";
        }

        int separator =
                item.indexOf(':');

        if (separator >= 0
                && separator + 1 < item.length()) {

            return item.substring(
                    separator + 1
            );
        }

        return item;
    }

    private String formatPosition(BlockPos position) {
        if (position == null) {
            return "-";
        }

        return "("
                + position.getX()
                + ", "
                + position.getY()
                + ", "
                + position.getZ()
                + ")";
    }

    private String formatDuration(double seconds) {
        if (seconds < 0.0D) {
            return "-";
        }

        if (seconds < 60.0D) {
            return String.format(
                    "%.1fs",
                    seconds
            );
        }

        long totalSeconds =
                Math.round(seconds);

        long minutes =
                totalSeconds / 60L;

        long remainingSeconds =
                totalSeconds % 60L;

        return minutes
                + "m "
                + remainingSeconds
                + "s";
    }
}