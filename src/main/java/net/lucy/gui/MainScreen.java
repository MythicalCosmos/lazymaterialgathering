package net.lucy.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MainScreen extends Screen {

    private final Screen parent;

    private static final int GUI_WIDTH = 720;
    private static final int GUI_HEIGHT = 420;

    public MainScreen(Screen parent) {
        super(Text.literal("Lazy Material Gathering"));
        this.parent = parent;
    }

    private int guiLeft() {
        return (this.width - GUI_WIDTH) / 2;
    }

    private int guiTop() {
        return (this.height - GUI_HEIGHT) / 2;
    }

    @Override
    protected void init() {

        int left = guiLeft();
        int top = guiTop();

        int tabY = top + 32;

        /*
         * Schematics
         */
        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Schematics"),
                        button -> openSchematics()
                ).dimensions(
                        left,
                        tabY,
                        175,
                        22
                ).build()
        );

        /*
         * Materials
         */
        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Materials"),
                        button -> openMaterials()
                ).dimensions(
                        left + 180,
                        tabY,
                        175,
                        22
                ).build()
        );

        /*
         * Recipes
         */
        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Recipes"),
                        button -> openRecipes()
                ).dimensions(
                        left + 360,
                        tabY,
                        175,
                        22
                ).build()
        );

        /*
         * Settings
         */
        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Settings"),
                        button -> openSettings()
                ).dimensions(
                        left + 540,
                        tabY,
                        175,
                        22
                ).build()
        );

        /*
         * Bottom close button
         */
        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Close"),
                        button -> close()
                ).dimensions(
                        left + 310,
                        top + GUI_HEIGHT - 28,
                        100,
                        20
                ).build()
        );
    }

    private void openSchematics() {
        this.client.setScreen(
                new SchematicBrowserScreen(this)
        );
    }

    private void openMaterials() {
        this.client.setScreen(
                new MaterialsScreen(this)
        );
    }

    private void openRecipes() {
        this.client.setScreen(
                new RecipesScreen(this)
        );
    }

    private void openSettings() {
        this.client.setScreen(
                new SettingsScreen(this)
        );
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        this.renderBackground(context);

        int left = guiLeft();
        int top = guiTop();

        /*
         * Main panel
         */
        context.fill(
                left,
                top,
                left + GUI_WIDTH,
                top + GUI_HEIGHT,
                0xD0101010
        );

        /*
         * Border
         */
        context.drawBorder(
                left,
                top,
                GUI_WIDTH,
                GUI_HEIGHT,
                0xFF707070
        );

        /*
         * Title
         */
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                top + 10,
                0xFFFFFF
        );

        super.render(
                context,
                mouseX,
                mouseY,
                delta
        );
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
