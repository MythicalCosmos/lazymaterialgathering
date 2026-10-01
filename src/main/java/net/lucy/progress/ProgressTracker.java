package net.lucy.progress;


import java.util.Collection;



public class ProgressTracker {


    private static final ProgressTracker INSTANCE =
            new ProgressTracker();



    public static ProgressTracker get(){

        return INSTANCE;

    }



    private SchematicProgress current;




    public void start(
            String schematic
    ){

        current =
                new SchematicProgress(
                        schematic
                );

    }





    public SchematicProgress getCurrent(){

        return current;

    }





    public void setRequirement(
            String item,
            long amount
    ){

        if(current == null)
            return;


        current.addRequirement(
                item,
                amount
        );

    }





    public void collected(
            String item,
            long amount
    ){

        if(current == null)
            return;


        current.addCollected(
                item,
                amount
        );

    }





    public void placed(String item, long amount){
        if(current == null)
            return;

        current.addPlaced(item, amount);

    }

}