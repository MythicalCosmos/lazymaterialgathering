package net.lucy.gui;

import net.lucy.config.Config;
import net.lucy.data.Recipes;
import net.lucy.model.Recipe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeBrowserScreen extends Screen {

    private final Screen parent;
    private int scrollOffset = 0;

    private static final int ROW_HEIGHT = 22;

    public RecipeBrowserScreen(Screen parent) {
        super(Text.literal("Recipe Browser"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = 40;
        int panelLeft = width / 2 - 200;

        List<Map.Entry<String, List<Recipe>>> multiRecipeItems = new ArrayList<>();
        for (Map.Entry<String, List<Recipe>> entry : Recipes.recipes.entrySet()) {
            if (entry.getValue().size() > 1) {
                multiRecipeItems.add(entry);
            }
        }

        for (Map.Entry<String, List<Recipe>> entry : multiRecipeItems) {
            String itemId = entry.getKey();
            List<Recipe> options = entry.getValue();

            String[] ids = options.stream().map(r -> r.id).toArray(String[]::new);
            String current = Config.recipePreferences.getOrDefault(itemId, ids[0]);

            CyclingButtonWidget<String> cycler = CyclingButtonWidget
                    .<String>builder(Text::literal)
                    .values(ids)
                    .initially(current)
                    .build(panelLeft + 220, y, 160, 18, Text.literal(""),
                            (button, value) -> Config.recipePreferences.put(itemId, value));

            addDrawableChild(cycler);
            y += ROW_HEIGHT;
        }

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Save & Close"), button -> saveAndClose())
                        .dimensions(width / 2 - 75, height - 30, 150, 20).build()
        );
    }

    private void saveAndClose() {
        try {
            Config.save(Config.getConfigPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        close();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scrollOffset -= (int) (amount * ROW_HEIGHT);
        if (scrollOffset < 0) scrollOffset = 0;
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelLeft = width / 2 - 200;
        GuiTheme.drawPanel(context, panelLeft, 30, 400, height - 70);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, GuiTheme.TITLE_COLOR);

        int y = 40;
        for (Map.Entry<String, List<Recipe>> entry : Recipes.recipes.entrySet()) {
            if (entry.getValue().size() <= 1) continue;

            Identifier id = Identifier.tryParse(entry.getKey());
            Item item = id != null ? Registries.ITEM.get(id) : null;

            if (item != null) {
                context.drawItem(new ItemStack(item), panelLeft + 10, y);
            }

            String displayName = item != null
                    ? item.getName().getString()
                    : entry.getKey();

            context.drawTextWithShadow(textRenderer, Text.literal(displayName), panelLeft + 35, y + 5, 0xFFFFFF);

            y += ROW_HEIGHT;
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}