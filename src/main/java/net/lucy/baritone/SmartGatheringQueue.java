package net.lucy.baritone;


import net.lucy.resource.ResourceNode;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;



public class SmartGatheringQueue {



    private final Queue<GatherTask> tasks =
            new ConcurrentLinkedQueue<>();


    private final ResourceTargetSelector selector =
            new ResourceTargetSelector();






    public void add(
            String item,
            long amount
    ){

        tasks.add(
                new GatherTask(
                        item,
                        amount
                )
        );

    }






    public void tick(){


        GatherTask task =
                tasks.peek();



        if(task == null)
            return;



        ResourceNode target =
                selector.findTarget(
                        task.item,
                        task.amount
                );



        if(target == null){

            /*
             * No known source.
             *
             * Later:
             * trigger Baritone search.
             */

            return;

        }



        startBaritone(
                target
        );


        tasks.poll();

    }






    private void startBaritone(
            ResourceNode node
    ){

        IBaritone baritone =
                BaritoneAPI
                        .getProvider()
                        .getPrimaryBaritone();



        baritone
                .getCustomGoalProcess()
                .setGoalAndPath(
                        new baritone.api.pathing.goals.GoalBlock(
                                node.getPosition()
                        )
                );


    }







    public static class GatherTask {


        public final String item;


        public final long amount;



        public GatherTask(
                String item,
                long amount
        ){

            this.item=item;
            this.amount=amount;

        }

    }

}