package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.calc.ItemClassifier;
import net.lucy.data.ObtainMethods;
import net.lucy.data.Recipes;
import net.lucy.model.ObtainMethod;
import net.lucy.model.Recipe;
import net.lucy.model.Sources;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything known about how to get one item: every recipe that makes it, drawn as a real
 * crafting grid or smelting slot; any curated source recorded for it in ItemSources
 * (mined with/without Silk Touch, farmed, and so on); and any older-style ObtainMethod
 * entries. Opened by clicking a row on the Raw Materials screen.
 */
public class ItemSourcesScreen extends GuiListBase<Recipe, RecipeCardWidget, WidgetListRecipeCards>
{
    private final String itemName;
    private List<Recipe> recipes = new ArrayList<>();
    private List<ObtainMethod> obtainMethods = new ArrayList<>();
    private Set<Sources> knownSources = new HashSet<>();

    public ItemSourcesScreen(String itemName)
    {
        super(12, 0); // list Y is set per-screen below, once we know how much header space is needed

        this.itemName = itemName;
        this.title = "How to obtain: " + TableRow.displayName(itemName);
    }

    @Override
    protected WidgetListRecipeCards createListWidget(int listX, int listY)
    {
        this.recipes = Recipes.recipes.getOrDefault(this.itemName, new ArrayList<>());
        this.obtainMethods = ObtainMethods.methods.getOrDefault(this.itemName, new ArrayList<>());
        this.knownSources = ItemClassifier.getAllSources(this.itemName);

        return new WidgetListRecipeCards(listX, this.computeListY(), this.getBrowserWidth(), this.getBrowserHeight(), this.recipes, this.itemName);
    }

    // Non-craft methods take a few lines at the top, so the list starts lower when there are any.
    // (Named computeListY, not getListY, because GuiListBase already declares a protected
    // getListY() of its own — overriding it with a private method is what caused the error.)
    private int computeListY()
    {
        int lineCount = this.knownSources.size() + this.obtainMethods.size();
        return 44 + (lineCount == 0 ? 0 : (lineCount * 11 + 14));
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 40 - this.computeListY();
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int textY = 44;

        if (this.knownSources.isEmpty() == false || this.obtainMethods.isEmpty() == false)
        {
            this.addLabel(this.getListX() + 2, textY, 200, 12, 0xFFFFAA00, "Other ways to get this:");
            textY += 14;

            for (Sources source : this.knownSources)
            {
                String line = describeSource(source);
                this.addLabel(this.getListX() + 6, textY, this.getStringWidth(line) + 2, 11, 0xFFFFFFFF, line);
                textY += 11;
            }

            for (ObtainMethod method : this.obtainMethods)
            {
                String line = TableRow.prettify(method.category.name()) + ": " + method.description;
                this.addLabel(this.getListX() + 6, textY, this.getStringWidth(line) + 2, 11, 0xFFFFFFFF, line);
                textY += 11;
            }
        }

        if (this.recipes.isEmpty() && this.obtainMethods.isEmpty() && this.knownSources.isEmpty())
        {
            String message = "No recipe or source recorded for this item \u2014 it's likely a base";
            String message2 = "material you mine or find directly.";
            this.addLabel(this.getListX() + 2, textY + 6, this.getStringWidth(message) + 2, 12, 0xFFFFAA00, message);
            this.addLabel(this.getListX() + 2, textY + 18, this.getStringWidth(message2) + 2, 12, 0xFFFFAA00, message2);
        }

        int x = 12;
        int y = this.height - 26;

        x += this.addNavButton(x, y, "Copy Info", this::copyToClipboard);

        String backLabel = "Back to Raw Materials";
        int backWidth = this.getStringWidth(backLabel) + 20;
        ButtonGeneric backButton = new ButtonGeneric(this.width - backWidth - 10, y, backWidth, 20, backLabel);
        this.addButton(backButton, (button, mouseButton) -> GuiBase.openGui(new RawMaterialsScreen()));
    }

    // Simplifies describing sources using unified Source types
    private static String describeSource(Sources source)
    {
        Sources.Type type = source.getType();

        // Handle common action text variants beautifully
        if (type == Sources.Type.SMELTED || type == Sources.Type.BREWED || type == Sources.Type.CRAFTED)
        {
            return "Obtained via " + TableRow.prettify(type.name());
        }

        String line = TableRow.prettify(type.name());

        if (source.hasModifier())
        {
            line += " (needs " + TableRow.prettify(source.getModifier()) + ")";
        }

        return line;
    }

    private int addNavButton(int x, int y, String label, Runnable action)
    {
        int width = this.getStringWidth(label) + 10;
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());

        return width + 4;
    }

    private void copyToClipboard()
    {
        StringBuilder text = new StringBuilder(TableRow.displayName(this.itemName)).append('\n');

        for (Sources source : this.knownSources)
        {
            text.append(describeSource(source)).append('\n');
        }

        for (ObtainMethod method : this.obtainMethods)
        {
            text.append(TableRow.prettify(method.category.name())).append(": ").append(method.description).append('\n');
        }

        for (Recipe recipe : this.recipes)
        {
            text.append(recipe.type).append(' ').append(recipe.id).append(": ");
            recipe.ingredients.forEach((name, count) -> text.append(count).append("x ").append(TableRow.displayName(name)).append(", "));
            text.append("-> ").append(recipe.outputCount).append('\n');
        }

        MinecraftClient.getInstance().keyboard.setClipboard(text.toString());
        this.addMessage(MessageType.SUCCESS, "Copied to clipboard");
    }
}
