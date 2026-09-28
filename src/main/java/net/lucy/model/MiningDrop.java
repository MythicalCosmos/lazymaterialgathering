package net.lucy.model;

public final class MiningDrop {
    private final String normalDrop;
    private final int normalCount;
    private final String silkTouchDrop;
    private final int silkTouchCount;
    private final boolean shearsAlsoWork;

    public MiningDrop(String normalDrop, int normalCount, String silkTouchDrop, int silkTouchCount, boolean shearsAlsoWork) {
        this.normalDrop = normalDrop;
        this.normalCount = normalCount;
        this.silkTouchDrop = silkTouchDrop;
        this.silkTouchCount = silkTouchCount;
        this.shearsAlsoWork = shearsAlsoWork;
    }
    public String getNormalDrop() {return normalDrop;}
    public int getNormalCount() {return normalCount;}
    public String getSilkTouchDrop() {return silkTouchDrop;}
    public int getSilkTouchCount() {return silkTouchCount;}
    public boolean shearsAlsoWork() {return shearsAlsoWork;}
}