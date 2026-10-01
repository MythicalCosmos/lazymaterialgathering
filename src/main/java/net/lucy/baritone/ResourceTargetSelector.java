package net.lucy.baritone;


import net.lucy.resource.ResourceCache;
import net.lucy.resource.ResourceNode;

import net.minecraft.util.math.BlockPos;



public class ResourceTargetSelector {


    private final ResourceCache cache;



    public ResourceTargetSelector(){

        this.cache =
                ResourceCache.get();

    }




    public ResourceNode findTarget(
            String item,
            long amount
    ){


        return cache
                .get(item)
                .stream()

                .filter(
                        node ->
                                node.getEstimatedAmount() >= amount
                )

                .min(

                        (a,b) ->

                                Double.compare(
                                        calculateCost(a,amount),
                                        calculateCost(b,amount)
                                )

                )

                .orElse(null);

    }






    private double calculateCost(
            ResourceNode node,
            long needed
    ){

        double distance =
                node.getPosition()
                        .getSquaredDistance(
                                BlockPos.ORIGIN
                        );


        return
                distance
                        /
                        Math.max(
                                node.getEstimatedAmount(),
                                needed
                        );

    }


}