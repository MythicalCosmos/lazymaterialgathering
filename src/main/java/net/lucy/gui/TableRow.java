package net.lucy.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One line in a table screen (material list, raw materials, recipe choices).
 */
public class TableRow
{
    public final String itemName;      // e.g. "oak_planks", used to draw the item icon (can be null)
    public final String[] columns;     // the text shown in each column
    public final String copyText;      // what the "Copy List" button copies for this row
    public final String filterText;    // what the search box looks at

    public final List<String> hoverLines = new ArrayList<>();   // shown when the mouse is over the row (optional)

    @Nullable
    public IntConsumer onClick;        // called with the mouse button when the row is clicked (optional)

    public TableRow(@Nullable String itemName, String copyText, String... columns)
    {
        this.itemName = itemName;
        this.copyText = copyText;
        this.columns = columns;

        String firstColumn = columns.length > 0 ? columns[0] : "";
        this.filterText = ((itemName == null ? "" : itemName) + " " + firstColumn).toLowerCase();
    }

    // ---------- Small helpers shared by the screens ----------

    /**
     * The item to show for a name, whether that name is an item id ("stick") or a block id
     * ("oak_log"). A lot of blocks don't share their item's id at all — a wall torch is the
     * block "wall_torch" but the item "torch", "redstone_wire" is the item "redstone", and so
     * on — so this checks the block registry first and follows it to its real item, instead
     * of assuming the name is already an item id.
     */
    public static ItemStack stackFor(@Nullable String name)
    {
        Item item = resolveItem(name);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Nullable
    private static Item resolveItem(@Nullable String name)
    {
        if (name == null)
        {
            return null;
        }

        Identifier id = Identifier.tryParse(name);

        if (id == null)
        {
            return null;
        }

        if (Registries.BLOCK.containsId(id))
        {
            Block block = Registries.BLOCK.get(id);
            Item blockItem = Item.BLOCK_ITEMS.get(block);

            if (blockItem != null && blockItem != Items.AIR)
            {
                return blockItem;
            }
        }

        Item item = Registries.ITEM.get(id);
        return item == Items.AIR ? null : item;
    }

    /** "oak_planks" -> "Oak Planks" (uses the game's own name when the item exists). */
    public static String displayName(String itemName)
    {
        Item item = resolveItem(itemName);

        if (item != null)
        {
            return item.getName().getString();
        }

        // No item at all for this name (a fluid, a virtual block, ...); make a readable name ourselves
        return prettify(itemName);
    }

    /** "MOB_DROP" or "oak_wall_sign" -> "Mob Drop" / "Oak Wall Sign" */
    public static String prettify(String text)
    {
        StringBuilder result = new StringBuilder();

        for (String word : text.replace("minecraft:", "").split("[_:]"))
        {
            if (word.isEmpty())
            {
                continue;
            }

            if (result.length() > 0)
            {
                result.append(' ');
            }

            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }

        return result.toString();
    }

    /** 200 -> "3 x 64 + 8". Returns an empty text for less than one stack. */
    public static String stacks(long count)
    {
        if (count < 64)
        {
            return "";
        }

        long fullStacks = count / 64;
        long rest = count % 64;

        return rest == 0 ? fullStacks + " x 64" : fullStacks + " x 64 + " + rest;
    }

    // ---------- Shared, crash-proof item icon drawing ----------

    private static final Logger LOGGER = Logger.getLogger("LazyMaterialGathering");
    // Only log a given item once, ever (not every frame), so a broken icon doesn't spam the log.
    private static final Set<String> LOGGED_ICON_ERRORS = ConcurrentHashMap.newKeySet();

    /**
     * Draws one item icon at (x, y), the way every screen in this mod does it. Wrapped in
     * a try/catch: if item rendering throws for some reason, this logs it ONCE (check
     * logs/latest.log for "Failed to draw the icon" if icons aren't showing) instead of
     * silently failing every frame or taking the rest of the row down with it.
     */
    public static void drawItemIcon(DrawContext drawContext, ItemStack stack, int x, int y, String itemNameForLogging)
    {
        drawItemIcon(drawContext, stack, x, y, itemNameForLogging, false);
    }

    /** Same as above, but greys the icon out (used for a disabled recipe option). */
    public static void drawItemIcon(DrawContext drawContext, ItemStack stack, int x, int y, String itemNameForLogging, boolean dimmed)
    {
        if (stack.isEmpty())
        {
            return;
        }

        try
        {
            drawContext.getMatrices().push();
            RenderUtils.enableDiffuseLightingGui3D();

            if (dimmed)
            {
                RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1f);
            }

            drawContext.drawItem(stack, x, y);

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
            RenderUtils.disableDiffuseLighting();
            drawContext.getMatrices().pop();
        }
        catch (Exception e)
        {
            if (LOGGED_ICON_ERRORS.add(itemNameForLogging))
            {
                LOGGER.log(Level.WARNING, "Failed to draw the icon for '" + itemNameForLogging + "'", e);
            }
        }
    }
}