package net.lucy.data.world;

import net.minecraft.util.math.BlockPos;

public class ResourceLocation {
    private final String item;
    private final BlockPos position;
    private int amount;
    private long lastSeen;
    public ResourceLocation(String item, BlockPos position, int amount) {
        this.item = item;
        this.position = position;
        this.amount = amount;
        this.lastSeen = System.currentTimeMillis();
    }

    public String getItem() {
        return item;
    }

    public BlockPos getPosition() {
        return position;
    }

    public int getAmount() {
        return amount;
    }

    public void updateAmount(int amount) {
        this.amount = amount;
        this.lastSeen = System.currentTimeMillis();
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public double getEfficiency(BlockPos player) {
        double distance = player.getSquaredDistance(position);
        return amount <= 0 ? Double.MAX_VALUE : distance / amount;
    }
}