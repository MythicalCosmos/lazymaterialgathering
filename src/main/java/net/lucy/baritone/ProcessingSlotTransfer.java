package net.lucy.baritone;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

public final class ProcessingSlotTransfer {
    private ProcessingSlotTransfer() {
    }

    public static boolean moveIntoSlot(MinecraftClient client, ScreenHandler handler, String itemId, int destinationSlot) {
        if (client == null || client.player == null || client.interactionManager == null || handler == null || itemId == null || itemId.isBlank()) {
            return false;
        }

        if (destinationSlot < 0 || destinationSlot >= handler.slots.size()) {
            return false;
        }

        Item item = getItem(itemId);
        if (item == null) {
            return false;
        }

        for (int slot = handler.slots.size() - 1; slot >= 0; slot--) {
            ItemStack stack = handler.getSlot(slot).getStack();
            if (stack.isEmpty() || !stack.isOf(item)) {
                continue;
            }

            if (slot == destinationSlot) {
                continue;
            }

            client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(handler.syncId, destinationSlot, 0, SlotActionType.PICKUP, client.player);
            if (!client.player.currentScreenHandler.getCursorStack().isEmpty()) {
                client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
            }
            return true;
        }
        return false;
    }

    public static boolean slotContains(ScreenHandler handler, int slot, String itemId) {
        if (handler == null || slot < 0 || slot >= handler.slots.size() || itemId == null) {
            return false;
        }

        Item item = getItem(itemId);
        if (item == null) {
            return false;
        }

        ItemStack stack = handler.getSlot(slot).getStack();
        return !stack.isEmpty() && stack.isOf(item);
    }

    public static long countInSlot(ScreenHandler handler, int slot, String itemId) {
        if (handler == null || slot < 0 || slot >= handler.slots.size() || itemId == null) {
            return 0L;
        }

        Item item = getItem(itemId);
        if (item == null) {
            return 0L;
        }

        ItemStack stack = handler.getSlot(slot).getStack();
        if (stack.isEmpty() || !stack.isOf(item)) {
            return 0L;
        }
        return stack.getCount();
    }

    private static Item getItem(String itemId) {
        Identifier id;
        try {
            id = itemId.contains(":") ? new Identifier(itemId) : new Identifier("minecraft", itemId);
        } catch (IllegalArgumentException e) {
            return null;
        }

        Item item = Registries.ITEM.get(id);
        if (item == null || item == Registries.ITEM.get(new Identifier("minecraft", "air"))) {
            return null;
        }
        return item;
    }
}