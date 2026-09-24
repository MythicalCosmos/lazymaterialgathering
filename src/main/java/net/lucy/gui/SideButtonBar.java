package net.lucy.gui;

/**
 * Shared layout numbers so every screen's navigation buttons sit in the same place: a
 * column down the left edge, instead of a row across the top or bottom. Screens position
 * their own buttons using these (there's no shared widget, since each screen's base class
 * already owns its own addButton/addLabel calls) and reserve WIDTH of horizontal space for
 * their list or option area.
 */
public class SideButtonBar
{
    public static final int X = 8;
    public static final int START_Y = 28;
    public static final int SPACING = 22;
    public static final int BUTTON_WIDTH = 92;

    // How much horizontal space the column takes up, including margins either side —
    // screens start their list/content this far from the left edge.
    public static final int WIDTH = X + BUTTON_WIDTH + 14;
}