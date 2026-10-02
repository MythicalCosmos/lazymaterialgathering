package net.lucy.overlay;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.lucy.baritone.GatheringQueue;
import net.lucy.config.Configs;
import net.lucy.progress.ProgressTracker;
import net.lucy.progress.SchematicProgress;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class GatheringOverlay {
    private static final int PANEL_WIDTH = 310;
    private static final int PANEL_PADDING = 8;
    private static final int LINE_HEIGHT = 12;
    private static final int BAR_HEIGHT = 8;
    private static final int MAX_MATERIALS = 8;
    private GatheringOverlay() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> render(drawContext, tickDelta));
    }

    private static void render(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        if (!Configs.InfoOverlays.INFO_OVERLAY_ENABLED.getBooleanValue()) {
            return;
        }

        SchematicProgress progress = ProgressTracker.get().getCurrent();
        if (progress == null && !GatheringQueue.isRunning()) {
            return;
        }

        int x = Configs.InfoOverlays.INFO_OVERLAY_X.getIntegerValue();
        int y = Configs.InfoOverlays.INFO_OVERLAY_Y.getIntegerValue();
        int height = calculateHeight(progress);
        drawContext.fill(x, y, x + PANEL_WIDTH, y + height, 0xC0101010);
        drawContext.drawBorder(x, y, PANEL_WIDTH, height, 0xFF777777);
        int cursorY = y + PANEL_PADDING;
        drawContext.drawTextWithShadow(client.textRenderer, Text.literal("Lazy Material Gathering"), x + PANEL_PADDING, cursorY, 0xFFFFFFFF);
        cursorY += LINE_HEIGHT + 2;
        if (progress != null) {
            drawProgressSection(drawContext, progress, x, cursorY);
            cursorY += 30;
            drawMaterialsSection(drawContext, progress, x, cursorY);
            cursorY += calculateMaterialsHeight(progress);
        }
        drawGatheringSection(drawContext, x, cursorY);
    }

    private static int calculateHeight(SchematicProgress progress) {
        int height = PANEL_PADDING * 2;
        height += LINE_HEIGHT + 2;
        if (progress != null) {
            height += 30;
            height += calculateMaterialsHeight(progress);
        }

        height += LINE_HEIGHT * 4;
        return height;
    }

    private static void drawProgressSection(DrawContext context, SchematicProgress progress, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        String name = progress.getName();
        if (name.length() > 42) {
            name = name.substring(0, 39) + "...";
        }

        context.drawTextWithShadow(client.textRenderer, Text.literal(name), x + PANEL_PADDING, y, 0xFFCCCCCC);
        int barX = x + PANEL_PADDING;
        int barY = y + LINE_HEIGHT + 2;
        int barWidth = PANEL_WIDTH - PANEL_PADDING * 2;
        context.fill(barX, barY, barX + barWidth, barY + BAR_HEIGHT, 0xFF303030);
        int filledWidth = (int) Math.round(barWidth * progress.getProgress());
        if (filledWidth > 0) {
            context.fill(barX, barY, barX + filledWidth, barY + BAR_HEIGHT, 0xFF55AA55);
        }

        String percentage = String.format("%.1f%%", progress.getProgress() * 100.0);
        context.drawTextWithShadow(client.textRenderer, Text.literal(percentage + "  •  " + progress.getTotalObtained() + " / " + progress.getTotalRequired()), barX, barY + BAR_HEIGHT + 3, 0xFFFFFFFF);
    }

    private static void drawMaterialsSection(DrawContext context, SchematicProgress progress, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Map.Entry<String, Long>> materials = new ArrayList<>(progress.getRequired().entrySet());
        materials.removeIf(entry -> progress.getRemaining(entry.getKey()) <= 0);
        materials.sort(Comparator.comparingLong((Map.Entry<String, Long> entry) -> progress.getRemaining(entry.getKey())).reversed().thenComparing(Map.Entry::getKey));
        int visible = Math.min(MAX_MATERIALS, materials.size());
        int cursorY = y;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Materials remaining"), x + PANEL_PADDING, cursorY, 0xFFFFFFFF);
        cursorY += LINE_HEIGHT;
        for (int i = 0; i < visible; i++) {
            Map.Entry<String, Long> entry = materials.get(i);
            String item = prettyName(entry.getKey());
            long remaining = progress.getRemaining(entry.getKey());
            long required = progress.getRequired(entry.getKey());
            long obtained = Math.min(required, progress.getObtained(entry.getKey()));
            String line = item + ": " + remaining + " left" + " (" + obtained + "/" + required + ")";
            context.drawTextWithShadow(client.textRenderer, Text.literal(line), x + PANEL_PADDING, cursorY, 0xFFDDDDDD);
            cursorY += LINE_HEIGHT;
        }

        if (materials.size() > MAX_MATERIALS) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("... and " + (materials.size() - MAX_MATERIALS) + " more"), x + PANEL_PADDING, cursorY, 0xFFAAAAAA);
        }
    }

    private static int calculateMaterialsHeight(SchematicProgress progress) {
        int count = 1;
        List<Map.Entry<String, Long>> materials = new ArrayList<>(progress.getRequired().entrySet());
        materials.removeIf(entry -> progress.getRemaining(entry.getKey()) <= 0);
        count += Math.min(MAX_MATERIALS, materials.size());
        if (materials.size() > MAX_MATERIALS) {
            count++;
        }
        return count * LINE_HEIGHT;
    }

    private static void drawGatheringSection(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        int cursorY = y;
        String currentItem = GatheringQueue.getCurrentItem();
        if (currentItem == null) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("Bot: idle"), x + PANEL_PADDING, cursorY, 0xFFAAAAAA);
            return;
        }

        context.drawTextWithShadow(client.textRenderer, Text.literal("Current: " + prettyName(currentItem)), x + PANEL_PADDING, cursorY, 0xFFFFFFFF);
        cursorY += LINE_HEIGHT;
        long required = GatheringQueue.getCurrentRequired();
        long inventory = GatheringQueue.getCurrentInventory();
        long remaining = GatheringQueue.getCurrentStillNeeded();
        context.drawTextWithShadow(client.textRenderer, Text.literal("Have: " + inventory + " / " + required + "  •  " + remaining + " left"), x + PANEL_PADDING, cursorY, 0xFFCCCCCC);
        cursorY += LINE_HEIGHT;
        String state = GatheringQueue.isPaused() ? "paused" : "running";
        context.drawTextWithShadow(client.textRenderer, Text.literal("Tasks: " + GatheringQueue.getRemainingCount() + "  •  " + state), x + PANEL_PADDING, cursorY, 0xFFCCCCCC);
        cursorY += LINE_HEIGHT;
        var eta = GatheringQueue.getEstimatedSecondsRemaining();
        String etaText = eta.isPresent() ? formatDuration(eta.get()) : "--";
        context.drawTextWithShadow(client.textRenderer, Text.literal("Baritone ETA: " + etaText), x + PANEL_PADDING, cursorY, 0xFFCCCCCC);
        BlockPos target = GatheringQueue.getCurrentResourceTarget();
        if (target != null) {
            cursorY += LINE_HEIGHT;
            BlockPos player = client.player.getBlockPos();
            double distance = Math.sqrt(player.getSquaredDistance(target));
            context.drawTextWithShadow(client.textRenderer, Text.literal("Resource: " + target.getX() + ", " + target.getY() + ", " + target.getZ() + " (" + String.format("%.0f", distance) + "m)"), x + PANEL_PADDING, cursorY, 0xFFAAAAAA);
        }
    }

    private static String prettyName(String id) {
        if (id == null || id.isBlank()) {
            return "Unknown";
        }

        int colon = id.indexOf(':');
        String name = colon >= 0 ? id.substring(colon + 1) : id;
        return name.replace('_', ' ');
    }

    private static String formatDuration(double seconds) {
        if (seconds < 0) {
            return "--";
        }

        long totalSeconds = Math.round(seconds);
        long minutes = totalSeconds / 60;
        long remainingSeconds = totalSeconds % 60;
        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        }
        return remainingSeconds + "s";
    }
}