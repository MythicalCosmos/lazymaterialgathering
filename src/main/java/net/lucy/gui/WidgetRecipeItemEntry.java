package net.lucy.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.lucy.calc.RawMaterials;
import net.lucy.config.Configs;
import net.lucy.data.DataManager;
import net.lucy.model.Recipe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One row on the Preferred Recipes screen: an item, and (when expanded) a grid of
 * icons, one per recipe that makes it. This is the "dropdown" — clicking the header
 * expands it in place rather than opening a separate floating panel.
 *
 * Left click an icon toggles that recipe on or off. Right click makes it the preferred
 * recipe (and turns it on if it was off). The preferred recipe has a gold border, an
 * off recipe is dimmed, and both show a check or cross mark in the corner.
 */
public class WidgetRecipeItemEntry extends WidgetListEntryBase<RecipeItemEntry>
{
    private static final int ICON_SIZE = 20;   // the clickable box per recipe, including its margin
    private static final int ICON_INNER = 16;  // the item icon itself

    private final WidgetListRecipeItems parent;
    private final RecipeItemEntry recipeEntry;
    private final boolean isOdd;
    private final ItemStack headerStack;

    // Screen position of each recipe icon, in the same order as recipeEntry.options
    private final List<int[]> iconPositions = new ArrayList<>();

    private final int arrowX;

    public WidgetRecipeItemEntry(int x, int y, int width, int height, boolean isOdd,
                                 RecipeItemEntry entry, int listIndex, WidgetListRecipeItems parent)
    {
        super(x, y, width, height, entry, listIndex);

        this.parent = parent;
        this.recipeEntry = entry;
        this.isOdd = isOdd;
        this.headerStack = itemStack(entry.itemName);
        this.arrowX = x + width - 16;

        if (entry.expanded)
        {
            this.layoutIcons();
        }
    }

    // Works out where each recipe's icon goes, wrapping to a new line when the row is full
    private void layoutIcons()
    {
        int perRow = Math.max(1, (this.width - 8) / ICON_SIZE);
        int startX = this.x + 6;
        int startY = this.y + 24; // below the header

        for (int i = 0; i < this.recipeEntry.options.size(); i++)
        {
            int col = i % perRow;
            int row = i / perRow;
            this.iconPositions.add(new int[] { startX + col * ICON_SIZE, startY + row * ICON_SIZE });
        }
    }

    /** How tall this row needs to be, used by WidgetListRecipeItems.getBrowserEntryHeightFor(). */
    public static int heightFor(RecipeItemEntry entry, int width)
    {
        if (entry.expanded == false)
        {
            return 22;
        }

        int perRow = Math.max(1, (width - 8) / ICON_SIZE);
        int rows = (int) Math.ceil(entry.options.size() / (double) perRow);

        return 24 + rows * ICON_SIZE + 6;
    }

    private static ItemStack itemStack(String itemName)
    {
        Identifier id = Identifier.tryParse(itemName);
        if (id == null) return ItemStack.EMPTY;

        Item item = Registries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    // The item shown for one recipe option: its first listed ingredient, so recipes for the
    // same output (e.g. "stick from planks" vs "stick from bamboo") look different at a glance.
    private static ItemStack representativeStack(Recipe recipe)
    {
        for (String ingredientName : recipe.ingredients.keySet())
        {
            ItemStack stack = itemStack(ingredientName);
            if (stack.isEmpty() == false)
            {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        int backgroundColor = this.isOdd ? 0x20FFFFFF : 0x50FFFFFF;
        RenderUtils.drawRect(this.x, this.y, this.width, this.height, backgroundColor);

        // ---- Header: item icon, name, summary, expand arrow ----

        if (this.headerStack.isEmpty() == false)
        {
            drawContext.getMatrices().push();
            RenderUtils.enableDiffuseLightingGui3D();
            drawContext.drawItem(this.headerStack, this.x + 2, this.y + 3);
            RenderSystem.disableBlend();
            RenderUtils.disableDiffuseLighting();
            drawContext.getMatrices().pop();
        }

        this.drawString(this.x + 22, this.y + 7, 0xFFFFFFFF, TableRow.displayName(this.recipeEntry.itemName), drawContext);

        List<Recipe> enabledOptions = RawMaterials.getEnabledOptions(this.recipeEntry.itemName, this.recipeEntry.options);
        Recipe preferred = RawMaterials.selectRecipe(this.recipeEntry.itemName, this.recipeEntry.options);
        String summary = enabledOptions.size() + "/" + this.recipeEntry.options.size() + " enabled, using " + preferred.id;
        this.drawString(this.x + this.width / 2, this.y + 7, 0xFFAAAAAA, summary, drawContext);

        String arrow = this.recipeEntry.expanded ? "\u25BC" : "\u25B6";
        this.drawString(this.arrowX, this.y + 7, 0xFFFFFFFF, arrow, drawContext);

        // ---- Expanded: one icon per recipe option ----

        if (this.recipeEntry.expanded)
        {
            for (int i = 0; i < this.recipeEntry.options.size(); i++)
            {
                Recipe option = this.recipeEntry.options.get(i);
                int[] pos = this.iconPositions.get(i);
                boolean enabled = Configs.isRecipeEnabled(this.recipeEntry.itemName, option.id);
                boolean isPreferred = option == preferred;

                int slotColor = isPreferred ? 0x90FFD700 : (enabled ? 0x4000FF00 : 0x50000000);
                RenderUtils.drawRect(pos[0], pos[1], ICON_INNER, ICON_INNER, slotColor);

                ItemStack stack = representativeStack(option);
                if (stack.isEmpty() == false)
                {
                    drawContext.getMatrices().push();
                    RenderUtils.enableDiffuseLightingGui3D();

                    if (enabled == false)
                    {
                        RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1f); // dim disabled recipes
                    }

                    drawContext.drawItem(stack, pos[0], pos[1]);

                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                    RenderSystem.disableBlend();
                    RenderUtils.disableDiffuseLighting();
                    drawContext.getMatrices().pop();
                }

                // A small check or cross in the corner, so the state is clear even with no item icon
                String mark = enabled ? GuiBase.TXT_GREEN + "\u2713" : GuiBase.TXT_RED + "\u2715";
                this.drawStringWithShadow(pos[0] + ICON_INNER - 5, pos[1] - 2, 0xFFFFFFFF, mark, drawContext);
            }
        }

        super.render(mouseX, mouseY, selected, drawContext);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        if (this.recipeEntry.expanded)
        {
            for (int i = 0; i < this.recipeEntry.options.size(); i++)
            {
                int[] pos = this.iconPositions.get(i);

                if (GuiBase.isMouseOver(mouseX, mouseY, pos[0], pos[1], ICON_INNER, ICON_INNER))
                {
                    RenderUtils.drawHoverText(mouseX, mouseY, describe(this.recipeEntry.options.get(i)), drawContext);
                    break;
                }
            }
        }
        else if (GuiBase.isMouseOver(mouseX, mouseY, this.arrowX, this.y, 14, this.height))
        {
            List<String> lines = List.of("Click to see every recipe for this item");
            RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
        }

        super.postRenderHovered(mouseX, mouseY, selected, drawContext);
    }

    private static List<String> describe(Recipe recipe)
    {
        List<String> lines = new ArrayList<>();
        lines.add(GuiBase.TXT_GOLD + recipe.id);

        StringBuilder ingredients = new StringBuilder();
        for (Map.Entry<String, Integer> ingredient : recipe.ingredients.entrySet())
        {
            if (ingredients.length() > 0) ingredients.append(", ");
            ingredients.append(ingredient.getValue()).append("x ").append(TableRow.displayName(ingredient.getKey()));
        }
        lines.add(ingredients + " -> makes " + recipe.outputCount);
        lines.add("");
        lines.add(GuiBase.TXT_GRAY + "Left click: turn on/off.  Right click: make preferred.");

        return lines;
    }

    @Override
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton)
    {
        if (this.recipeEntry.expanded)
        {
            for (int i = 0; i < this.recipeEntry.options.size(); i++)
            {
                int[] pos = this.iconPositions.get(i);

                if (GuiBase.isMouseOver(mouseX, mouseY, pos[0], pos[1], ICON_INNER, ICON_INNER))
                {
                    this.handleIconClick(this.recipeEntry.options.get(i), mouseButton);
                    return true;
                }
            }
        }

        // Clicking anywhere on the header line (not just the arrow) expands or collapses the row
        if (mouseButton == 0 && mouseY < this.y + 22 && this.isMouseOver(mouseX, mouseY))
        {
            this.recipeEntry.expanded = !this.recipeEntry.expanded;
            this.parent.refreshEntries();
            return true;
        }

        return super.onMouseClickedImpl(mouseX, mouseY, mouseButton);
    }

    private void handleIconClick(Recipe option, int mouseButton)
    {
        String itemName = this.recipeEntry.itemName;

        if (mouseButton == 1) // right click: make preferred
        {
            Configs.setRecipeEnabled(itemName, option.id, true);
            Configs.recipePreferences.put(itemName, option.id);
        }
        else // left click: toggle on/off
        {
            boolean currentlyEnabled = Configs.isRecipeEnabled(itemName, option.id);
            List<Recipe> enabled = RawMaterials.getEnabledOptions(itemName, this.recipeEntry.options);

            if (currentlyEnabled && enabled.size() <= 1)
            {
                this.parent.getScreen().addMessage(fi.dy.masa.malilib.gui.Message.MessageType.ERROR,
                        "At least one recipe has to stay enabled for %s", TableRow.displayName(itemName));
                return;
            }

            Configs.setRecipeEnabled(itemName, option.id, !currentlyEnabled);

            // If we just turned off the preferred recipe, hand "preferred" to another enabled one
            String preferredId = Configs.recipePreferences.get(itemName);
            if (currentlyEnabled && option.id.equals(preferredId))
            {
                List<Recipe> stillEnabled = RawMaterials.getEnabledOptions(itemName, this.recipeEntry.options);
                Configs.recipePreferences.put(itemName, stillEnabled.get(0).id);
            }
        }

        Configs.saveToFile();
        DataManager.recalculateRawMaterials();
    }
}