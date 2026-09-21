package net.lucy.gui;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

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

    /** "oak_planks" -> "Oak Planks" (uses the game's own name when the item exists). */
    public static String displayName(String itemName)
    {
        Identifier id = Identifier.tryParse(itemName);

        if (id != null)
        {
            Item item = Registries.ITEM.get(id);

            if (item != Items.AIR)
            {
                return item.getName().getString();
            }
        }

        // Some block ids have no item (for example wall signs), so make a readable name ourselves
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
}