package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.baritone.CraftingQueue;
import net.lucy.baritone.GatheringQueue;
import net.lucy.baritone.MaterialPipelineQueue;
import net.lucy.baritone.ProcessingQueue;
import net.lucy.config.Configs;
import net.lucy.model.CraftingTaskState;
import net.lucy.model.GatheringTaskState;
import net.lucy.model.ProcessingTaskState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class MaterialPipelineControlScreen extends GuiBase {

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
                "Pause / Resume Pipeline",
                this::togglePipeline
        );

        y += addMenuButton(
                x,
                y,
                width,
                "Stop Pipeline",
                this::stopPipeline
        );

        y += addMenuButton(
                x,
                y,
                width,
                "Gathering Debug",
                () -> GuiBase.openGui(
                        new GatheringDebugScreen()
                )
        );

        y += addMenuButton(
                x,
                y,
                width,
                "Processing Debug",
                () -> GuiBase.openGui(
                        new ProcessingDebugScreen()
                )
        );

        y += addMenuButton(
                x,
                y,
                width,
                "Crafting Debug",
                () -> GuiBase.openGui(
                        new CraftingDebugScreen()
                )
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
        ButtonGeneric button =
                new ButtonGeneric(
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

    private void togglePipeline() {
        if (!MaterialPipelineQueue.isRunning()
                && !MaterialPipelineQueue.isPaused()) {

            message =
                    "Pipeline is not running.";

            messageColor =
                    0xFFAA00;

            return;
        }

        if (MaterialPipelineQueue.isPaused()) {
            MaterialPipelineQueue.resume();

            message =
                    "Pipeline resumed.";

            messageColor =
                    0x55FF55;

            return;
        }

        MaterialPipelineQueue.pause();

        message =
                "Pipeline paused.";

        messageColor =
                0xFFAA00;
    }

    private void stopPipeline() {
        MaterialPipelineQueue.stop();

        message =
                "Pipeline stopped.";

        messageColor =
                0xFF5555;
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

        int centerX =
                this.width / 2;

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Material Pipeline Control"
                ),
                centerX,
                20,
                0xFFFFFF
        );

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Gather -> Process -> Craft"
                ),
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

        renderPipelineStatus(
                drawContext,
                client,
                centerX,
                215
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

    private void renderPipelineStatus(
            DrawContext drawContext,
            MinecraftClient client,
            int centerX,
            int y
    ) {
        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Pipeline: "
                                + MaterialPipelineQueue.getState()
                ),
                centerX,
                y,
                getPipelineColor()
        );

        y += 18;

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Current item: "
                                + formatItem(
                                MaterialPipelineQueue
                                        .getCurrentItem()
                        )
                ),
                centerX,
                y,
                0xCCCCCC
        );

        y += 18;

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Tasks remaining: "
                                + MaterialPipelineQueue
                                .getRemainingTasks()
                ),
                centerX,
                y,
                0xCCCCCC
        );

        y += 22;

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Gathering: "
                                + GatheringQueue.getState()
                ),
                centerX,
                y,
                getGatheringColor()
        );

        y += 16;

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Processing: "
                                + ProcessingQueue.getState()
                ),
                centerX,
                y,
                getProcessingColor()
        );

        y += 16;

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Crafting: "
                                + CraftingQueue.getState()
                ),
                centerX,
                y,
                getCraftingColor()
        );

        y += 22;

        String blocked =
                MaterialPipelineQueue.getBlockedReason();

        if (blocked != null
                && !blocked.isBlank()) {

            drawContext.drawCenteredTextWithShadow(
                    client.textRenderer,
                    Text.literal(
                            "BLOCKED: "
                                    + truncate(
                                    blocked,
                                    55
                            )
                    ),
                    centerX,
                    y,
                    0xFF5555
            );
        }
    }

    private int getPipelineColor() {
        if (MaterialPipelineQueue.isPaused()) {
            return 0xFFAA00;
        }

        return switch (
                MaterialPipelineQueue.getState()
                ) {
            case IDLE ->
                    0xAAAAAA;

            case COMPLETE ->
                    0x55FF55;

            case BLOCKED ->
                    0xFF5555;

            case PAUSED ->
                    0xFFAA00;

            default ->
                    0x55FF55;
        };
    }

    private int getGatheringColor() {
        GatheringTaskState state =
                GatheringQueue.getState();

        return switch (state) {
            case COMPLETE ->
                    0x55FF55;

            case BLOCKED ->
                    0xFF5555;

            case PAUSED ->
                    0xFFAA00;

            default ->
                    0xCCCCCC;
        };
    }

    private int getProcessingColor() {
        ProcessingTaskState state =
                ProcessingQueue.getState();

        return switch (state) {
            case COMPLETE ->
                    0x55FF55;

            case BLOCKED ->
                    0xFF5555;

            case PAUSED ->
                    0xFFAA00;

            default ->
                    0xCCCCCC;
        };
    }

    private int getCraftingColor() {
        CraftingTaskState state =
                CraftingQueue.getState();

        return switch (state) {
            case COMPLETE ->
                    0x55FF55;

            case BLOCKED ->
                    0xFF5555;

            case PAUSED ->
                    0xFFAA00;

            default ->
                    0xCCCCCC;
        };
    }

    private String formatItem(
            String item
    ) {
        if (item == null
                || item.isBlank()) {
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

    private String truncate(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(
                0,
                Math.max(
                        0,
                        maxLength - 3
                )
        ) + "...";
    }
}