package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.data.ObtainMethods;
import net.lucy.data.Recipes;
import net.lucy.model.ObtainMethod;
import net.lucy.model.Recipe;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything known about how to get one item: every recipe that makes it, drawn as a real
 * crafting grid or smelting slot, plus any non-crafting way to get it (mining, mob drops,
 * trading) recorded for it. Opened by clicking a row on the Raw Materials screen.
 */
public class ItemSourcesScreen extends GuiListBase<Recipe, RecipeCardWidget, WidgetListRecipeCards>
{
    private final String itemName;
    private List<Recipe> recipes = new ArrayList<>();
    private List<ObtainMethod> obtainMethods = new ArrayList<>();

    public ItemSourcesScreen(String itemName)
    {
        super(SideButtonBar.WIDTH + 10, 0); // list Y is set per-screen below

        this.itemName = itemName;
        this.title = "How to obtain: " + TableRow.displayName(itemName);
    }

    @Override
    protected WidgetListRecipeCards createListWidget(int listX, int listY)
    {
        this.recipes = Recipes.recipes.getOrDefault(this.itemName, new ArrayList<>());
        this.obtainMethods = ObtainMethods.methods.getOrDefault(this.itemName, new ArrayList<>());

        return new WidgetListRecipeCards(listX, this.computeListY(), this.getBrowserWidth(), this.getBrowserHeight(), this.recipes, this.itemName);
    }

    // Non-craft methods take a few lines at the top, so the list starts lower when there are any.
    // (Named computeListY, not getListY, because GuiListBase already declares a protected
    // getListY() of its own — overriding it with a private method is what caused the error.)
    private int computeListY()
    {
        return 18 + (this.obtainMethods.isEmpty() ? 0 : (this.obtainMethods.size() * 11 + 14));
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - SideButtonBar.WIDTH - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 20 - this.computeListY();
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int textY = 18;

        if (this.obtainMethods.isEmpty() == false)
        {
            this.addLabel(this.getListX() + 2, textY, 200, 12, 0xFFFFAA00, "Other ways to get this:");
            textY += 14;

            for (ObtainMethod method : this.obtainMethods)
            {
                String line = TableRow.prettify(method.category.name()) + ": " + method.description;
                this.addLabel(this.getListX() + 6, textY, this.getStringWidth(line) + 2, 11, 0xFFFFFFFF, line);
                textY += 11;
            }
        }

        if (this.recipes.isEmpty() && this.obtainMethods.isEmpty())
        {
            String message = "No recipe or source recorded for this item \u2014 it's likely a base";
            String message2 = "material you mine or find directly.";
            this.addLabel(this.getListX() + 2, textY + 6, this.getStringWidth(message) + 2, 12, 0xFFFFAA00, message);
            this.addLabel(this.getListX() + 2, textY + 18, this.getStringWidth(message2) + 2, 12, 0xFFFFAA00, message2);
        }

        int y = SideButtonBar.START_Y;

        y = this.addSideButton(y, "Copy Info", this::copyToClipboard);
        this.addSideButton(y, "Back to Raw Materials", () -> GuiBase.openGui(new RawMaterialsScreen()));
    }

    private int addSideButton(int y, String label, Runnable action)
    {
        int width = Math.max(SideButtonBar.BUTTON_WIDTH, this.getStringWidth(label) + 10);
        ButtonGeneric button = new ButtonGeneric(SideButtonBar.X, y, width, 20, label);
        this.addButton(button, (b, mouseButton) -> action.run());

        return y + SideButtonBar.SPACING;
    }

    private void copyToClipboard()
    {
        StringBuilder text = new StringBuilder(TableRow.displayName(this.itemName)).append('\n');

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