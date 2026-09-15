package net.lucy.gui;

import net.lucy.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

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

        pathField = new TextFieldWidget(
                textRenderer,
                centerX - 150,
                55,
                300,
                20,
                Text.literal("Schematic File")
        );

        pathField.setMaxLength(500);
        pathField.setText(
                Config.schematicPath == null
                        ? ""
                        : Config.schematicPath
        );

        addDrawableChild(pathField);

        // Browse button
        addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Browse"),
                        button -> browse()
                ).dimensions(
                        centerX - 150,
                        85,
                        95,
                        20
                ).build()
        );

        // Load button
        addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Load"),
                        button -> load()
                ).dimensions(
                        centerX - 47,
                        85,
                        95,
                        20
                ).build()
        );

        // Cancel
        addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Cancel"),
                        button -> close()
                ).dimensions(
                        centerX + 56,
                        85,
                        95,
                        20
                ).build()
        );
    }

    private void browse() {
        /*
         * We don't use Swing here.
         *
         * Minecraft's GUI runs on the Minecraft render thread,
         * so a Swing JFileChooser can cause scaling/focus problems.
         *
         * For now, the user can paste/type the absolute path.
         */
        statusMessage = "Enter the full path to a .litematic file.";
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

        try {
            Config.save(Config.getConfigPath());

            statusMessage = "Schematic selected.";
        } catch (Exception e) {
            statusMessage = "Could not save configuration.";
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
        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                textRenderer,
                title,
                width / 2,
                20,
                0xFFFFFF
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Schematic File"),
                width / 2,
                40,
                0xAAAAAA
        );

        if (!statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal(statusMessage),
                    width / 2,
                    125,
                    0xFFFF55
            );
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
