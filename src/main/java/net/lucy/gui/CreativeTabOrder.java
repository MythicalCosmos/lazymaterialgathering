package net.lucy.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Orders items the way the creative inventory's tabs do (Building Blocks, then Redstone,
 * then Tools, and so on), so the Preferred Recipes list reads the way people already browse
 * items in-game, instead of a flat alphabetical list.
 *
 * This is the one piece of this mod that reaches into Minecraft's own creative-tab code
 * rather than malilib or Baritone, and I haven't been able to compile it against the real
 * game to confirm the exact method names on your Minecraft version. If it fails to build,
 * everything still falls back to alphabetical order (getIndex() just returns a very large
 * number for everything) — send me the exact compiler error and I'll adjust it.
 */
public class CreativeTabOrder
{
    @Nullable
    private static Map<Item, Integer> order;

    /** Where this item sits in the creative tabs. Lower = earlier. Unknown items sort last. */
    public static int getIndex(String itemName)
    {
        ensureBuilt();

        Identifier id = Identifier.tryParse(itemName);
        Item item = id == null ? Items.AIR : Registries.ITEM.get(id);

        return order.getOrDefault(item, Integer.MAX_VALUE);
    }

    private static void ensureBuilt()
    {
        if (order != null)
        {
            return;
        }

        order = new HashMap<>();

        try
        {
            MinecraftClient client = MinecraftClient.getInstance();
            DynamicRegistryManager registryManager = client.world != null
                    ? client.world.getRegistryManager()
                    : DynamicRegistryManager.EMPTY;

            // Populates every group's display list, the same call the creative inventory
            // screen makes before it shows itself.
            ItemGroups.updateDisplayContext(FeatureSet.of(FeatureFlags.VANILLA), false, registryManager);

            int index = 0;
            for (ItemGroup group : Registries.ITEM_GROUP)
            {
                for (ItemStack stack : group.getDisplayStacks())
                {
                    order.putIfAbsent(stack.getItem(), index);
                }
                index++;
            }
        }
        catch (Exception e)
        {
            // Leave "order" empty: getIndex() then returns Integer.MAX_VALUE for everything,
            // and the screen's own sort just falls back to alphabetical.
            e.printStackTrace();
        }
    }
}