package net.lucy.planner;

public class MaterialCost {
    private final String item;
    private long required;
    private long obtained;
    public MaterialCost(String item, long amount){
        this.item = item;
        this.required = amount;
        this.obtained = 0;
    }

    public void addObtained(long amount){
        obtained += amount;
        if(obtained > required) obtained = required;
    }

    public long getRemaining(){
        return Math.max(0, required - obtained);
    }

    public double getProgress(){
        if(required == 0)
            return 1;

        return (double)obtained / (double)required;
    }

    public String getItem(){
        return item;
    }

    public long getRequired(){
        return required;
    }

    public long getObtained(){
        return obtained;
    }
    public void increase(long amount){
        required += amount;
    }
}