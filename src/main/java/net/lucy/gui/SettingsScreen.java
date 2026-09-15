package net.lucy.gui;

import net.lucy.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SettingsScreen extends Screen {

    private final Screen parent;

    private TextFieldWidget outputDirectoryField;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        outputDirectoryField = new TextFieldWidget(textRenderer, centerX - 150, 55, 300, 20, Text.literal("Output Directory"));
        outputDirectoryField.setText(Config.outputDirectory == null ? "" : Config.outputDirectory);
        outputDirectoryField.setMaxLength(500);
        addDrawableChild(outputDirectoryField);


        addDrawableChild(
                ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                        .dimensions(centerX + 5, 155, 145, 20).build()
        );
    }

    private void save() {
        Config.outputDirectory = outputDirectoryField.getText().trim();


        try {
            Config.save(net.lucy.LazyMaterialGatheringClient.getConfigFile());
        } catch (Exception e) {
            e.printStackTrace();
        }

        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelLeft = width / 2 - 200;
        GuiTheme.drawPanel(context, panelLeft, 30, 400, 155);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, GuiTheme.TITLE_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal("Output Directory"), width / 2 - 150, 40, GuiTheme.LABEL_COLOR);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}