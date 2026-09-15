package net.lucy.gui;

import net.lucy.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;

public class SchematicScreen extends Screen {

    private final Screen parent;

    private TextFieldWidget schematicPath;

    private String status = "";

    public SchematicScreen(Screen parent) {
        super(Text.literal("Load Schematic"));
        this.parent = parent;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;

        schematicPath = new TextFieldWidget(
                this.textRenderer,
                centerX - 180,
                90,
                360,
                20,
                Text.literal("Schematic")
        );

        schematicPath.setMaxLength(1000);

        schematicPath.setText(
                Config.schematicPath == null
                        ? ""
                        : Config.schematicPath
        );

        this.addDrawableChild(schematicPath);

        /*
         * Browse
         */

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Browse..."),
                        button -> browse()
                ).dimensions(
                        centerX - 180,
                        120,
                        85,
                        20
                ).build()
        );

        /*
         * Load
         */

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Load"),
                        button -> load()
                ).dimensions(
                        centerX - 45,
                        120,
                        90,
                        20
                ).build()
        );

        /*
         * Back
         */

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Back"),
                        button -> close()
                ).dimensions(
                        centerX + 60,
                        120,
                        85,
                        20
                ).build()
        );
    }

    private void browse() {

        /*
         * FileDialog is used instead of JFileChooser.
         *
         * This gives us a native Windows file picker without
         * putting a Swing window inside Minecraft.
         */

        FileDialog dialog =
                new FileDialog(
                        (Frame) null,
                        "Select Litematic",
                        FileDialog.LOAD
                );

        dialog.setFile("*.litematic");
        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return;
        }

        File file = new File(
                dialog.getDirectory(),
                dialog.getFile()
        );

        schematicPath.setText(
                file.getAbsolutePath()
        );
    }

    private void load() {

        String path =
                schematicPath.getText().trim();

        if (path.isEmpty()) {
            status = "No schematic selected.";
            return;
        }

        File file = new File(path);

        if (!file.exists()) {
            status = "File does not exist.";
            return;
        }

        if (!file.isFile()) {
            status = "Selected path is not a file.";
            return;
        }

        Config.schematicPath = file.getAbsolutePath();

        status = "Schematic selected.";
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        this.renderBackground(context);

        int left = 30;
        int right = this.width - 30;

        context.fill(
                left,
                45,
                right,
                this.height - 35,
                0xCC101010
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                15,
                0xFFFFFF
        );

        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Schematic file"),
                this.width / 2 - 180,
                72,
                0xCCCCCC
        );

        if (!status.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal(status),
                    this.width / 2,
                    155,
                    0xFFFF55
            );
        }

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
