package net.lucy.gui;

import net.lucy.SchemParser;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Map;

public class MaterialsScreen extends Screen {

    private final Screen parent;

    private int scrollOffset = 0;

    public MaterialsScreen(Screen parent) {
        super(Text.literal("Materials"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), button -> close())
                        .dimensions(width / 2 - 50, height - 30, 100, 20)
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelLeft = width / 2 - 200;
        int panelTop = 30;
        GuiTheme.drawPanel(context, panelLeft, panelTop, 400, height - 70);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, GuiTheme.TITLE_COLOR);

        int y = 40;

        if (SchemParser.blockCounts == null || SchemParser.blockCounts.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("No schematic has been parsed yet."),
                    width / 2, y, GuiTheme.LABEL_COLOR
            );
            return;
        }

        int index = 0;
        for (Map.Entry<String, Long> entry : SchemParser.blockCounts.entrySet()) {
            int drawY = y + (index * 12) - scrollOffset;

            if (drawY >= panelTop + 10 && drawY <= panelTop + (height - 70) - 15) {
                String text = entry.getKey() + "  x" + entry.getValue();
                context.drawTextWithShadow(textRenderer, Text.literal(text), width / 2 - 190, drawY, 0xFFFFFF);
            }

            index++;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (SchemParser.blockCounts == null || SchemParser.blockCounts.isEmpty()) {
            return true;
        }

        scrollOffset -= (int) (verticalAmount * 12);
        if (scrollOffset < 0) scrollOffset = 0;

        int maxScroll = Math.max(0, SchemParser.blockCounts.size() * 12 - (height - 80));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        return true;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}