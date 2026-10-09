package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.baritone.CraftingQueue;
import net.lucy.config.Configs;
import net.lucy.data.InventoryUtils;
import net.lucy.model.CraftingTask;
import net.lucy.model.CraftingTaskState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

import java.util.Map;

public class CraftingDebugScreen extends GuiBase {

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
                "Stop Crafting",
                this::stopCrafting
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
        if (!CraftingQueue.isRunning()) {
            message = "Crafting is not running.";
            messageColor = 0xFFAA00;
            return;
        }

        if (CraftingQueue.isPaused()) {
            CraftingQueue.resume();

            message = "Crafting resumed.";
            messageColor = 0x55FF55;
        } else {
            CraftingQueue.pause();

            message = "Crafting paused.";
            messageColor = 0xFFAA00;
        }
    }

    private void stopCrafting() {
        CraftingQueue.stop();

        message = "Crafting stopped.";
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
                Text.literal("Crafting Debug"),
                centerX,
                20,
                0xFFFFFF
        );

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Recipe execution and inventory verification"
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

        renderCraftingStatus(
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

    private void renderCraftingStatus(
            DrawContext drawContext,
            MinecraftClient client,
            int centerX,
            int y
    ) {
        int left =
                centerX - 220;

        CraftingTaskState state =
                CraftingQueue.getState();

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
                        + CraftingQueue.isRunning(),
                left,
                y,
                0xCCCCCC
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Paused: "
                        + CraftingQueue.isPaused(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        CraftingTask task =
                CraftingQueue.getCurrentTask();

        if (task == null) {
            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Task: -",
                    left,
                    y,
                    0xAAAAAA
            );

            y += 20;

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
                "Item: "
                        + formatItem(task.item()),
                left,
                y,
                0xFFFFFF
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Recipe: "
                        + truncate(
                        task.recipeId(),
                        55
                ),
                left,
                y,
                0xBBD8FF
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Requires table: "
                        + task.requiresCraftingTable(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Required: "
                        + task.required(),
                left,
                y,
                0xCCCCCC
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Crafts: "
                        + task.crafts(),
                left,
                y,
                0xCCCCCC
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Output / craft: "
                        + task.outputPerCraft(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        long current =
                InventoryUtils.count(
                        task.item()
                );

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Inventory: "
                        + current
                        + " / "
                        + task.required(),
                left,
                y,
                current >= task.required()
                        ? 0x55FF55
                        : 0xCCCCCC
        );

        y += 20;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Ingredients:",
                left,
                y,
                0xFFFFFF
        );

        y += 16;

        for (Map.Entry<String, Long> entry :
                task.ingredients().entrySet()) {

            long available =
                    InventoryUtils.count(
                            entry.getKey()
                    );

            String line =
                    formatItem(entry.getKey())
                            + " "
                            + available
                            + "/"
                            + entry.getValue();

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    truncate(line, 55),
                    left,
                    y,
                    available >= entry.getValue()
                            ? 0x55FF55
                            : 0xFF5555
            );

            y += 16;
        }

        y += 4;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Tasks remaining: "
                        + CraftingQueue.getRemainingTasks(),
                left,
                y,
                0xCCCCCC
        );

        y += 20;

        renderScreenStatus(
                drawContext,
                client,
                left,
                y
        );

        y += 20;

        String blocked =
                CraftingQueue.getBlockedReason();

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

    private void renderScreenStatus(
            DrawContext drawContext,
            MinecraftClient client,
            int left,
            int y
    ) {
        if (!(client.currentScreen
                instanceof HandledScreen<?> handled)) {

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Crafting screen: closed",
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
                    "Crafting screen: handler unavailable",
                    left,
                    y,
                    0xFF5555
            );

            return;
        }

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Crafting screen: open",
                left,
                y,
                0x55FF55
        );

        y += 16;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Handler slots: "
                        + handler.slots.size(),
                left,
                y,
                0xCCCCCC
        );

        int shown = 0;

        for (int slot = 0;
             slot < handler.slots.size();
             slot++) {

            ItemStack stack =
                    handler.getSlot(slot).getStack();

            if (stack.isEmpty()) {
                continue;
            }

            if (shown >= 3) {
                break;
            }

            y += 16;

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    truncate(
                            "Slot "
                                    + slot
                                    + ": "
                                    + formatStack(stack),
                            60
                    ),
                    left,
                    y,
                    0xCCCCCC
            );

            shown++;
        }
    }

    private int getStateColor(
            CraftingTaskState state
    ) {
        return switch (state) {
            case IDLE ->
                    0xAAAAAA;

            case OPENING_INVENTORY,
                 WAITING_FOR_CRAFTING_TABLE,
                 FILLING_RECIPE ->
                    0xFFCC55;

            case CRAFTING,
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

    private String formatStack(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return "-";
        }

        return formatItem(
                stack.getItem()
                        .toString()
        )
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