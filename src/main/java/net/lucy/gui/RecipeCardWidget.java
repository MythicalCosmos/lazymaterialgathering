package net.lucy.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.lucy.model.Recipe;
import net.lucy.model.RecipeType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Draws one recipe the way it actually looks in-game: a 3x3 crafting grid (or a single
 * slot for smelting) with real item icons, an arrow, and the result — instead of just a
 * text list of ingredients.
 */
public class RecipeCardWidget extends WidgetListEntryBase<Recipe>
{
    public static final int HEIGHT = 72;
    private static final int SLOT = 18;

    private final Recipe recipe;
    private final String outputName;
    private final boolean isOdd;

    // Screen position of each of the 9 grid slots (only used for CRAFTING)
    private final int[][] slotPositions = new int[9][2];
    private final int outputX;
    private final int outputY;

    public RecipeCardWidget(int x, int y, int width, int height, boolean isOdd,
                            Recipe recipe, int listIndex, String outputName)
    {
        super(x, y, width, height, recipe, listIndex);

        this.recipe = recipe;
        this.outputName = outputName;
        this.isOdd = isOdd;

        int gridX = x + 10;
        int gridY = y + 20;

        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 3; col++)
            {
                this.slotPositions[row * 3 + col] = new int[] { gridX + col * SLOT, gridY + row * SLOT };
            }
        }

        this.outputX = gridX + 3 * SLOT + 24;
        this.outputY = gridY + SLOT;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);
        RenderUtils.drawRect(this.x, this.y, this.width, this.height, this.isOdd ? 0x20FFFFFF : 0x50FFFFFF);

        String label = (this.recipe.type == RecipeType.SMELTING ? "Smelting: " : "Crafting: ") + this.recipe.id;
        this.drawString(this.x + 8, this.y + 4, 0xFFAAAAAA, label, drawContext);

        if (this.recipe.type == RecipeType.SMELTING)
        {
            this.drawSmelting(drawContext);
        }
        else
        {
            this.drawCraftingGrid(drawContext);
        }

        this.drawSlot(drawContext, this.outputX, this.outputY, this.outputName, this.recipe.outputCount);

        int arrowX = (this.recipe.type == RecipeType.SMELTING) ? this.slotPositions[4][0] + SLOT + 4 : this.slotPositions[5][0] + SLOT + 4;
        this.drawString(arrowX, this.outputY + 5, 0xFFFFFFFF, "\u2192", drawContext);

        super.render(mouseX, mouseY, selected, drawContext);
    }

    private void drawCraftingGrid(DrawContext drawContext)
    {
        // The grid layout the game itself uses, when we have it (always true for recipes
        // read live from a world); otherwise pack the ingredients in reading order as the
        // closest approximation (this happens for recipes loaded from the offline cache).
        String[] slots = this.recipe.gridSlots != null ? this.recipe.gridSlots : packIngredients();

        for (int i = 0; i < 9; i++)
        {
            if (slots[i] != null)
            {
                int[] pos = this.slotPositions[i];
                this.drawSlot(drawContext, pos[0], pos[1], slots[i], 1);
            }
        }
    }

    private String[] packIngredients()
    {
        String[] slots = new String[9];
        int index = 0;

        for (Map.Entry<String, Integer> ingredient : this.recipe.ingredients.entrySet())
        {
            for (int n = 0; n < ingredient.getValue() && index < 9; n++)
            {
                slots[index++] = ingredient.getKey();
            }
        }

        return slots;
    }

    private void drawSmelting(DrawContext drawContext)
    {
        String inputName = this.recipe.ingredients.keySet().iterator().next();
        int[] pos = this.slotPositions[4];
        this.drawSlot(drawContext, pos[0], pos[1], inputName, 1);
    }

    private void drawSlot(DrawContext drawContext, int x, int y, String itemName, int count)
    {
        RenderUtils.drawRect(x - 1, y - 1, SLOT, SLOT, 0x40000000);

        ItemStack stack = TableRow.stackFor(itemName);
        if (stack.isEmpty())
        {
            return;
        }

        drawContext.getMatrices().push();
        RenderUtils.enableDiffuseLightingGui3D();
        drawContext.drawItem(stack, x, y);
        RenderSystem.disableBlend();
        RenderUtils.disableDiffuseLighting();
        drawContext.getMatrices().pop();

        if (count > 1)
        {
            this.drawStringWithShadow(x + 10, y + 8, 0xFFFFFFFF, String.valueOf(count), drawContext);
        }
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        String[] slots = this.recipe.type == RecipeType.SMELTING ? null
                : (this.recipe.gridSlots != null ? this.recipe.gridSlots : packIngredients());

        if (this.recipe.type == RecipeType.SMELTING)
        {
            int[] pos = this.slotPositions[4];
            if (GuiBase.isMouseOver(mouseX, mouseY, pos[0], pos[1], SLOT, SLOT))
            {
                this.showTooltip(drawContext, mouseX, mouseY, this.recipe.ingredients.keySet().iterator().next());
            }
        }
        else
        {
            for (int i = 0; i < 9; i++)
            {
                if (slots[i] == null) continue;
                int[] pos = this.slotPositions[i];
                if (GuiBase.isMouseOver(mouseX, mouseY, pos[0], pos[1], SLOT, SLOT))
                {
                    this.showTooltip(drawContext, mouseX, mouseY, slots[i]);
                    break;
                }
            }
        }

        if (GuiBase.isMouseOver(mouseX, mouseY, this.outputX, this.outputY, SLOT, SLOT))
        {
            this.showTooltip(drawContext, mouseX, mouseY, this.outputName);
        }

        super.postRenderHovered(mouseX, mouseY, selected, drawContext);
    }

    private void showTooltip(DrawContext drawContext, int mouseX, int mouseY, String itemName)
    {
        List<String> lines = new ArrayList<>();
        lines.add(TableRow.displayName(itemName));
        RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
    }
}