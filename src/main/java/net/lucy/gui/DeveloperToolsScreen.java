package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.baritone.CraftingQueue;
import net.lucy.baritone.GatheringQueue;
import net.lucy.baritone.MaterialPipelineQueue;
import net.lucy.baritone.ProcessingQueue;
import net.lucy.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class DeveloperToolsScreen extends GuiBase {

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
                "Material Pipeline Test",
                () -> GuiBase.openGui(
                        new MaterialPipelineTestScreen()
                )
        );

        y += addMenuButton(
                x,
                y,
                width,
                "Pipeline Control",
                () -> GuiBase.openGui(
                        new MaterialPipelineControlScreen()
                )
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

        y += addMenuButton(
                x,
                y,
                width,
                "Resource Debug",
                () -> GuiBase.openGui(
                        new ResourceDebugScreen()
                )
        );

        y += 12;

        y += addMenuButton(
                x,
                y,
                width,
                "Stop All Automation",
                this::stopAll
        );

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

    private void stopAll() {
        MaterialPipelineQueue.stop();
        GatheringQueue.stop();
        ProcessingQueue.stop();
        CraftingQueue.stop();
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
                Text.literal("Developer Tools"),
                centerX,
                20,
                0xFFFFFF
        );

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal("DEV MODE"),
                centerX,
                38,
                0xFFAA00
        );

        super.render(
                drawContext,
                mouseX,
                mouseY,
                delta
        );

        renderStatus(
                drawContext,
                client,
                centerX,
                325
        );
    }

    private void renderStatus(
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
                0xCCCCCC
        );

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Gathering: "
                                + GatheringQueue.getState()
                ),
                centerX,
                y + 16,
                0xCCCCCC
        );

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Processing: "
                                + ProcessingQueue.getState()
                ),
                centerX,
                y + 32,
                0xCCCCCC
        );

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Crafting: "
                                + CraftingQueue.getState()
                ),
                centerX,
                y + 48,
                0xCCCCCC
        );
    }
}