package net.lucy.overlay;

import net.lucy.progress.ProgressTracker;
import net.lucy.progress.SchematicProgress;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.Map;

public class GatheringOverlay {
    public static void init(){
        HudRenderCallback.EVENT.register(GatheringOverlay::render);
    }

    private static void render(DrawContext context, float tickDelta){
        SchematicProgress progress = ProgressTracker.get().getCurrent();
        if(progress == null)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        int x = 10;
        int y = 10;
        context.drawText(client.textRenderer, "Lazy Material Gathering", x, y, 0xffffff, true);
        y += 14;
        context.drawText(client.textRenderer, "Schematic: " + progress.getName(), x, y, 0xffffff, false);
        y += 14;
        int percent = (int) (progress.getProgress() * 100);
        context.drawText(client.textRenderer, "Progress: " + percent + "%", x, y, 0xffffff, false);
        y += 18;
        context.drawText(client.textRenderer, "Materials:", x, y, 0xffffff, true);
        y += 14;
        for(Map.Entry<String,Long> entry : progress.getRequired().entrySet()){
            String item = entry.getKey();
            long remaining = progress.getRemaining(item);
            context.drawText(client.textRenderer, item + ": " + remaining + " left", x, y, 0xffffff, false);
            y += 12;
        }
    }
}