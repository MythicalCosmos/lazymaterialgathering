package net.lucy.gui;

import net.lucy.SchemParser;
import net.lucy.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.sandrohc.schematic4j.SchematicLoader;
import net.sandrohc.schematic4j.schematic.Schematic;

import java.nio.file.Files;
import java.nio.file.Path;

public class SchematicBrowserScreen extends Screen {

    private final Screen parent;

    private TextFieldWidget pathField;
    private String statusMessage = "";

    public SchematicBrowserScreen(Screen parent) {
        super(Text.literal("Load Schematic"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        pathField = new TextFieldWidget(textRenderer, centerX - 150, 55, 300, 20, Text.literal("Schematic File"));
        pathField.setMaxLength(500);
        pathField.setText(Config.schematicPath == null ? "" : Config.schematicPath);
        addDrawableChild(pathField);

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Browse"), button -> browse())
                        .dimensions(centerX - 150, 85, 95, 20).build()
        );

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Load"), button -> load())
                        .dimensions(centerX - 47, 85, 95, 20).build()
        );

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                        .dimensions(centerX + 56, 85, 95, 20).build()
        );
    }

    private void browse() {
        java.awt.FileDialog dialog = new java.awt.FileDialog((java.awt.Frame) null, "Select Litematic", java.awt.FileDialog.LOAD);
        dialog.setFile("*.litematic");
        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return;
        }

        java.io.File file = new java.io.File(dialog.getDirectory(), dialog.getFile());
        pathField.setText(file.getAbsolutePath());
    }

    private void load() {
        String path = pathField.getText().trim();

        if (path.isEmpty()) {
            statusMessage = "No schematic selected.";
            return;
        }

        Path file = Path.of(path);

        if (!Files.exists(file)) {
            statusMessage = "File does not exist.";
            return;
        }

        if (!path.toLowerCase().endsWith(".litematic")) {
            statusMessage = "File must be a .litematic file.";
            return;
        }

        Config.schematicPath = path;
        statusMessage = "Parsing...";

        new Thread(() -> {
            try {
                Schematic schematic = SchematicLoader.load(file);
                SchemParser.parse(schematic);
                Config.save(Config.getConfigPath());

                MinecraftClient.getInstance().execute(() ->
                        statusMessage = "Parsed successfully — " + SchemParser.blockCounts.size() + " block types.");
            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() ->
                        statusMessage = "Parse failed: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelLeft = width / 2 - 200;
        GuiTheme.drawPanel(context, panelLeft, 30, 400, 90);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, GuiTheme.TITLE_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal("Schematic File"), width / 2 - 150, 40, GuiTheme.LABEL_COLOR);

        if (!statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(statusMessage), width / 2, 135, GuiTheme.STATUS_COLOR);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}