package net.lucy.resource;

import net.minecraft.util.math.BlockPos;

public class ResourceNode {

    private final String item;

    private final BlockPos position;

    private int estimatedAmount;

    private long lastSeen;

    private boolean exposed;

    private double miningCost;


    public ResourceNode(
            String item,
            BlockPos position,
            int estimatedAmount,
            boolean exposed
    ) {
        this.item = item;
        this.position = position;
        this.estimatedAmount = estimatedAmount;
        this.exposed = exposed;
        this.lastSeen = System.currentTimeMillis();

        calculateCost();
    }


    private void calculateCost() {

        double distance =
                position.getX() *
                        position.getX()
                        +
                        position.getY() *
                                position.getY()
                        +
                        position.getZ() *
                                position.getZ();


        /*
         * Base cost.
         *
         * Later this will include:
         *
         * - Baritone path distance
         * - tool requirement
         * - block hardness
         * - danger
         */

        this.miningCost =
                Math.sqrt(distance)
                        /
                        Math.max(estimatedAmount,1);
    }


    public void refresh(
            int newAmount,
            boolean exposed
    ){

        this.estimatedAmount = newAmount;
        this.exposed = exposed;
        this.lastSeen = System.currentTimeMillis();

        calculateCost();
    }


    public String getItem(){

        return item;
    }


    public BlockPos getPosition(){

        return position;
    }


    public int getEstimatedAmount(){

        return estimatedAmount;
    }


    public long getLastSeen(){

        return lastSeen;
    }


    public boolean isExposed(){

        return exposed;
    }


    public double getMiningCost(){

        return miningCost;
    }


    public boolean isExpired(){

        /*
         * Forget nodes older than 30 minutes.
         */

        return System.currentTimeMillis()
                -
                lastSeen
                >
                1000L * 60L * 30L;
    }
}