package net.lucy.model;

public class MiningDrop {
    public String normalDrop;
    public String silkTouchDrop;

    public MiningDrop(String normalDrop, String silkTouchDrop) {
        this.normalDrop = normalDrop;
        this.silkTouchDrop = silkTouchDrop;
    }

    public MiningDrop(String normalDrop) {
        this(normalDrop, normalDrop);
    }
}