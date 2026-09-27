package net.lucy.model;

public class MiningDrop {
    public String normalDrop;
    public int normalCount;

    public String silkTouchDrop;
    public int silkTouchCount;

    // If true, having shears counts the same as having Silk Touch for this block (most
    // leaves, vines, tall grass and ferns): either one gives silkTouchDrop instead of
    // normalDrop.
    public boolean shearsAlsoWork;

    public MiningDrop(String normalDrop, int normalCount, String silkTouchDrop, int silkTouchCount, boolean shearsAlsoWork) {
        this.normalDrop = normalDrop;
        this.normalCount = normalCount;
        this.silkTouchDrop = silkTouchDrop;
        this.silkTouchCount = silkTouchCount;
        this.shearsAlsoWork = shearsAlsoWork;
    }

    // Convenience for the common case: no quantity change, no shears involved
    public MiningDrop(String normalDrop, String silkTouchDrop) {
        this(normalDrop, 1, silkTouchDrop, 1, false);
    }
}