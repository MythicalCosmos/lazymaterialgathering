package net.lucy.baritone;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Shared safe placement support for workstations used by the automation.
 *
 * A workstation is only placed when its item is already in the player's
 * inventory and a replaceable spot on a solid surface is available nearby.
 */
public final class WorkstationUtils {

    private WorkstationUtils() {
    }

    public static boolean hasItem(
            MinecraftClient client,
            String itemId
    ) {
        Item item = getItem(itemId);
        if (client == null || client.player == null || item == null) {
            return false;
        }

        PlayerInventory inventory = client.player.getInventory();
        int slots = Math.min(36, inventory.size());

        for (int i = 0; i < slots; i++) {
            if (inventory.getStack(i).isOf(item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Attempts one safe placement step. Returns true if it performed an
     * interaction or inventory-screen step; callers should wait and verify
     * the block in the world before proceeding.
     */
    public static boolean tryPlaceBlock(
            MinecraftClient client,
            String itemId,
            int radius
    ) {
        if (client == null
                || client.player == null
                || client.world == null
                || client.interactionManager == null) {
            return false;
        }

        Item item = getItem(itemId);
        if (item == null) {
            return false;
        }

        PlayerInventory inventory = client.player.getInventory();
        int hotbarSlot = findItem(inventory, item, 0, 9);
        int mainSlot = findItem(inventory, item, 9, Math.min(36, inventory.size()));

        if (client.currentScreen instanceof InventoryScreen) {
            if (hotbarSlot >= 0) {
                client.setScreen(null);
                return true;
            }

            if (mainSlot >= 0) {
                ScreenHandler handler = client.player.currentScreenHandler;
                client.interactionManager.clickSlot(
                        handler.syncId,
                        mainSlot,
                        inventory.selectedSlot,
                        SlotActionType.SWAP,
                        client.player
                );
                client.setScreen(null);
                return true;
            }

            return false;
        }

        // Do not steal interactions from another open container or screen.
        if (client.currentScreen != null) {
            return false;
        }

        if (hotbarSlot < 0) {
            if (mainSlot >= 0) {
                client.setScreen(new InventoryScreen(client.player));
                return true;
            }

            return false;
        }

        inventory.selectedSlot = hotbarSlot;

        BlockPos placePos = findPlacementPosition(client, radius);
        if (placePos == null) {
            return false;
        }

        BlockPos support = placePos.down();
        client.interactionManager.interactBlock(
                client.player,
                Hand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3d(
                                support.getX() + 0.5D,
                                support.getY() + 1.0D,
                                support.getZ() + 0.5D
                        ),
                        Direction.UP,
                        support,
                        false
                )
        );

        return true;
    }

    private static BlockPos findPlacementPosition(
            MinecraftClient client,
            int radius
    ) {
        BlockPos origin = client.player.getBlockPos();
        Box playerBox = client.player.getBoundingBox();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos pos = origin.add(dx, dy, dz);

                    if (!client.world.getBlockState(pos).isAir()) {
                        continue;
                    }

                    Box targetBox = new Box(
                            pos.getX(), pos.getY(), pos.getZ(),
                            pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D
                    );

                    if (playerBox.intersects(targetBox)) {
                        continue;
                    }

                    BlockPos support = pos.down();
                    if (!client.world.getBlockState(support)
                            .isSolidBlock(client.world, support)) {
                        continue;
                    }

                    double distance = client.player.squaredDistanceTo(
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D
                    );

                    if (distance < bestDistance) {
                        best = pos;
                        bestDistance = distance;
                    }
                }
            }
        }

        return best;
    }

    private static int findItem(
            PlayerInventory inventory,
            Item item,
            int start,
            int end
    ) {
        for (int i = start; i < end; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.isOf(item)) {
                return i;
            }
        }

        return -1;
    }

    private static Item getItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        try {
            Identifier id = itemId.contains(":")
                    ? new Identifier(itemId)
                    : new Identifier("minecraft", itemId);

            Item item = Registries.ITEM.get(id);
            return item == null || item == net.minecraft.item.Items.AIR
                    ? null
                    : item;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
