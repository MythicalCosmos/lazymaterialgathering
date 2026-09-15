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

public class LocationsScreen extends Screen {

    private final Screen parent;

    private TextFieldWidget outputDirectory;

    public LocationsScreen(Screen parent) {
        super(Text.literal("File Locations"));
        this.parent = parent;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;

        outputDirectory =
                new TextFieldWidget(
                        this.textRenderer,
                        centerX - 180,
                        90,
                        360,
                        20,
                        Text.literal("Output Directory")
                );

        outputDirectory.setMaxLength(1000);

        outputDirectory.setText(
                Config.outputDirectory == null
                        ? ""
                        : Config.outputDirectory
        );

        this.addDrawableChild(outputDirectory);

        /*
         * Browse directory
         */

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Browse..."),
                        button -> browseDirectory()
                ).dimensions(
                        centerX - 180,
                        120,
                        100,
                        20
                ).build()
        );

        /*
         * Save
         */

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Save"),
                        button -> save()
                ).dimensions(
                        centerX - 50,
                        120,
                        100,
                        20
                ).build()
        );

        /*
         * Back
         */

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Back"),
                        button -> {
                            save();
                            close();
                        }
                ).dimensions(
                        centerX + 80,
                        120,
                        100,
                        20
                ).build()
        );
    }

    private void browseDirectory() {

        FileDialog dialog =
                new FileDialog(
                        (Frame) null,
                        "Select Output Directory",
                        FileDialog.LOAD
                );

        dialog.setVisible(true);

        if (dialog.getDirectory() != null) {

            outputDirectory.setText(
                    dialog.getDirectory()
            );
        }
    }

    private void save() {

        Config.outputDirectory =
                outputDirectory.getText().trim();
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        this.renderBackground(context);

        context.fill(
                25,
                40,
                this.width - 25,
                this.height - 40,
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
                Text.literal("Output directory"),
                this.width / 2 - 180,
                72,
                0xCCCCCC
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
