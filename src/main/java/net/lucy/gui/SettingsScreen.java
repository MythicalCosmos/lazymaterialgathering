package net.lucy.gui;

import net.lucy.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SettingsScreen extends Screen {

    private final Screen parent;

    private TextFieldWidget configPathField;
    private TextFieldWidget outputDirectoryField;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {

        int centerX = width / 2;

        /*
         * Config file location
         */
        configPathField = new TextFieldWidget(
                textRenderer,
                centerX - 150,
                55,
                300,
                20,
                Text.literal("Config File")
        );

        configPathField.setText(
                Config.configFilePath == null
                        ? ""
                        : Config.configFilePath
        );

        configPathField.setMaxLength(500);

        addDrawableChild(configPathField);

        /*
         * Output directory
         */
        outputDirectoryField = new TextFieldWidget(
                textRenderer,
                centerX - 150,
                105,
                300,
                20,
                Text.literal("Output Directory")
        );

        outputDirectoryField.setText(
                Config.outputDirectory == null
                        ? ""
                        : Config.outputDirectory
        );

        outputDirectoryField.setMaxLength(500);

        addDrawableChild(outputDirectoryField);

        /*
         * Save
         */
        addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Save"),
                        button -> save()
                ).dimensions(
                        centerX - 150,
                        145,
                        145,
                        20
                ).build()
        );

        /*
         * Cancel
         */
        addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Cancel"),
                        button -> close()
                ).dimensions(
                        centerX + 5,
                        145,
                        145,
                        20
                ).build()
        );
    }

    private void save() {

        Config.configFilePath =
                configPathField.getText().trim();

        Config.outputDirectory =
                outputDirectoryField.getText().trim();

        try {
            Config.save(Config.getConfigPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        close();
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                textRenderer,
                title,
                width / 2,
                15,
                0xFFFFFF
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Config File Location"),
                width / 2 - 150,
                35,
                0xAAAAAA
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Output Directory"),
                width / 2 - 150,
                85,
                0xAAAAAA
        );
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
