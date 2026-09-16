package net.lucy.gui;

import net.lucy.SchemParser;
import net.lucy.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.sandrohc.schematic4j.SchematicLoader;
import net.sandrohc.schematic4j.schematic.Schematic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SchematicBrowserScreen extends Screen {

    private final Screen parent;
    private List<Path> schematicFiles = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private String statusMessage = "";

    public SchematicBrowserScreen(Screen parent) {
        super(Text.literal("Load Schematic"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshFileList();

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Refresh"), button -> refreshFileList())
                        .dimensions(width / 2 - 200, height - 60, 130, 20).build()
        );

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Load"), button -> load())
                        .dimensions(width / 2 - 65, height - 60, 130, 20).build()
        );

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Close"), button -> close())
                        .dimensions(width / 2 + 70, height - 60, 130, 20).build()
        );
    }

    private Path getSchematicsFolder() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("schematics");
    }

    private void refreshFileList() {
        schematicFiles.clear();
        selectedIndex = -1;

        Path folder = getSchematicsFolder();

        try {
            Files.createDirectories(folder);

            try (Stream<Path> stream = Files.list(folder)) {
                stream.filter(p -> p.toString().toLowerCase().endsWith(".litematic"))
                        .sorted()
                        .forEach(schematicFiles::add);
            }

            statusMessage = schematicFiles.isEmpty()
                    ? "No .litematic files found in " + folder
                    : "";
        } catch (IOException e) {
            statusMessage = "Could not read schematics folder: " + e.getMessage();
        }
    }

    private void load() {
        if (selectedIndex < 0 || selectedIndex >= schematicFiles.size()) {
            statusMessage = "Select a schematic first.";
            return;
        }

        Path file = schematicFiles.get(selectedIndex);
        Config.schematicPath = file.toAbsolutePath().toString();
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listTop = 55;
        int rowHeight = 14;
        int listLeft = width / 2 - 195;
        int listWidth = 390;

        if (mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= listTop) {
            int clickedRow = (int) ((mouseY - listTop + scrollOffset) / rowHeight);
            if (clickedRow >= 0 && clickedRow < schematicFiles.size()) {
                selectedIndex = clickedRow;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scrollOffset -= (int) (amount * 14);
        if (scrollOffset < 0) scrollOffset = 0;

        int maxScroll = Math.max(0, schematicFiles.size() * 14 - (height - 130));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelLeft = width / 2 - 200;
        GuiTheme.drawPanel(context, panelLeft, 30, 400, height - 90);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, GuiTheme.TITLE_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal("Folder: " + getSchematicsFolder()), width / 2 - 195, 38, GuiTheme.LABEL_COLOR);

        int listTop = 55;
        int rowHeight = 14;

        for (int i = 0; i < schematicFiles.size(); i++) {
            int drawY = listTop + (i * rowHeight) - scrollOffset;
            if (drawY < listTop - rowHeight || drawY > height - 100) continue;

            String name = schematicFiles.get(i).getFileName().toString();
            int color = (i == selectedIndex) ? 0xFFFF55 : 0xFFFFFF;

            if (i == selectedIndex) {
                context.fill(width / 2 - 195, drawY - 1, width / 2 + 195, drawY + 11, 0x552277FF);
            }

            context.drawTextWithShadow(textRenderer, Text.literal(name), width / 2 - 190, drawY, color);
        }

        if (!statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(statusMessage), width / 2, height - 75, GuiTheme.STATUS_COLOR);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}