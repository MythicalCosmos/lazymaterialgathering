package net.lucy.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * Draws one TableRow: the item icon, then each column of text.
 */
public class TableRowWidget extends WidgetListEntryBase<TableRow>
{
    private final TableRow row;
    private final boolean isOdd;
    private final int[] columnPercents;
    private final ItemStack stack;

    public TableRowWidget(int x, int y, int width, int height, boolean isOdd,
                          TableRow row, int listIndex, int[] columnPercents)
    {
        super(x, y, width, height, row, listIndex);

        this.row = row;
        this.isOdd = isOdd;
        this.columnPercents = columnPercents;
        this.stack = TableRow.stackFor(row.itemName);
    }


    // Where a column starts. Column 0 leaves room for the item icon.
    private int getColumnX(int column)
    {
        if (column == 0)
        {
            return this.x + 22;
        }

        return this.x + this.width * this.columnPercents[column] / 100;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        // Lighter background for the hovered or selected row, alternating shades for the others
        if (selected || this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0x70FFFFFF);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0x20FFFFFF);
        }
        else
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0x50FFFFFF);
        }

        // The text columns. Text that is too long for its column gets cut off.
        for (int i = 0; i < this.row.columns.length && i < this.columnPercents.length; i++)
        {
            int columnX = this.getColumnX(i);
            int nextX = (i + 1 < this.columnPercents.length) ? this.getColumnX(i + 1) : this.x + this.width;
            String text = this.textRenderer.trimToWidth(this.row.columns[i], nextX - columnX - 4);

            this.drawString(columnX, this.y + 7, 0xFFFFFFFF, text, drawContext);
        }

        // The item icon
        if (this.stack.isEmpty() == false)
        {
            drawContext.getMatrices().push();
            RenderUtils.enableDiffuseLightingGui3D();

            RenderUtils.drawRect(this.x + 2, this.y + 3, 16, 16, 0x20FFFFFF);
            drawContext.drawItem(this.stack, this.x + 2, this.y + 3);

            RenderSystem.disableBlend();
            RenderUtils.disableDiffuseLighting();
            drawContext.getMatrices().pop();
        }

        super.render(mouseX, mouseY, selected, drawContext);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        if (this.row.hoverLines.isEmpty() == false && this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawHoverText(mouseX, mouseY, this.row.hoverLines, drawContext);
        }

        super.postRenderHovered(mouseX, mouseY, selected, drawContext);
    }

    @Override
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton)
    {
        if (this.row.onClick != null)
        {
            this.row.onClick.accept(mouseButton);
            return true;
        }

        return super.onMouseClickedImpl(mouseX, mouseY, mouseButton);
    }
}