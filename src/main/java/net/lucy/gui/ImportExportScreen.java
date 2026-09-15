package net.lucy.gui;

import net.lucy.LazyMaterialGatheringClient;
import net.lucy.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;

public class ImportExportScreen extends Screen {

    private final Screen parent;

    private String status = "";

    public ImportExportScreen(Screen parent) {
        super(Text.literal("Import / Export"));
        this.parent = parent;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;

        /*
         * Export
         */

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Export Configuration"),
                        button -> exportConfig()
                ).dimensions(
                        centerX - 100,
                        75,
                        200,
                        20
                ).build()
        );

        /*
         * Import
         */

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Import Configuration"),
                        button -> importConfig()
                ).dimensions(
                        centerX - 100,
                        105,
                        200,
                        20
                ).build()
        );

        /*
         * Reset
         */

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Reset Configuration"),
                        button -> reset()
                ).dimensions(
                        centerX - 100,
                        145,
                        200,
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
                        centerX - 100,
                        185,
                        200,
                        20
                ).build()
        );
    }

    private void exportConfig() {

        FileDialog dialog =
                new FileDialog(
                        (Frame) null,
                        "Export Configuration",
                        FileDialog.SAVE
                );

        dialog.setFile(
                "lazymaterialgathering.properties"
        );

        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return;
        }

        File file =
                new File(
                        dialog.getDirectory(),
                        dialog.getFile()
                );

        try {

            Config.save(file.toPath());

            status = "Configuration exported.";

        } catch (Exception e) {

            status = "Export failed.";

            e.printStackTrace();
        }
    }

    private void importConfig() {

        FileDialog dialog =
                new FileDialog(
                        (Frame) null,
                        "Import Configuration",
                        FileDialog.LOAD
                );

        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return;
        }

        File file =
                new File(
                        dialog.getDirectory(),
                        dialog.getFile()
                );

        try {

            Config.load(file.toPath());

            status = "Configuration imported.";

        } catch (Exception e) {

            status = "Import failed.";

            e.printStackTrace();
        }
    }

    private void reset() {

        Config.reset();

        try {

            Config.save(
                    LazyMaterialGatheringClient
                            .getConfigFile()
            );

            status = "Configuration reset.";

        } catch (Exception e) {

            status = "Reset failed.";

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

        if (!status.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal(status),
                    this.width / 2,
                    225,
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
