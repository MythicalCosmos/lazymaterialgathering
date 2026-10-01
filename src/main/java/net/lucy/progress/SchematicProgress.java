package net.lucy.progress;

import java.util.HashMap;
import java.util.Map;

public class SchematicProgress {
    private final String schematicName;
    private final Map<String,Long> required = new HashMap<>();
    private final Map<String,Long> collected = new HashMap<>();
    private final Map<String,Long> placed = new HashMap<>();
    public SchematicProgress(String schematicName){
        this.schematicName = schematicName;
    }

    public void addRequirement(String item, long amount){
        required.put(item, amount);
    }

    public void addCollected(String item, long amount){
        collected.merge(item, amount, Long::sum);
    }

    public void addPlaced(String item, long amount){
        placed.merge(item, amount, Long::sum);
    }

    public long getRemaining(String item){
        long need = required.getOrDefault(item, 0L);
        long have = collected.getOrDefault(item, 0L);
        return Math.max(0, need-have);
    }

    public double getProgress(){
        long total = 0;
        long finished = 0;
        for(String item : required.keySet()){
            long amount = required.get(item);
            total += amount;
            finished += Math.min(amount, placed.getOrDefault(item, 0L));
        }

        if(total == 0)
            return 0;
        return (double)finished / (double)total;
    }

    public String getName(){
        return schematicName;
    }

    public Map<String,Long> getRequired(){
        return required;
    }
}