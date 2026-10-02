package net.lucy.baritone;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Protects Baritone's settings file from accidental empty/truncated writes.
 *
 * This does not replace Baritone's own settings system.
 *
 * Instead:
 *
 *   settings.txt
 *        |
 *        v
 *   last-known-good backup
 *
 * If settings.txt is missing or empty when Minecraft closes,
 * the backup is restored.
 */
public final class BaritoneSettingsGuard {

    private static final String BACKUP_NAME =
            "settings.txt.lmg-backup";

    private static final long SNAPSHOT_INTERVAL_MS =
            10_000L;

    private static Path backup;

    private static long lastSnapshot;

    private BaritoneSettingsGuard() {
    }

    public static void install() {
        snapshot();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long now = System.currentTimeMillis();

            if (now - lastSnapshot >= SNAPSHOT_INTERVAL_MS) {
                snapshot();
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(
                client -> restoreIfTruncated()
        );
    }

    public static void snapshot() {
        Path settings = settingsPath();
        backup = settings.resolveSibling(BACKUP_NAME);

        try {
            if (!Files.isRegularFile(settings)) {
                return;
            }

            long size = Files.size(settings);

            if (size <= 0L) {
                return;
            }

            Files.createDirectories(
                    settings.getParent()
            );

            Files.copy(
                    settings,
                    backup,
                    StandardCopyOption.REPLACE_EXISTING
            );

            lastSnapshot =
                    System.currentTimeMillis();

        } catch (IOException e) {
            System.err.println(
                    "[LMG] Could not snapshot Baritone settings: "
                            + e.getMessage()
            );
        }
    }

    public static void restoreIfTruncated() {
        if (backup == null) {
            backup = settingsPath()
                    .resolveSibling(BACKUP_NAME);
        }

        Path settings = settingsPath();

        try {
            if (!Files.isRegularFile(backup)) {
                return;
            }

            if (Files.size(backup) <= 0L) {
                return;
            }

            boolean missing =
                    !Files.isRegularFile(settings);

            boolean empty =
                    !missing && Files.size(settings) == 0L;

            if (missing || empty) {
                Files.createDirectories(
                        settings.getParent()
                );

                Files.copy(
                        backup,
                        settings,
                        StandardCopyOption.REPLACE_EXISTING
                );

                System.out.println(
                        "[LMG] Restored Baritone settings after "
                                + "an empty/truncated write."
                );
            }

        } catch (IOException e) {
            System.err.println(
                    "[LMG] Could not restore Baritone settings: "
                            + e.getMessage()
            );
        }
    }

    private static Path settingsPath() {
        return MinecraftClient.getInstance()
                .runDirectory
                .toPath()
                .resolve("baritone")
                .resolve("settings.txt");
    }
}