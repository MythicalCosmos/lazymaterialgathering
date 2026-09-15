package net.lucy.gui;

import net.lucy.LazyMaterialGatheringClient;
import net.lucy.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    private final Screen parent;

    private static final int TAB_HEIGHT = 24;

    private int selectedTab = 0;

    private static final String[] TAB_NAMES = {
            "Schematic",
            "Recipes",
            "Locations",
            "Import / Export"
    };

    public ConfigScreen(Screen parent) {
        super(Text.literal("Lazy Material Gathering"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {

        this.clearChildren();

        int centerX = this.width / 2;

        /*
         * Tabs
         */

        int tabWidth = 120;

        for (int i = 0; i < TAB_NAMES.length; i++) {

            final int tab = i;

            int x =
                    centerX
                            - (TAB_NAMES.length * tabWidth) / 2
                            + i * tabWidth;

            this.addDrawableChild(
                    ButtonWidget.builder(
                            Text.literal(TAB_NAMES[i]),
                            button -> {

                                selectedTab = tab;

                                rebuild();
                            }
                    ).dimensions(
                            x,
                            30,
                            tabWidth - 2,
                            TAB_HEIGHT
                    ).build()
            );
        }

        /*
         * Current tab
         */

        switch (selectedTab) {

            case 0 ->
                    buildSchematicTab();

            case 1 ->
                    buildRecipesTab();

            case 2 ->
                    buildLocationsTab();

            case 3 ->
                    buildImportExportTab();
        }

        /*
         * Bottom buttons
         */

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Save"),
                        button -> save()
                ).dimensions(
                        centerX - 155,
                        this.height - 35,
                        100,
                        20
                ).build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Done"),
                        button -> {
                            save();
                            close();
                        }
                ).dimensions(
                        centerX - 50,
                        this.height - 35,
                        100,
                        20
                ).build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Cancel"),
                        button -> close()
                ).dimensions(
                        centerX + 55,
                        this.height - 35,
                        100,
                        20
                ).build()
        );
    }

    private void buildSchematicTab() {

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Open Schematic Screen"),
                        button -> setScreen(
                                new SchematicScreen(this)
                        )
                ).dimensions(
                        this.width / 2 - 120,
                        85,
                        240,
                        20
                ).build()
        );
    }

    private void buildRecipesTab() {

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Configure Preferred Recipes"),
                        button -> setScreen(
                                new RecipesScreen(this)
                        )
                ).dimensions(
                        this.width / 2 - 120,
                        85,
                        240,
                        20
                ).build()
        );
    }

    private void buildLocationsTab() {

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Configure File Locations"),
                        button -> setScreen(
                                new LocationsScreen(this)
                        )
                ).dimensions(
                        this.width / 2 - 120,
                        85,
                        240,
                        20
                ).build()
        );
    }

    private void buildImportExportTab() {

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Import / Export Configuration"),
                        button -> setScreen(
                                new ImportExportScreen(this)
                        )
                ).dimensions(
                        this.width / 2 - 120,
                        85,
                        240,
                        20
                ).build()
        );
    }

    private void setScreen(Screen screen) {
        this.client.setScreen(screen);
    }

    private void save() {

        try {
            Config.save(
                    LazyMaterialGatheringClient.getConfigFile()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        this.renderBackground(context);

        /*
         * Main title
         */

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                10,
                0xFFFFFF
        );

        /*
         * Panel
         */

        int left = 25;
        int right = this.width - 25;

        context.fill(
                left,
                58,
                right,
                this.height - 45,
                0xCC101010
        );

        context.fill(
                left + 1,
                59,
                right - 1,
                this.height - 46,
                0xAA202020
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
