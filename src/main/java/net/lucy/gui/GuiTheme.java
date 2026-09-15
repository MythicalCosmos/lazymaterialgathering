package net.lucy.gui;

import net.minecraft.client.gui.DrawContext;

public class GuiTheme {

    public static final int PANEL_FILL = 0xD0101010;
    public static final int PANEL_BORDER = 0xFF707070;
    public static final int TITLE_COLOR = 0xFFFFFF;
    public static final int LABEL_COLOR = 0xAAAAAA;
    public static final int STATUS_COLOR = 0xFFFF55;

    public static void drawPanel(DrawContext context, int left, int top, int width, int height) {
        context.fill(left, top, left + width, top + height, PANEL_FILL);
        context.drawBorder(left, top, width, height, PANEL_BORDER);
    }
}