package net.lucy.gui;

import net.lucy.calc.RecipeExporter;
import net.lucy.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipesScreen extends Screen {

    private final Screen parent;

    private TextFieldWidget recipeNameField;
    private TextFieldWidget recipeValueField;

    private int scrollOffset = 0;
    private String statusMessage = "";

    public RecipesScreen(Screen parent) {
        super(Text.literal("Preferred Recipes"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        recipeNameField = new TextFieldWidget(textRenderer, centerX - 150, 45, 145, 20, Text.literal("Recipe ID"));
        recipeValueField = new TextFieldWidget(textRenderer, centerX + 5, 45, 145, 20, Text.literal("Preferred Recipe"));
        recipeNameField.setMaxLength(200);
        recipeValueField.setMaxLength(200);
        addDrawableChild(recipeNameField);
        addDrawableChild(recipeValueField);

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Add / Update"), button -> addRecipe())
                        .dimensions(centerX - 150, 70, 145, 20).build()
        );

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Remove"), button -> removeRecipe())
                        .dimensions(centerX + 5, 70, 145, 20).build()
        );

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Export All Recipes"), button -> exportRecipes())
                        .dimensions(centerX - 150, 95, 300, 20).build()
        );

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Save"), button -> save())
                        .dimensions(centerX - 75, height - 30, 150, 20).build()
        );
    }

    private void addRecipe() {
        String recipe = recipeNameField.getText().trim();
        String preferred = recipeValueField.getText().trim();

        if (recipe.isEmpty() || preferred.isEmpty()) return;

        Config.recipePreferences.put(recipe, preferred);
        recipeNameField.setText("");
        recipeValueField.setText("");
    }

    private void removeRecipe() {
        String recipe = recipeNameField.getText().trim();
        if (!recipe.isEmpty()) {
            Config.recipePreferences.remove(recipe);
        }
    }

    private void exportRecipes() {
        try {
            RecipeExporter.exportAll(Path.of("all_recipes.txt"));
            statusMessage = "Exported all recipes to all_recipes.txt";
        } catch (Exception e) {
            statusMessage = "Export failed: " + e.getMessage();
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            Config.save(Config.getConfigPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelLeft = width / 2 - 200;
        GuiTheme.drawPanel(context, panelLeft, 30, 400, height - 70);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, GuiTheme.TITLE_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal("Recipe ID"), width / 2 - 150, 32, GuiTheme.LABEL_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal("Preferred Recipe"), width / 2 + 5, 32, GuiTheme.LABEL_COLOR);

        if (!statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(statusMessage), width / 2, 122, GuiTheme.STATUS_COLOR);
        }

        int y = 145;
        List<Map.Entry<String, String>> recipes = new ArrayList<>(Config.recipePreferences.entrySet());

        for (int i = 0; i < recipes.size(); i++) {
            int drawY = y + i * 18 - scrollOffset;
            if (drawY < 135 || drawY > height - 45) continue;

            Map.Entry<String, String> entry = recipes.get(i);
            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(entry.getKey() + "  ->  " + entry.getValue()),
                    width / 2 - 150, drawY, 0xFFFFFF
            );
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scrollOffset -= (int) (amount * 18);
        if (scrollOffset < 0) scrollOffset = 0;

        int maxScroll = Math.max(0, Config.recipePreferences.size() * 18 - (height - 190));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        return true;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}