package net.lucy.data;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Small client-side helpers for quantity-aware gathering.
 */
public final class InventoryUtils {

    private InventoryUtils() {
    }

    /**
     * Counts every stack of the requested item in the player's inventory.
     *
     * Supports both:
     *
     * minecraft:stone
     *
     * and:
     *
     * stone
     */
    public static long count(String itemId) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null) {
            return 0L;
        }

        Identifier id = parse(itemId);

        if (id == null) {
            return 0L;
        }

        var item =
                Registries.ITEM.get(id);

        PlayerInventory inventory =
                client.player.getInventory();

        long total = 0L;

        for (int i = 0; i < inventory.size(); i++) {

            ItemStack stack =
                    inventory.getStack(i);

            if (
                    !stack.isEmpty()
                            && stack.isOf(item)
            ) {
                total += stack.getCount();
            }
        }

        return total;
    }

    /**
     * Converts an item name into an Identifier.
     */
    public static Identifier parse(String name) {

        try {

            return name.contains(":")
                    ? new Identifier(name)
                    : new Identifier(
                    "minecraft",
                    name
            );

        } catch (IllegalArgumentException e) {

            return null;
        }
    }
}