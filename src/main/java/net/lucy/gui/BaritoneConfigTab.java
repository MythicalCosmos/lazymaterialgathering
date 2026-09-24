package net.lucy.gui;

/**
 * The tabs on the Baritone settings screen. Grouped the same way Baritone's
 * own settings.txt / wiki groups them, so the categories should feel familiar.
 */
public enum BaritoneConfigTab
{
    MOVEMENT("Movement"),
    MINING("Mining"),
    PATHING("Pathing Cost"),
    RENDERING("Rendering");

    private final String displayName;

    BaritoneConfigTab(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }
}