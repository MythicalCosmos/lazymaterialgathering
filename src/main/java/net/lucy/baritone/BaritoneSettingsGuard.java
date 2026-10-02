package net.lucy.baritone;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Protects Baritone's settings.txt from an accidental empty/truncated write.
 *
 * This does not replace normal Baritone settings saving.
 * It only keeps a last-known-good copy and restores it if Baritone leaves
 * settings.txt missing or empty when the client stops.
 */
public final class BaritoneSettingsGuard {
    private static final String BACKUP_NAME = "settings.txt.lmg-backup";
    private static Path backup;
    private BaritoneSettingsGuard() {
    }

    public static void install() {
        snapshot();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> restoreIfTruncated());
    }

    public static void snapshot() {
        Path settings = settingsPath();
        backup = settings.resolveSibling(BACKUP_NAME);
        try {
            if (Files.isRegularFile(settings) && Files.size(settings) > 0L) {
                Files.copy(settings, backup, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            System.err.println("[LMG] Could not snapshot Baritone settings: " + e.getMessage());
        }
    }

    public static void restoreIfTruncated() {
        if (backup == null) {
            snapshot();
        }

        Path settings = settingsPath();
        try {
            if (!Files.isRegularFile(backup) || Files.size(backup) == 0L) {
                return;
            }

            if (!Files.isRegularFile(settings) || Files.size(settings) == 0L) {
                Files.copy(backup, settings, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[LMG] Restored Baritone settings after an empty/truncated write.");
            }

        } catch (IOException e) {
            System.err.println("[LMG] Could not restore Baritone settings: " + e.getMessage());
        }
    }

    private static Path settingsPath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("baritone").resolve("settings.txt");
    }
}