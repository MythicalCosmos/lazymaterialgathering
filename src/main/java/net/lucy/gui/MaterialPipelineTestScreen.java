package net.lucy.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.baritone.MaterialPipelineQueue;
import net.lucy.calc.CraftingPlanner;
import net.lucy.calc.MiningSourceResolver;
import net.lucy.calc.ProcessingPlanner;
import net.lucy.config.Configs;
import net.lucy.data.InventoryUtils;
import net.lucy.model.CraftingPlan;
import net.lucy.model.ProcessingPlan;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class MaterialPipelineTestScreen extends GuiBase {

    private static final int ROWS = 6;
    private static final int MAX_AMOUNT = 256;

    private final TextFieldWidget[] itemFields =
            new TextFieldWidget[ROWS];

    private final TextFieldWidget[] amountFields =
            new TextFieldWidget[ROWS];

    private Map<String, Long> previewTargets =
            new LinkedHashMap<>();

    private String previewStatus = "";
    private int previewColor = 0xFFFFFF;

    private String status = "";
    private int statusColor = 0xFFFFFF;

    private String pipelineSummary = "";

    @Override
    public void initGui() {
        super.initGui();

        int centerX = this.width / 2;

        int left = centerX - 230;

        int itemWidth = 260;

        int amountLeft =
                left + 275;

        int amountWidth = 90;

        int top = 65;

        for (int i = 0; i < ROWS; i++) {

            int y =
                    top + i * 32;

            TextFieldWidget itemField =
                    new TextFieldWidget(
                            MinecraftClient.getInstance().textRenderer,
                            left,
                            y,
                            itemWidth,
                            20,
                            Text.literal("Item")
                    );

            itemField.setMaxLength(128);

            TextFieldWidget amountField =
                    new TextFieldWidget(
                            MinecraftClient.getInstance().textRenderer,
                            amountLeft,
                            y,
                            amountWidth,
                            20,
                            Text.literal("Amount")
                    );

            amountField.setMaxLength(8);

            if (i == 0) {
                itemField.setText(
                        "minecraft:iron_ingot"
                );

                amountField.setText("3");
            }

            itemFields[i] =
                    this.addDrawableChild(
                            itemField
                    );

            amountFields[i] =
                    this.addDrawableChild(
                            amountField
                    );
        }

        int buttonY =
                top + ROWS * 32 + 12;

        ButtonGeneric previewButton =
                new ButtonGeneric(
                        left,
                        buttonY,
                        120,
                        20,
                        "Preview"
                );

        this.addButton(
                previewButton,
                (button, mouseButton) ->
                        preview()
        );

        ButtonGeneric startButton =
                new ButtonGeneric(
                        left + 130,
                        buttonY,
                        120,
                        20,
                        "Start Test"
                );

        this.addButton(
                startButton,
                (button, mouseButton) ->
                        startTest()
        );

        ButtonGeneric clearButton =
                new ButtonGeneric(
                        left + 260,
                        buttonY,
                        90,
                        20,
                        "Clear"
                );

        this.addButton(
                clearButton,
                (button, mouseButton) ->
                        clearFields()
        );

        ButtonGeneric stopButton =
                new ButtonGeneric(
                        left + 360,
                        buttonY,
                        100,
                        20,
                        "Stop"
                );

        this.addButton(
                stopButton,
                (button, mouseButton) -> {

                    MaterialPipelineQueue.stop();

                    status =
                            "Pipeline stopped.";

                    statusColor =
                            0xFFAA00;
                }
        );

        ButtonGeneric closeButton =
                new ButtonGeneric(
                        left,
                        buttonY + 28,
                        100,
                        20,
                        "Close"
                );

        this.addButton(
                closeButton,
                (button, mouseButton) ->
                        closeScreen()
        );
    }

    private void preview() {
        Map<String, Long> targets =
                readTargets();

        if (targets == null
                || targets.isEmpty()) {
            return;
        }

        previewTargets =
                new LinkedHashMap<>(targets);

        ProcessingPlan processing =
                ProcessingPlanner.plan(
                        targets
                );

        CraftingPlan crafting =
                CraftingPlanner.plan(
                        targets
                );

        StringBuilder summary =
                new StringBuilder();

        if (!processing.tasks().isEmpty()) {
            summary.append("PROCESS ");
        }

        if (!crafting.tasks().isEmpty()) {
            if (summary.length() > 0) {
                summary.append("-> ");
            }

            summary.append("CRAFT");
        }

        if (summary.length() == 0) {
            summary.append("GATHER");
        }

        pipelineSummary =
                summary.toString();

        previewStatus =
                "Plan calculated.";

        previewColor =
                0x55FF55;

        status = "";
    }

    private void startTest() {
        Map<String, Long> targets =
                readTargets();

        if (targets == null
                || targets.isEmpty()) {
            return;
        }

        /*
         * Always use the exact current fields when starting.
         * The preview is informational only.
         */
        MaterialPipelineQueue.start(
                targets
        );

        status =
                "Pipeline test started.";

        statusColor =
                0x55FF55;

        closeScreen();
    }

    private Map<String, Long> readTargets() {
        Map<String, Long> targets =
                new LinkedHashMap<>();

        for (int i = 0; i < ROWS; i++) {

            String itemText =
                    itemFields[i]
                            .getText()
                            .trim();

            String amountText =
                    amountFields[i]
                            .getText()
                            .trim();

            if (itemText.isBlank()
                    && amountText.isBlank()) {
                continue;
            }

            if (itemText.isBlank()) {

                setError(
                        "Row "
                                + (i + 1)
                                + ": item is empty."
                );

                return null;
            }

            Identifier id =
                    parseItem(itemText);

            if (id == null
                    || !Registries.ITEM.containsId(id)) {

                setError(
                        "Row "
                                + (i + 1)
                                + ": invalid item "
                                + itemText
                );

                return null;
            }

            if (amountText.isBlank()) {

                setError(
                        "Row "
                                + (i + 1)
                                + ": amount is empty."
                );

                return null;
            }

            long amount;

            try {
                amount =
                        Long.parseLong(
                                amountText
                        );
            } catch (NumberFormatException e) {

                setError(
                        "Row "
                                + (i + 1)
                                + ": amount must be a number."
                );

                return null;
            }

            if (amount <= 0L
                    || amount > MAX_AMOUNT) {

                setError(
                        "Row "
                                + (i + 1)
                                + ": amount must be 1-"
                                + MAX_AMOUNT
                                + "."
                );

                return null;
            }

            targets.merge(
                    id.toString(),
                    amount,
                    Long::sum
            );
        }

        if (targets.isEmpty()) {

            setError(
                    "Enter at least one item."
            );

            return null;
        }

        return targets;
    }

    private void clearFields() {

        for (int i = 0; i < ROWS; i++) {

            itemFields[i].setText("");
            amountFields[i].setText("");
        }

        previewTargets.clear();

        previewStatus = "";

        pipelineSummary = "";

        status = "";
    }

    private void setError(String message) {

        status =
                message;

        statusColor =
                0xFF5555;
    }

    private Identifier parseItem(
            String value
    ) {
        try {
            return value.contains(":")
                    ? new Identifier(value)
                    : new Identifier(
                    "minecraft",
                    value
            );
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void closeScreen() {
        MinecraftClient.getInstance()
                .setScreen(null);
    }

    @Override
    public void render(
            DrawContext drawContext,
            int mouseX,
            int mouseY,
            float delta
    ) {
        this.renderBackground(
                drawContext
        );

        MinecraftClient client =
                MinecraftClient.getInstance();

        int centerX =
                this.width / 2;

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Material Pipeline Test"
                ),
                centerX,
                20,
                0xFFFFFF
        );

        drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(
                        "Test gather -> process -> craft without a schematic."
                ),
                centerX,
                38,
                0xAAAAAA
        );

        int left =
                centerX - 230;

        int top = 49;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Item ID",
                left,
                top,
                0xFFFFFF
        );

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Amount",
                left + 275,
                top,
                0xFFFFFF
        );

        super.render(
                drawContext,
                mouseX,
                mouseY,
                delta
        );

        renderPreview(
                drawContext,
                client,
                left,
                top + ROWS * 32 + 52
        );

        if (!status.isBlank()) {

            drawContext.drawCenteredTextWithShadow(
                    client.textRenderer,
                    Text.literal(status),
                    centerX,
                    this.height - 28,
                    statusColor
            );
        }
    }

    private void renderPreview(
            DrawContext drawContext,
            MinecraftClient client,
            int left,
            int y
    ) {
        if (previewTargets.isEmpty()) {
            return;
        }

        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Preview:",
                left,
                y,
                0xFFFFFF
        );

        int line =
                y + 16;

        int shown = 0;

        for (Map.Entry<String, Long> entry :
                previewTargets.entrySet()) {

            if (shown >= 6) {
                break;
            }

            String item =
                    entry.getKey();

            long amount =
                    entry.getValue();

            long inventory =
                    InventoryUtils.count(
                            item
                    );

            String text =
                    shortItemName(item)
                            + " "
                            + inventory
                            + "/"
                            + amount;

            if (MiningSourceResolver
                    .hasMiningSource(item)) {

                text += "  [mineable]";
            }

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    text,
                    left,
                    line,
                    0xCCCCCC
            );

            line += 14;

            shown++;
        }

        if (!pipelineSummary.isBlank()) {

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Pipeline: "
                            + pipelineSummary,
                    left,
                    line + 6,
                    0x88CCFF
            );

            line += 22;
        }

        if (!previewStatus.isBlank()) {

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    previewStatus,
                    left,
                    line,
                    previewColor
            );
        }
    }

    private String shortItemName(
            String id
    ) {
        int separator =
                id.indexOf(':');

        if (separator >= 0
                && separator + 1 < id.length()) {

            return id.substring(
                    separator + 1
            );
        }

        return id;
    }
}
