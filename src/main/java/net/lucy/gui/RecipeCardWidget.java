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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Draws one recipe the way it actually looks in-game: a 3x3 crafting grid, a single slot
 * for smelting/blasting/smoking/campfire cooking/stonecutting/smithing, or a brewing
 * stand's layout, with real item icons, an arrow, and the result — instead of just a text
 * list of ingredients.
 */
public class RecipeCardWidget extends WidgetListEntryBase<Recipe> {
    public static final int HEIGHT = 72;
    private static final int SLOT = 18;
    private final Recipe recipe;
    private final String outputName;
    private final boolean isOdd;
    // Screen position of each of the 9 grid slots (only used for CRAFTING)
    private final int[][] slotPositions = new int[9][2];
    private final int outputX;
    private final int outputY;
    public RecipeCardWidget(int x, int y, int width, int height, boolean isOdd, Recipe recipe, int listIndex, String outputName) {
        super(x, y, width, height, recipe, listIndex);
        this.recipe = recipe;
        this.outputName = outputName;
        this.isOdd = isOdd;
        int gridX = x + 10;
        int gridY = y + 20;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.slotPositions[row * 3 + col] = new int[] { gridX + col * SLOT, gridY + row * SLOT };
            }
        }
        this.outputX = gridX + 3 * SLOT + 24;
        this.outputY = gridY + SLOT;
    }

    // Recipe types that only ever have one ingredient slot (a furnace, smoker, blast
    // furnace, campfire, stonecutter or smithing table all show one item going in)
    private static boolean isSingleSlotType(RecipeType type) {
        return type == RecipeType.SMELTING || type == RecipeType.BLASTING || type == RecipeType.SMOKING || type == RecipeType.CAMPFIRE || type == RecipeType.STONECUTTING || type == RecipeType.SMITHING;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext drawContext) {
        RenderUtils.color(1f, 1f, 1f, 1f);
        RenderUtils.drawRect(this.x, this.y, this.width, this.height, this.isOdd ? 0x20FFFFFF : 0x50FFFFFF);
        this.drawString(this.x + 8, this.y + 4, 0xFFAAAAAA, this.getLabel(), drawContext);
        if (this.recipe.type == RecipeType.BREWING) {
            this.drawBrewing(drawContext);
        } else if (isSingleSlotType(this.recipe.type)) {
            this.drawSmelting(drawContext);
        } else {
            this.drawCraftingGrid(drawContext);
        }

        this.drawSlot(drawContext, this.outputX, this.outputY, this.outputName, this.recipe.outputCount);
        int arrowX = (isSingleSlotType(this.recipe.type) || this.recipe.type == RecipeType.BREWING) ? this.slotPositions[4][0] + SLOT + 4 : this.slotPositions[5][0] + SLOT + 4;
        this.drawString(arrowX, this.outputY + 5, 0xFFFFFFFF, "\u2192", drawContext);
        super.render(mouseX, mouseY, selected, drawContext);
    }

    private String getLabel() {
        if (this.recipe.type == RecipeType.BREWING) {
            // The real recipe id is a long "brewing/potion/potion_minecraft_awkward_..." string;
            // the icons already say what it does, so just note what kind of brew this is
            return "Brewing: makes " + this.recipe.outputCount;
        }

        if (isSingleSlotType(this.recipe.type)) {
            return TableRow.prettify(this.recipe.type.name()) + ": " + this.recipe.id;
        }
        return "Crafting: " + this.recipe.id;
    }

    private void drawCraftingGrid(DrawContext drawContext) {
        // The grid layout the game itself uses, when we have it (always true for recipes
        // read live from a world); otherwise pack the ingredients in reading order as the
        // closest approximation (this happens for recipes loaded from the offline cache).
        String[] slots = this.recipe.gridSlots != null ? this.recipe.gridSlots : packIngredients();
        for (int i = 0; i < 9; i++) {
            if (slots[i] != null) {
                int[] pos = this.slotPositions[i];
                this.drawSlot(drawContext, pos[0], pos[1], slots[i], 1);
            }
        }
    }

    private String[] packIngredients() {
        String[] slots = new String[9];
        int index = 0;
        for (Map.Entry<String, Integer> ingredient : this.recipe.ingredients.entrySet()) {
            for (int n = 0; n < ingredient.getValue() && index < 9; n++) {
                slots[index++] = ingredient.getKey();
            }
        }
        return slots;
    }

    private void drawSmelting(DrawContext drawContext) {
        String inputName = this.recipe.ingredients.keySet().iterator().next();
        int[] pos = this.slotPositions[4];
        this.drawSlot(drawContext, pos[0], pos[1], inputName, 1);
    }

    // RecipeExporter always inserts the base potion (or, for a splash/lingering
    // conversion, the item being converted) first and the modifier ingredient second --
    // see countFirstChoices() there -- so this can rely on that order rather than
    // guessing which is which from the item names.
    private String[] brewingSlots() {
        Iterator<String> ingredients = this.recipe.ingredients.keySet().iterator();
        String[] slots = new String[9];
        slots[7] = ingredients.hasNext() ? ingredients.next() : null; // the bottle(s), bottom-middle
        slots[1] = ingredients.hasNext() ? ingredients.next() : null; // the modifier, top-middle
        return slots;
    }

    private void drawBrewing(DrawContext drawContext) {
        String[] slots = this.brewingSlots();
        int bottleCount = this.recipe.type == RecipeType.BREWING && this.recipe.ingredients.size() == 2 ? new ArrayList<>(this.recipe.ingredients.values()).get(0) : 1;

        if (slots[7] != null) {
            int[] pos = this.slotPositions[7];
            this.drawSlot(drawContext, pos[0], pos[1], slots[7], bottleCount);
        }
        if (slots[1] != null) {
            int[] pos = this.slotPositions[1];
            this.drawSlot(drawContext, pos[0], pos[1], slots[1], 1);
        }
    }

    private void drawSlot(DrawContext drawContext, int x, int y, String itemName, int count) {
        RenderUtils.drawRect(x - 1, y - 1, SLOT, SLOT, 0x40000000);
        ItemStack stack = TableRow.stackFor(itemName);
        TableRow.drawItemIcon(drawContext, stack, x, y, itemName);
        if (count > 1 && stack.isEmpty() == false) {
            this.drawStringWithShadow(x + 10, y + 8, 0xFFFFFFFF, String.valueOf(count), drawContext);
        }
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, DrawContext drawContext) {
        RenderUtils.color(1f, 1f, 1f, 1f);
        String[] slots;
        if (this.recipe.type == RecipeType.BREWING) {
            slots = this.brewingSlots();
        } else if (isSingleSlotType(this.recipe.type) == false) {
            slots = this.recipe.gridSlots != null ? this.recipe.gridSlots : packIngredients();
        } else {
            slots = null;
        }

        if (isSingleSlotType(this.recipe.type)) {
            int[] pos = this.slotPositions[4];
            if (GuiBase.isMouseOver(mouseX, mouseY, pos[0], pos[1], SLOT, SLOT)) {
                this.showTooltip(drawContext, mouseX, mouseY, this.recipe.ingredients.keySet().iterator().next());
            }
        }
        else {
            for (int i = 0; i < 9; i++) {
                if (slots[i] == null) continue;
                int[] pos = this.slotPositions[i];
                if (GuiBase.isMouseOver(mouseX, mouseY, pos[0], pos[1], SLOT, SLOT)) {
                    this.showTooltip(drawContext, mouseX, mouseY, slots[i]);
                    break;
                }
            }
        }

        if (GuiBase.isMouseOver(mouseX, mouseY, this.outputX, this.outputY, SLOT, SLOT)) {
            this.showTooltip(drawContext, mouseX, mouseY, this.outputName);
        }
        super.postRenderHovered(mouseX, mouseY, selected, drawContext);
    }

    private void showTooltip(DrawContext drawContext, int mouseX, int mouseY, String itemName) {
        List<String> lines = new ArrayList<>();
        lines.add(TableRow.displayName(itemName));
        RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
    }
}