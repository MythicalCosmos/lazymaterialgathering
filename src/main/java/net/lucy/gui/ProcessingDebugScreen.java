package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.baritone.ProcessingQueue;
import net.lucy.config.Configs;
import net.lucy.data.InventoryUtils;
import net.lucy.model.ProcessingTask;
import net.lucy.model.ProcessingTaskState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public class ProcessingDebugScreen extends GuiBase {

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
                "Stop Processing",
                this::stopProcessing
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

    private void togglePause() {
        if (!ProcessingQueue.isRunning()) {
            message = "Processing is not running.";
            messageColor = 0xFFAA00;
            return;
        }

        if (ProcessingQueue.isPaused()) {
            ProcessingQueue.resume();

            message = "Processing resumed.";
            messageColor = 0x55FF55;
        } else {
            ProcessingQueue.pause();

            message = "Processing paused.";
            messageColor = 0xFFAA00;
        }
    }

    private void stopProcessing() {
        ProcessingQueue.stop();

        message = "Processing stopped.";
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

        int centerX =
                this.width / 2;

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal("Processing Debug"),
                centerX,
                20,
                0xFFFFFF
        );

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Furnace / Blast Furnace / Smoker"
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

        renderProcessingStatus(
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

    private void renderProcessingStatus(
            DrawContext drawContext,
            MinecraftClient client,
            int centerX,
            int y
    ) {
        int left =
                centerX - 220;

        ProcessingTaskState state =
                ProcessingQueue.getState();

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "State: " + state,
                left,
                y,
                getStateColor(state)
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Running: "
                        + ProcessingQueue.isRunning(),
                left,
                y,
                0xCCCCCC
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Paused: "
                        + ProcessingQueue.isPaused(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        ProcessingTask task =
                ProcessingQueue.getCurrentTask();

        if (task == null) {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Task: -",
                    left,
                    y,
                    0xAAAAAA
            );

            y += 18;

            renderScreenStatus(
                    drawContext,
                    client,
                    left,
                    y
            );

            return;
        }

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Output: "
                        + formatItem(task.output()),
                left,
                y,
                0xFFFFFF
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Input: "
                        + formatItem(task.input()),
                left,
                y,
                0xBBD8FF
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Type: "
                        + task.type(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        long inputRequired =
                task.inputRequired();

        long inputInventory =
                InventoryUtils.count(
                        task.input()
                );

        long outputInventory =
                InventoryUtils.count(
                        task.output()
                );

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Input inventory: "
                        + inputInventory
                        + " / "
                        + inputRequired,
                left,
                y,
                inputInventory >= inputRequired
                        ? 0x55FF55
                        : 0xFF5555
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Output inventory: "
                        + outputInventory,
                left,
                y,
                0xCCCCCC
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Input / output: "
                        + task.inputPerOutput(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Required outputs: "
                        + task.required(),
                left,
                y,
                0xCCCCCC
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Crafting batches: "
                        + task.required()
                        + " / "
                        + task.inputPerOutput(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Tasks remaining: "
                        + ProcessingQueue.getRemainingTasks(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        renderProcessorScreen(
                drawContext,
                client,
                left,
                y
        );

        y += 70;

        String blocked =
                ProcessingQueue.getBlockedReason();

        if (blocked != null
                && !blocked.isBlank()) {

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Blocked:",
                    left,
                    y,
                    0xFF5555
            );

            y += 16;

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    truncate(
                            blocked,
                            65
                    ),
                    left,
                    y,
                    0xFF7777
            );
        }
    }

    private void renderProcessorScreen(
            DrawContext drawContext,
            MinecraftClient client,
            int left,
            int y
    ) {
        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Processor screen:",
                left,
                y,
                0xFFFFFF
        );

        y += 16;

        if (!(client.currentScreen
                instanceof HandledScreen<?> handled)) {

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "No handled screen open.",
                    left,
                    y,
                    0xAAAAAA
            );

            return;
        }

        ScreenHandler handler =
                handled.getScreenHandler();

        if (handler == null) {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Screen handler unavailable.",
                    left,
                    y,
                    0xFF5555
            );

            return;
        }

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Handler slots: "
                        + handler.slots.size(),
                left,
                y,
                0xCCCCCC
        );

        y += 16;

        int shown =
                0;

        for (int slot = 0;
             slot < handler.slots.size();
             slot++) {

            ItemStack stack =
                    handler.getSlot(slot).getStack();

            if (stack.isEmpty()) {
                continue;
            }

            String line =
                    "Slot "
                            + slot
                            + ": "
                            + formatItemStack(stack);

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    truncate(line, 65),
                    left,
                    y,
                    0xCCCCCC
            );

            y += 16;

            shown++;

            if (shown >= 3) {
                break;
            }
        }

        if (shown == 0) {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "No visible items in handler.",
                    left,
                    y,
                    0xAAAAAA
            );
        }
    }

    private void renderScreenStatus(
            DrawContext drawContext,
            MinecraftClient client,
            int left,
            int y
    ) {
        if (!(client.currentScreen
                instanceof HandledScreen<?>)) {

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Processor screen: closed",
                    left,
                    y,
                    0xAAAAAA
            );

            return;
        }

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Processor screen: open",
                left,
                y,
                0x55FF55
        );
    }

    private int getStateColor(
            ProcessingTaskState state
    ) {
        return switch (state) {
            case IDLE ->
                    0xAAAAAA;

            case FINDING_PROCESSOR,
                 OPENING_PROCESSOR,
                 LOADING_INPUT,
                 LOADING_FUEL ->
                    0xFFCC55;

            case PROCESSING,
                 COLLECTING,
                 VERIFYING ->
                    0x55FF55;

            case COMPLETE ->
                    0x55FF55;

            case BLOCKED ->
                    0xFF5555;

            case PAUSED ->
                    0xFFAA00;
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

    private String formatItemStack(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return "-";
        }

        String id =
                stack.getItem()
                        .toString();

        return id
                + " x"
                + stack.getCount();
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