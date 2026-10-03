package net.lucy.overlay;

import fi.dy.masa.malilib.gui.GuiBase;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.lucy.baritone.GatheringQueue;
import net.lucy.config.Configs;
import net.lucy.progress.ProgressTracker;
import net.lucy.progress.SchematicProgress;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class GatheringOverlay {

    /*
     * Three display presets.
     *
     * MINIMAL:
     *     Very small status indicator.
     *
     * COMPACT:
     *     Recommended/default mode.
     *
     * DETAILED:
     *     More information, similar to a compact Litematica-style
     *     material/progress panel.
     */
    public enum Preset {
        MINIMAL,
        COMPACT,
        DETAILED
    }

    /*
     * Default preset.
     *
     * You can change this to:
     *
     *     Preset.MINIMAL
     *     Preset.COMPACT
     *     Preset.DETAILED
     */
    private static Preset preset = Preset.DETAILED;

    /*
     * Bottom-right layout settings.
     */
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_PADDING = 7;
    private static final int LINE_HEIGHT = 11;
    private static final int BAR_HEIGHT = 6;

    /*
     * Maximum materials shown in each preset.
     */
    private static final int MINIMAL_MATERIALS = 0;
    private static final int COMPACT_MATERIALS = 5;
    private static final int DETAILED_MATERIALS = 10;

    /*
     * Gap from the screen edges.
     */
    private static final int SCREEN_MARGIN = 8;

    /*
     * Colors.
     */
    private static final int COLOR_BACKGROUND = 0xC0101010;
    private static final int COLOR_BORDER = 0xFF666666;
    private static final int COLOR_HEADER = 0xFFFFFFFF;
    private static final int COLOR_PRIMARY = 0xFFE0E0E0;
    private static final int COLOR_SECONDARY = 0xFFAAAAAA;
    private static final int COLOR_MUTED = 0xFF777777;
    private static final int COLOR_PROGRESS_BACKGROUND = 0xFF303030;
    private static final int COLOR_PROGRESS = 0xFF55AA55;

    private GatheringOverlay() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(
                (drawContext, tickDelta) ->
                        render(drawContext)
        );
    }

    /**
     * Change the overlay preset at runtime.
     */
    public static void setPreset(Preset newPreset) {
        if (newPreset != null) {
            preset = newPreset;
        }
    }

    /**
     * Get the currently selected preset.
     */
    public static Preset getPreset() {
        return preset;
    }

    private static void render(DrawContext context) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null
                || client.world == null) {
            return;
        }

        if (!Configs.InfoOverlays
                .INFO_OVERLAY_ENABLED
                .getBooleanValue()) {
            return;
        }

        SchematicProgress progress =
                ProgressTracker.get().getCurrent();

        if (progress == null
                && !GatheringQueue.isRunning()) {
            return;
        }

        switch (preset) {

            case MINIMAL ->
                    renderMinimal(
                            context,
                            client,
                            progress
                    );

            case COMPACT ->
                    renderCompact(
                            context,
                            client,
                            progress
                    );

            case DETAILED ->
                    renderDetailed(
                            context,
                            client,
                            progress
                    );
        }
    }

    // ============================================================
    // MINIMAL
    // ============================================================

    private static void renderMinimal(
            DrawContext context,
            MinecraftClient client,
            SchematicProgress progress
    ) {

        int width = 190;

        List<String> lines =
                new ArrayList<>();

        if (progress != null) {

            String name =
                    shorten(
                            progress.getName(),
                            24
                    );

            String percentage =
                    String.format(
                            "%.0f%%",
                            progress.getProgress() * 100.0
                    );

            long remaining =
                    getTotalRemaining(progress);

            lines.add(
                    "LMG  •  "
                            + name
            );

            lines.add(
                    buildProgressBar(
                            progress.getProgress(),
                            12
                    )
                            + " "
                            + percentage
            );

            lines.add(
                    remaining
                            + " blocks left"
            );

        } else {

            lines.add("LMG");

        }

        drawPanel(
                context,
                client,
                lines,
                width
        );
    }

    // ============================================================
    // COMPACT
    // ============================================================

    private static void renderCompact(
            DrawContext context,
            MinecraftClient client,
            SchematicProgress progress
    ) {

        List<String> lines =
                new ArrayList<>();

        if (progress != null) {

            String name =
                    shorten(
                            progress.getName(),
                            28
                    );

            lines.add(
                    "LMG  •  "
                            + name
            );

            lines.add(
                    buildProgressBar(
                            progress.getProgress(),
                            18
                    )
                            + " "
                            + String.format(
                            "%.0f%%",
                            progress.getProgress() * 100.0
                    )
            );

            lines.add(
                    progress.getTotalObtained()
                            + " / "
                            + progress.getTotalRequired()
                            + " blocks"
            );

            lines.add("");

            List<Map.Entry<String, Long>>
                    materials =
                    getRemainingMaterials(
                            progress
                    );

            int max =
                    Math.min(
                            COMPACT_MATERIALS,
                            materials.size()
                    );

            if (max > 0) {

                lines.add("Missing");

                for (int i = 0; i < max; i++) {

                    Map.Entry<String, Long>
                            entry =
                            materials.get(i);

                    lines.add(
                            formatMaterialLine(
                                    entry.getKey(),
                                    progress.getRemaining(
                                            entry.getKey()
                                    )
                            )
                    );
                }

                if (materials.size() > max) {

                    lines.add(
                            "+ "
                                    + (
                                    materials.size()
                                            - max
                            )
                                    + " more"
                    );
                }
            }

            String status =
                    getStatusText();

            if (!status.isBlank()) {

                lines.add("");

                lines.add(status);
            }

        } else {

            lines.add("LMG  •  No schematic");

            String status =
                    getStatusText();

            if (!status.isBlank()) {
                lines.add(status);
            }
        }

        drawPanel(
                context,
                client,
                lines,
                PANEL_WIDTH
        );
    }

    // ============================================================
    // DETAILED
    // ============================================================

    private static void renderDetailed(
            DrawContext context,
            MinecraftClient client,
            SchematicProgress progress
    ) {

        List<String> lines =
                new ArrayList<>();

        if (progress != null) {

            String name =
                    shorten(
                            progress.getName(),
                            34
                    );

            lines.add(
                    "LMG  •  "
                            + name
            );

            lines.add(
                    buildProgressBar(
                            progress.getProgress(),
                            23
                    )
                            + " "
                            + String.format(
                            "%.1f%%",
                            progress.getProgress() * 100.0
                    )
            );

            lines.add(
                    progress.getTotalObtained()
                            + " / "
                            + progress.getTotalRequired()
                            + " blocks"
            );

            lines.add("");

            List<Map.Entry<String, Long>>
                    materials =
                    getRemainingMaterials(
                            progress
                    );

            int max =
                    Math.min(
                            DETAILED_MATERIALS,
                            materials.size()
                    );

            if (max > 0) {

                lines.add("MISSING");

                for (int i = 0; i < max; i++) {

                    Map.Entry<String, Long>
                            entry =
                            materials.get(i);

                    String item =
                            prettyName(
                                    entry.getKey()
                            );

                    long remaining =
                            progress.getRemaining(
                                    entry.getKey()
                            );

                    long required =
                            progress.getRequired(
                                    entry.getKey()
                            );

                    long obtained =
                            Math.min(
                                    required,
                                    progress.getObtained(
                                            entry.getKey()
                                    )
                            );

                    lines.add(
                            item
                                    + "  "
                                    + remaining
                                    + " left"
                                    + "  "
                                    + obtained
                                    + "/"
                                    + required
                    );
                }

                if (materials.size() > max) {

                    lines.add(
                            "... and "
                                    + (
                                    materials.size()
                                            - max
                            )
                                    + " more"
                    );
                }
            }

            lines.add("");

            String currentItem =
                    GatheringQueue.getCurrentItem();

            if (currentItem != null) {

                lines.add(
                        "Current: "
                                + prettyName(
                                currentItem
                        )
                );

                long required =
                        GatheringQueue
                                .getCurrentRequired();

                long inventory =
                        GatheringQueue
                                .getCurrentInventory();

                long remaining =
                        GatheringQueue
                                .getCurrentStillNeeded();

                lines.add(
                        "Have "
                                + inventory
                                + " / "
                                + required
                                + "  •  "
                                + remaining
                                + " left"
                );
            }

            var eta =
                    GatheringQueue
                            .getEstimatedSecondsRemaining();

            if (eta.isPresent()) {

                lines.add(
                        "Baritone ETA: "
                                + formatDuration(
                                eta.get()
                        )
                );
            }

        } else {

            lines.add(
                    "LMG  •  No schematic loaded"
            );

            String status =
                    getStatusText();

            if (!status.isBlank()) {
                lines.add(status);
            }
        }

        drawPanel(
                context,
                client,
                lines,
                PANEL_WIDTH
        );
    }

    // ============================================================
    // PANEL
    // ============================================================

    private static void drawPanel(
            DrawContext context,
            MinecraftClient client,
            List<String> lines,
            int width
    ) {

        if (lines.isEmpty()) {
            return;
        }

        int height =
                PANEL_PADDING * 2
                        + lines.size() * LINE_HEIGHT;

        /*
         * Always anchor to the bottom-right.
         *
         * This means the overlay automatically follows the
         * Minecraft window when its size changes.
         */
        int x =
                client.getWindow().getScaledWidth()
                        - width
                        - SCREEN_MARGIN;

        int y =
                client.getWindow().getScaledHeight()
                        - height
                        - SCREEN_MARGIN;

        /*
         * Background.
         */
        context.fill(
                x,
                y,
                x + width,
                y + height,
                COLOR_BACKGROUND
        );

        /*
         * Border.
         */
        context.drawBorder(
                x,
                y,
                width,
                height,
                COLOR_BORDER
        );

        int cursorY =
                y + PANEL_PADDING;

        for (int i = 0; i < lines.size(); i++) {

            String line =
                    lines.get(i);

            int color;

            if (i == 0) {
                color = COLOR_HEADER;
            } else if (line.equals("Missing")
                    || line.equals("MISSING")) {
                color = COLOR_HEADER;
            } else if (line.startsWith("...")) {
                color = COLOR_MUTED;
            } else if (line.startsWith("Current:")
                    || line.startsWith("Baritone")) {
                color = COLOR_SECONDARY;
            } else {
                color = COLOR_PRIMARY;
            }

            context.drawTextWithShadow(
                    client.textRenderer,
                    Text.literal(line),
                    x + PANEL_PADDING,
                    cursorY,
                    color
            );

            cursorY += LINE_HEIGHT;
        }
    }

    // ============================================================
    // MATERIALS
    // ============================================================

    private static List<Map.Entry<String, Long>>
    getRemainingMaterials(
            SchematicProgress progress
    ) {

        List<Map.Entry<String, Long>>
                materials =
                new ArrayList<>(
                        progress
                                .getRequired()
                                .entrySet()
                );

        materials.removeIf(
                entry ->
                        progress.getRemaining(
                                entry.getKey()
                        ) <= 0
        );

        materials.sort(
                Comparator
                        .<Map.Entry<String, Long>>
                                comparingLong(
                                entry ->
                                        progress
                                                .getRemaining(
                                                        entry.getKey()
                                                )
                        )
                        .reversed()
                        .thenComparing(
                                Map.Entry::getKey
                        )
        );

        return materials;
    }

    private static String formatMaterialLine(
            String item,
            long remaining
    ) {

        return prettyName(item)
                + "  "
                + remaining;
    }

    // ============================================================
    // STATUS
    // ============================================================

    private static String getStatusText() {

        String currentItem =
                GatheringQueue.getCurrentItem();

        if (currentItem == null) {

            if (GatheringQueue.isPaused()) {
                return "Bot: paused";
            }

            return "Bot: idle";
        }

        String item =
                prettyName(currentItem);

        String state =
                GatheringQueue.isPaused()
                        ? "paused"
                        : "gathering";

        return item
                + "  •  "
                + state;
    }

    // ============================================================
    // PROGRESS
    // ============================================================

    private static String buildProgressBar(
            double progress,
            int segments
    ) {

        progress =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                progress
                        )
                );

        int filled =
                (int) Math.round(
                        progress * segments
                );

        StringBuilder bar =
                new StringBuilder();

        for (int i = 0; i < segments; i++) {

            bar.append(
                    i < filled
                            ? "█"
                            : "░"
            );
        }

        return bar.toString();
    }

    private static long getTotalRemaining(
            SchematicProgress progress
    ) {

        long total = 0;

        for (String item :
                progress.getRequired().keySet()) {

            total +=
                    Math.max(
                            0,
                            progress.getRemaining(
                                    item
                            )
                    );
        }

        return total;
    }

    // ============================================================
    // TEXT
    // ============================================================

    private static String prettyName(
            String id
    ) {

        if (id == null
                || id.isBlank()) {
            return "Unknown";
        }

        int colon =
                id.indexOf(':');

        String name =
                colon >= 0
                        ? id.substring(colon + 1)
                        : id;

        String[] words =
                name.replace('_', ' ')
                        .split(" ");

        StringBuilder result =
                new StringBuilder();

        for (String word : words) {

            if (word.isBlank()) {
                continue;
            }

            if (result.length() > 0) {
                result.append(' ');
            }

            result.append(
                    Character.toUpperCase(
                            word.charAt(0)
                    )
            );

            if (word.length() > 1) {
                result.append(
                        word.substring(1)
                );
            }
        }

        return result.toString();
    }

    private static String shorten(
            String text,
            int maxLength
    ) {

        if (text == null) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(
                0,
                Math.max(
                        0,
                        maxLength - 3
                )
        ) + "...";
    }

    private static String formatDuration(
            double seconds
    ) {

        if (seconds < 0) {
            return "--";
        }

        long totalSeconds =
                Math.round(seconds);

        long hours =
                totalSeconds / 3600;

        long minutes =
                (totalSeconds % 3600) / 60;

        long remainingSeconds =
                totalSeconds % 60;

        if (hours > 0) {

            return hours
                    + "h "
                    + minutes
                    + "m";
        }

        if (minutes > 0) {

            return minutes
                    + "m "
                    + remainingSeconds
                    + "s";
        }

        return remainingSeconds + "s";
    }
}