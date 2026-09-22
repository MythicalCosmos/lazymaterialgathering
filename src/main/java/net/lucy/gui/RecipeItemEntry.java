package net.lucy.gui;

import net.lucy.model.Recipe;

import java.util.List;

/**
 * One item on the Preferred Recipes screen, plus whether its recipe row is expanded.
 * This is the same object every time the list is redrawn (only the on-screen widget is
 * recreated), so "expanded" survives scrolling.
 */
public class RecipeItemEntry
{
    public final String itemName;
    public final List<Recipe> options;
    public boolean expanded;

    public RecipeItemEntry(String itemName, List<Recipe> options)
    {
        this.itemName = itemName;
        this.options = options;
    }
}