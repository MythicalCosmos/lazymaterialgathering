package net.lucy.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.lucy.Reference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Remembers which biome you've actually seen at which chunk, in which dimension, so a
 * later trip for a biome-specific block (see BiomeData) can be pointed at somewhere you've
 * already found, instead of exploring blind. This is separate from Baritone's own world
 * cache: Baritone remembers *blocks* it's seen; this remembers *biomes*, keyed by chunk
 * rather than exact block position (a biome doesn't change block-by-block), and persists
 * across sessions the same way GuiState's other saved data does.
 *
 * Only the biome at the chunk's surface-ish height is recorded (see recordAt below) --
 * biomes barely ever change with height in the same column, and tracking one value per
 * chunk keeps the save file small and the "nearest match" search fast.
 */
public final class BiomeChunkCache {
    private static final String STORAGE_FILE_NAME = Reference.MOD_ID + "_biomes.json";
    // dimension id (e.g. "minecraft:overworld") -> chunk key ("x,z") -> biome id
    private static final Map<String, Map<String, String>> DATA = new HashMap<>();
    private static final int RECORD_INTERVAL_TICKS = 40; // every 2 seconds, not every tick
    private static int ticksUntilNextRecord = 0;
    private BiomeChunkCache() {
    }

    /** Called once, from your mod's client init, to record the player's biome as they explore. */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (--ticksUntilNextRecord > 0) {
                return;
            }
            ticksUntilNextRecord = RECORD_INTERVAL_TICKS;
            recordPlayerPosition();
        });
    }

    /** Records the biome at the player's current position. Call this occasionally (see register()), not every tick. */
    public static void recordPlayerPosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        recordAt(client.world.getRegistryKey(), client.player.getBlockPos());
    }

    public static void recordAt(RegistryKey<World> dimension, BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        RegistryEntry<Biome> biomeEntry = client.world.getBiome(pos);
        Optional<RegistryKey<Biome>> biomeKey = biomeEntry.getKey();
        if (biomeKey.isEmpty()) {
            return;
        }

        String biomeId = biomeKey.get().getValue().getPath();
        String dimensionId = dimension.getValue().toString();
        String chunkKey = chunkKeyOf(pos);
        Map<String, String> chunks = DATA.computeIfAbsent(dimensionId, key -> new HashMap<>());
        if (biomeId.equals(chunks.get(chunkKey)) == false) {
            chunks.put(chunkKey, biomeId);
            save();
        }
    }

    /**
     * The nearest recorded chunk (as a block position at its center, same Y as fromPos)
     * whose biome is one of the given ones, in the given dimension. Empty if nothing
     * matching has been seen yet -- that's not a failure, it just means explore first.
     */
    public static Optional<BlockPos> findNearestChunkWithBiome(RegistryKey<World> dimension, java.util.Set<String> biomes, BlockPos from) {
        Map<String, String> chunks = DATA.get(dimension.getValue().toString());
        if (chunks == null || chunks.isEmpty()) {
            return Optional.empty();
        }

        String bestKey = null;
        long bestDistanceSq = Long.MAX_VALUE;
        for (Map.Entry<String, String> entry : chunks.entrySet()) {
            if (biomes.contains(entry.getValue()) == false) {
                continue;
            }

            BlockPos center = centerOf(entry.getKey(), from.getY());
            long dx = center.getX() - from.getX();
            long dz = center.getZ() - from.getZ();
            long distanceSq = dx * dx + dz * dz;
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestKey = entry.getKey();
            }
        }
        return bestKey == null ? Optional.empty() : Optional.of(centerOf(bestKey, from.getY()));
    }

    /** How many chunks have a recorded biome, in the given dimension. For a status display. */
    public static int getRecordedChunkCount(RegistryKey<World> dimension) {
        Map<String, String> chunks = DATA.get(dimension.getValue().toString());
        return chunks == null ? 0 : chunks.size();
    }

    private static String chunkKeyOf(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        return chunkPos.x + "," + chunkPos.z;
    }

    private static BlockPos centerOf(String chunkKey, int y) {
        String[] parts = chunkKey.split(",", 2);
        int chunkX = Integer.parseInt(parts[0]);
        int chunkZ = Integer.parseInt(parts[1]);
        return new BlockPos(ChunkSectionPos.getBlockCoord(chunkX) + 8, y, ChunkSectionPos.getBlockCoord(chunkZ) + 8);
    }

    // ---------- Saving and loading ----------
    public static void load() {
        JsonElement element = JsonUtils.parseJsonFile(getStorageFile());
        DATA.clear();
        if (element == null || element.isJsonObject() == false) {
            return;
        }

        for (Map.Entry<String, JsonElement> dimensionEntry : element.getAsJsonObject().entrySet()) {
            Map<String, String> chunks = new HashMap<>();
            for (Map.Entry<String, JsonElement> chunkEntry : dimensionEntry.getValue().getAsJsonObject().entrySet()) {
                chunks.put(chunkEntry.getKey(), chunkEntry.getValue().getAsString());
            }
            DATA.put(dimensionEntry.getKey(), chunks);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, Map<String, String>> dimensionEntry : DATA.entrySet()) {
            JsonObject chunksObject = new JsonObject();
            for (Map.Entry<String, String> chunkEntry : dimensionEntry.getValue().entrySet()) {
                chunksObject.addProperty(chunkEntry.getKey(), chunkEntry.getValue());
            }
            root.add(dimensionEntry.getKey(), chunksObject);
        }
        JsonUtils.writeJsonToFile(root, getStorageFile());
    }

    private static File getStorageFile() {
        File dir = FileUtils.getConfigDirectory();
        if (dir.exists() == false) {
            dir.mkdirs();
        }
        return new File(dir, STORAGE_FILE_NAME);
    }
}