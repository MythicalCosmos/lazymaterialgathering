package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.calc.RecipeHeuristics;
import net.lucy.config.Configs;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;

import java.util.*;

/**
 * Every craftable item, so you can pick which recipes Baritone is allowed to use for it
 * and which one it should prefer. Items are ordered the same way the creative inventory's
 * tabs are (building blocks, then redstone, then tools, and so on).
 *
 * Each row starts collapsed, showing only the item and how many recipes it has. Click a
 * row to expand it into a grid of recipe icons: left click an icon to turn that recipe on
 * or off, right click to make it the preferred one. At least one recipe per item always
 * stays on. Use the search box to jump to an item by name.
 */
public class PreferredRecipeScreen extends GuiListBase<RecipeItemEntry, WidgetRecipeItemEntry, WidgetListRecipeItems> {
    private List<RecipeItemEntry> entries = new ArrayList<>();
    public PreferredRecipeScreen() {
        super(12, 46);
        this.title = "Preferred Recipes";
        Recipes.ensureLoaded();
    }

    @Override
    protected WidgetListRecipeItems createListWidget(int listX, int listY) {
        this.entries = buildEntries();
        return new WidgetListRecipeItems(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this.entries, this);
    }

    private static List<RecipeItemEntry> buildEntries() {
        Comparator<Map.Entry<String, List<Recipe>>> byCreativeTab = Comparator.comparingInt((Map.Entry<String, List<Recipe>> e) -> CreativeTabOrder.getIndex(e.getKey())).thenComparing(Map.Entry::getKey);
        Comparator<Map.Entry<String, List<Recipe>>> byCloseness = (e1, e2) -> {
            String k1 = e1.getKey();
            String k2 = e2.getKey();
            String base1 = getBaseBlockName(k1);
            String base2 = getBaseBlockName(k2);
            int baseCompare = base1.compareTo(base2);
            if (baseCompare != 0) {
                return baseCompare;
            }
            return k1.compareTo(k2);
        };
        List<Map.Entry<String, List<Recipe>>> sorted = new ArrayList<>(Recipes.recipes.entrySet());
        sorted.sort(byCreativeTab.thenComparing(byCloseness));
        List<RecipeItemEntry> entries = new ArrayList<>();
        for (Map.Entry<String, List<Recipe>> entry : sorted) {
            if (entry.getValue() != null && entry.getValue().size() >= 2) {
                entries.add(new RecipeItemEntry(entry.getKey(), entry.getValue()));
            }

        }
        return entries;
    }

    private static String getBaseBlockName(String name) {
        if (name == null) return "";

        // 1. Lowercase and remove any namespace prefixes like "minecraft:"
        String clean = name.toLowerCase();
        if (clean.contains(":")) {
            clean = clean.substring(clean.indexOf(":") + 1);
        }

        // Arrays of modifiers to strip away
        String[] prefixes = {"chiseled_", "cobbled_", "polished_", "smooth_", "cracked_", "mossy_", "cut_", "stripped_", "red_", "orange_", "yellow_", "lime_", "green_", "cyan_", "light_blue_", "blue_", "purple_", "magenta_", "pink_", "brown_", "black_", "light_gray_", "gray_", "white_"};
        String[] suffixes = {"_bricks", "_brick", "_tiles", "_tile", "_stairs", "_slab", "_wall", "_pillar", "_log", "_wood", "_planks"};

        // 2. Loop continuously until no more prefixes or suffixes can be stripped
        while (true) {
            String original = clean;

            // Strip matching prefixes
            for (String prefix : prefixes) {
                if (clean.startsWith(prefix)) {
                    clean = clean.substring(prefix.length());
                }
            }

            // Strip matching suffixes
            for (String suffix : suffixes) {
                if (clean.endsWith(suffix)) {
                    clean = clean.substring(0, clean.length() - suffix.length());
                }
            }

            // If the string didn't change in this loop cycle, we've found the true base material
            if (clean.equals(original)) {
                break;
            }
        }

        return clean;
    }


    @Override
    protected int getBrowserWidth() {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight() {
        return this.height - 80;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.entries.isEmpty()) {
            String message = "No recipes yet. Join a world once so they can be read (they're then";
            String message2 = "remembered for next time, even offline).";
            this.addLabel(this.getListX() + 4, 80, this.getStringWidth(message) + 2, 12, 0xFFFFAA00, message);
            this.addLabel(this.getListX() + 4, 92, this.getStringWidth(message2) + 2, 12, 0xFFFFAA00, message2);
        }
        else if (Recipes.isFromCache()) {
            String message = "Showing recipes saved from your last time in a world.";
            this.addLabel(this.getListX() + 4, this.height - 46, this.getStringWidth(message) + 2, 12, 0xFF55FFFF, message);
        }

        int x = 12;
        int y = this.height - 26;
        if (Configs.Generic.DEV_MODE_ENABLED.getBooleanValue()) {
            x += this.addNavButton(x, y, "Auto-Pick (Simplest)", this::runAutoPick);
        }

        x += 12;
        this.addNavButton(x, y, "Raw Materials", () -> GuiBase.openGui(new RawMaterialsScreen()));
        String mainMenuLabel = "Main Menu";
        int mainMenuWidth = this.getStringWidth(mainMenuLabel) + 20;
        ButtonGeneric mainMenuButton = new ButtonGeneric(this.width - mainMenuWidth - 10, y, mainMenuWidth, 20, mainMenuLabel);
        this.addButton(mainMenuButton, (button, mouseButton) -> GuiBase.openGui(new MainScreen()));
    }

    private int addNavButton(int x, int y, String label, Runnable action) {
        int width = this.getStringWidth(label) + 10;
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());
        return width + 4;
    }

    // Dev-only: bulk-picks a recipe for every item with more than one option, using a
    // "fewest ingredients" heuristic (see RecipeHeuristics) rather than real popularity
    // data, which the game doesn't record.
    private void runAutoPick() {
        int changed = RecipeHeuristics.applyToAll();
        this.entries = buildEntries();
        Objects.requireNonNull(this.getListWidget()).refreshEntries();
        this.addMessage(MessageType.SUCCESS, "Updated the preferred recipe for %s item(s)", changed);
    }
}