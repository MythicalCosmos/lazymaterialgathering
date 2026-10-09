package net.lucy.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.lucy.baritone.MaterialPipelineCoordinator;
import net.lucy.config.Configs;
import net.lucy.data.InventoryUtils;
import net.lucy.baritone.MaterialPipelineState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.lucy.baritone.GatheringQueue;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Bottom-right material pipeline HUD.
 *
 * Displays the current state of:
 *
 *     gather -> process -> craft
 *
 * The overlay is intentionally read-only. The pipeline coordinator
 * remains responsible for all actual work.
 */
public final class MaterialPipelineOverlay {

    private static boolean registered = false;

    private static final int RIGHT_MARGIN = 8;
    private static final int BOTTOM_MARGIN = 8;

    private static final int WIDTH = 235;

    private static final int PADDING = 8;

    private static final int LINE_HEIGHT = 12;

    private static final int TITLE_HEIGHT = 15;

    private MaterialPipelineOverlay() {
    }

    public static void register() {

        if (registered) {
            return;
        }

        registered = true;

        HudRenderCallback.EVENT.register(
                MaterialPipelineOverlay::render
        );
    }

    private static void render(
            DrawContext drawContext,
            float tickDelta
    ) {

        if (!Configs.InfoOverlays.INFO_OVERLAY_ENABLED
                .getBooleanValue()) {

            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client == null
                || client.player == null
                || client.world == null) {

            return;
        }

        MaterialPipelineState state =
                MaterialPipelineCoordinator.getState();

        /*
         * IDLE means there is no active pipeline to display.
         */
        if (state == MaterialPipelineState.IDLE) {
            return;
        }

        Map<String, Long> targets =
                MaterialPipelineCoordinator.getTargets();

        String currentItem =
                MaterialPipelineCoordinator.getCurrentItem();

        int remainingTasks =
                MaterialPipelineCoordinator.getRemainingTasks();

        String blockedReason =
                MaterialPipelineCoordinator.getBlockedReason();

        /*
         * Build the display rows first so the panel height can
         * be calculated before drawing it.
         */
        List<MaterialRow> rows =
                buildRows(
                        targets,
                        currentItem
                );

        int maxMaterials =
                Math.max(
                        0,
                        Configs.InfoOverlays
                                .INFO_OVERLAY_MAX_MATERIALS
                                .getIntegerValue()
                );

        if (maxMaterials > 0
                && rows.size() > maxMaterials) {

            rows = new ArrayList<>(
                    rows.subList(
                            0,
                            maxMaterials
                    )
            );
        }

        int extraLines = 0;

        /*
         * Current item line.
         */
        if (currentItem != null
                && !currentItem.isBlank()) {

            extraLines++;
        }

        /*
         * Gathering-specific lines.
         */
        if (state == MaterialPipelineState.GATHERING
                || state == MaterialPipelineState.VERIFYING_GATHERING) {

            String source =
                    GatheringQueue.getCurrentResourceSource();

            BlockPos resourceTarget =
                    GatheringQueue.getCurrentResourceTarget();

            BlockPos explorationTarget =
                    GatheringQueue.getCurrentExplorationTarget();

            if (source != null
                    && !source.isBlank()) {

                extraLines++;
            }

            if (resourceTarget != null
                    || explorationTarget != null) {

                extraLines++;
            }

            if (GatheringQueue
                    .getEstimatedSecondsRemaining()
                    .isPresent()) {

                extraLines++;
            }
        }

        /*
         * Remaining tasks line.
         */
        if (remainingTasks > 0) {
            extraLines++;
        }

        /*
         * Block reason can take multiple visual lines.
         */
        List<String> blockedLines =
                wrapText(
                        blockedReason,
                        WIDTH - PADDING * 2
                );

        if (state == MaterialPipelineState.BLOCKED
                && !blockedLines.isEmpty()) {

            extraLines +=
                    Math.min(
                            4,
                            blockedLines.size()
                    );
        }

        /*
         * COMPLETE / PAUSED.
         */
        if (state == MaterialPipelineState.COMPLETE
                || state == MaterialPipelineState.PAUSED) {

            extraLines++;
        }

        /*
         * State itself is one line.
         */
        int contentLines =
                1
                        + extraLines
                        + rows.size();

        int height =
                PADDING * 2
                        + TITLE_HEIGHT
                        + contentLines * LINE_HEIGHT
                        + 4;

        int screenWidth =
                client.getWindow().getScaledWidth();

        int screenHeight =
                client.getWindow().getScaledHeight();

        int x =
                screenWidth
                        - WIDTH
                        - RIGHT_MARGIN;

        int y =
                screenHeight
                        - height
                        - BOTTOM_MARGIN;

        /*
         * Background opacity is configured as 10-100%.
         */
        int opacity =
                Math.max(
                        10,
                        Math.min(
                                100,
                                Configs.InfoOverlays
                                        .INFO_OVERLAY_OPACITY
                                        .getIntegerValue()
                        )
                );

        int alpha =
                Math.round(
                        255.0F
                                * (opacity / 100.0F)
                );

        int background =
                (alpha << 24)
                        | 0x101010;

        drawContext.fill(
                x,
                y,
                x + WIDTH,
                y + height,
                background
        );

        /*
         * Thin outer border.
         */
        drawContext.fill(
                x,
                y,
                x + WIDTH,
                y + 1,
                0xA0FFFFFF
        );

        drawContext.fill(
                x,
                y + height - 1,
                x + WIDTH,
                y + height,
                0x60000000
        );

        drawContext.fill(
                x,
                y,
                x + 1,
                y + height,
                0x60000000
        );

        drawContext.fill(
                x + WIDTH - 1,
                y,
                x + WIDTH,
                y + height,
                0x60000000
        );

        int textX =
                x + PADDING;

        int textY =
                y + PADDING;

        /*
         * Title.
         */
        drawContext.drawTextWithShadow(
                client.textRenderer,
                "Lazy Material Gathering",
                textX,
                textY,
                0xFFFFFF
        );

        textY += TITLE_HEIGHT;

        /*
         * State.
         */
        String stateText =
                getStateDisplayName(state);

        drawContext.drawTextWithShadow(
                client.textRenderer,
                stateText,
                textX,
                textY,
                getStateColor(state)
        );

        textY += LINE_HEIGHT;

        /*
         * Current task.
         */
        /*
         * Gathering-specific physical resource information.
         *
         * This deliberately comes from GatheringQueue rather than
         * trying to infer a block from the requested inventory item.
         */
        if (state == MaterialPipelineState.GATHERING
                || state == MaterialPipelineState.VERIFYING_GATHERING) {

            String source =
                    GatheringQueue.getCurrentResourceSource();

            BlockPos resourceTarget =
                    GatheringQueue.getCurrentResourceTarget();

            BlockPos explorationTarget =
                    GatheringQueue.getCurrentExplorationTarget();

            if (source != null
                    && !source.isBlank()) {

                drawContext.drawTextWithShadow(
                        client.textRenderer,
                        "Source: " + formatItem(source),
                        textX,
                        textY,
                        0xBBD8FF
                );

                textY += LINE_HEIGHT;
            }

            if (resourceTarget != null) {

                drawContext.drawTextWithShadow(
                        client.textRenderer,
                        "Resource: "
                                + formatPosition(resourceTarget),
                        textX,
                        textY,
                        0xBBD8FF
                );

                textY += LINE_HEIGHT;

            } else if (explorationTarget != null) {

                drawContext.drawTextWithShadow(
                        client.textRenderer,
                        "Exploring: "
                                + formatPosition(explorationTarget),
                        textX,
                        textY,
                        0xAAAAFF
                );

                textY += LINE_HEIGHT;
            }

            java.util.Optional<Double> estimatedSeconds =
                    GatheringQueue.getEstimatedSecondsRemaining();

            if (estimatedSeconds.isPresent()) {

                String eta =
                        formatDuration(
                                estimatedSeconds.get()
                        );

                drawContext.drawTextWithShadow(
                        client.textRenderer,
                        "Path ETA: " + eta,
                        textX,
                        textY,
                        0xAAAAAA
                );

                textY += LINE_HEIGHT;
            }
        }
        if (currentItem != null
                && !currentItem.isBlank()) {

            String displayItem =
                    formatItem(currentItem);

            long currentInventory =
                    InventoryUtils.count(
                            currentItem
                    );

            long required =
                    targets.getOrDefault(
                            normalize(currentItem),
                            0L
                    );

            if (required > 0L) {

                String currentText =
                        "Current: "
                                + displayItem
                                + " "
                                + currentInventory
                                + "/"
                                + required;

                drawContext.drawTextWithShadow(
                        client.textRenderer,
                        currentText,
                        textX,
                        textY,
                        0xE0E0E0
                );

            } else {

                drawContext.drawTextWithShadow(
                        client.textRenderer,
                        "Current: "
                                + displayItem,
                        textX,
                        textY,
                        0xE0E0E0
                );
            }

            textY += LINE_HEIGHT;
        }

        /*
         * Remaining queue tasks.
         */
        if (remainingTasks > 0) {

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Remaining tasks: "
                            + remainingTasks,
                    textX,
                    textY,
                    0xBDBDBD
            );

            textY += LINE_HEIGHT;
        }

        /*
         * Terminal state messages.
         */
        if (state == MaterialPipelineState.COMPLETE) {

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "All requested materials complete.",
                    textX,
                    textY,
                    0x55FF55
            );

            textY += LINE_HEIGHT;
        }

        if (state == MaterialPipelineState.PAUSED) {

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "Pipeline paused",
                    textX,
                    textY,
                    0xFFFF55
            );

            textY += LINE_HEIGHT;
        }

        /*
         * Blocked state.
         */
        if (state == MaterialPipelineState.BLOCKED
                && !blockedLines.isEmpty()) {

            drawContext.drawTextWithShadow(
                    client.textRenderer,
                    "BLOCKED",
                    textX,
                    textY,
                    0xFF5555
            );

            textY += LINE_HEIGHT;

            int displayed =
                    Math.min(
                            4,
                            blockedLines.size()
                    );

            for (int i = 0;
                 i < displayed;
                 i++) {

                drawContext.drawTextWithShadow(
                        client.textRenderer,
                        blockedLines.get(i),
                        textX,
                        textY,
                        0xFFAAAA
                );

                textY += LINE_HEIGHT;
            }
        }

        /*
         * Material rows.
         */
        for (MaterialRow row : rows) {

            drawMaterialRow(
                    drawContext,
                    client,
                    row,
                    textX,
                    textY
            );

            textY += LINE_HEIGHT;
        }
    }

    private static void drawMaterialRow(
            DrawContext drawContext,
            MinecraftClient client,
            MaterialRow row,
            int x,
            int y
    ) {

        /*
         * Small item icon.
         */
        ItemStack stack =
                getItemStack(row.item());

        if (!stack.isEmpty()) {

            drawContext.drawItem(
                    stack,
                    x,
                    y - 3
            );
        }

        int textX =
                x + 18;

        String label =
                formatItem(row.item());

        String quantity =
                row.have()
                        + "/"
                        + row.required();

        int color =
                row.have() >= row.required()
                        ? 0x55FF55
                        : 0xFFFFFF;

        drawContext.drawTextWithShadow(
                client.textRenderer,
                label,
                textX,
                y,
                color
        );

        int quantityWidth =
                client.textRenderer
                        .getWidth(quantity);

        drawContext.drawTextWithShadow(
                client.textRenderer,
                quantity,
                x + WIDTH
                        - PADDING
                        - quantityWidth,
                y,
                row.have() >= row.required()
                        ? 0x55FF55
                        : 0xBDBDBD
        );
    }

    private static List<MaterialRow> buildRows(
            Map<String, Long> targets,
            String currentItem
    ) {

        List<MaterialRow> rows =
                new ArrayList<>();

        if (targets == null
                || targets.isEmpty()) {

            return rows;
        }

        /*
         * Current item gets priority.
         */
        if (currentItem != null
                && !currentItem.isBlank()) {

            String normalized =
                    normalize(currentItem);

            Long required =
                    targets.get(normalized);

            if (required != null
                    && required > 0L) {

                rows.add(
                        new MaterialRow(
                                normalized,
                                required,
                                InventoryUtils.count(
                                        normalized
                                )
                        )
                );
            }
        }

        /*
         * Remaining targets are sorted by the amount still needed,
         * largest first. This keeps the overlay useful for large
         * gathering jobs.
         */
        List<MaterialRow> remaining =
                new ArrayList<>();

        for (Map.Entry<String, Long> entry
                : targets.entrySet()) {

            String item =
                    normalize(entry.getKey());

            long required =
                    Math.max(
                            0L,
                            entry.getValue()
                    );

            if (required <= 0L) {
                continue;
            }

            long have =
                    InventoryUtils.count(item);

            /*
             * Don't duplicate the current row.
             */
            if (currentItem != null
                    && item.equals(
                    normalize(currentItem)
            )) {

                continue;
            }

            remaining.add(
                    new MaterialRow(
                            item,
                            required,
                            have
                    )
            );
        }

        remaining.sort(
                Comparator
                        .comparingLong(
                                MaterialRow::remaining
                        )
                        .reversed()
                        .thenComparing(
                                MaterialRow::item
                        )
        );

        rows.addAll(remaining);

        return rows;
    }

    private static ItemStack getItemStack(
            String itemId
    ) {

        Identifier id =
                parse(itemId);

        if (id == null) {
            return ItemStack.EMPTY;
        }

        var item =
                Registries.ITEM.get(id);

        if (item == null) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }

    private static String getStateDisplayName(
            MaterialPipelineState state
    ) {

        return switch (state.name()) {

            case "GATHERING" ->
                    "Gathering materials";

            case "VERIFYING_GATHERING" ->
                    "Verifying gathered materials";

            case "PROCESSING" ->
                    "Processing materials";

            case "VERIFYING_PROCESSING" ->
                    "Verifying processed materials";

            case "CRAFTING" ->
                    "Crafting";

            case "PAUSED" ->
                    "Paused";

            case "COMPLETE" ->
                    "Complete";

            case "BLOCKED" ->
                    "Blocked";

            default ->
                    state.name();
        };
    }

    private static int getStateColor(
            MaterialPipelineState state
    ) {

        return switch (state.name()) {

            case "GATHERING" ->
                    0x55AAFF;

            case "VERIFYING_GATHERING" ->
                    0xAAAAFF;

            case "PROCESSING" ->
                    0xFFAA55;

            case "VERIFYING_PROCESSING" ->
                    0xFFCC88;

            case "CRAFTING" ->
                    0x55FF55;

            case "PAUSED" ->
                    0xFFFF55;

            case "COMPLETE" ->
                    0x55FF55;

            case "BLOCKED" ->
                    0xFF5555;

            default ->
                    0xFFFFFF;
        };
    }

    private static String formatItem(
            String itemId
    ) {

        if (itemId == null
                || itemId.isBlank()) {

            return "";
        }

        String value =
                itemId;

        int colon =
                value.indexOf(':');

        if (colon >= 0) {
            value =
                    value.substring(
                            colon + 1
                    );
        }

        StringBuilder result =
                new StringBuilder();

        boolean capitalize =
                true;

        for (int i = 0;
             i < value.length();
             i++) {

            char c =
                    value.charAt(i);

            if (c == '_') {

                result.append(' ');
                capitalize = true;

                continue;
            }

            if (capitalize) {

                result.append(
                        Character.toUpperCase(c)
                );

                capitalize = false;

            } else {

                result.append(c);
            }
        }

        return result.toString();
    }

    private static List<String> wrapText(
            String text,
            int maxWidth
    ) {

        List<String> result =
                new ArrayList<>();

        if (text == null
                || text.isBlank()) {

            return result;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client == null) {
            return result;
        }

        String[] words =
                text.split("\\s+");

        StringBuilder line =
                new StringBuilder();

        for (String word : words) {

            String candidate =
                    line.isEmpty()
                            ? word
                            : line
                            + " "
                            + word;

            if (client.textRenderer
                    .getWidth(candidate)
                    <= maxWidth) {

                line.setLength(0);
                line.append(candidate);

            } else {

                if (!line.isEmpty()) {
                    result.add(
                            line.toString()
                    );
                }

                line.setLength(0);
                line.append(word);
            }
        }

        if (!line.isEmpty()) {
            result.add(
                    line.toString()
            );
        }

        return result;
    }
    private static String formatPosition(
            BlockPos pos
    ) {

        if (pos == null) {
            return "-";
        }

        return pos.getX()
                + " "
                + pos.getY()
                + " "
                + pos.getZ();
    }

    private static String formatDuration(
            double seconds
    ) {

        if (seconds < 0.0D
                || Double.isNaN(seconds)
                || Double.isInfinite(seconds)) {

            return "-";
        }

        if (seconds < 60.0D) {

            return String.format(
                    "%.1fs",
                    seconds
            );
        }

        long totalSeconds =
                Math.round(seconds);

        long minutes =
                totalSeconds / 60L;

        long remainingSeconds =
                totalSeconds % 60L;

        if (minutes < 60L) {

            return String.format(
                    "%dm %02ds",
                    minutes,
                    remainingSeconds
            );
        }

        long hours =
                minutes / 60L;

        long remainingMinutes =
                minutes % 60L;

        return String.format(
                "%dh %02dm",
                hours,
                remainingMinutes
        );
    }

    private static String normalize(
            String id
    ) {

        if (id == null
                || id.isBlank()) {

            return "";
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }

    private static Identifier parse(
            String id
    ) {

        try {

            if (id == null
                    || id.isBlank()) {

                return null;
            }

            return id.contains(":")
                    ? new Identifier(id)
                    : new Identifier(
                    "minecraft",
                    id
            );

        } catch (IllegalArgumentException e) {

            return null;
        }
    }

    private record MaterialRow(
            String item,
            long required,
            long have
    ) {

        long remaining() {
            return Math.max(
                    0L,
                    required - have
            );
        }
    }
}