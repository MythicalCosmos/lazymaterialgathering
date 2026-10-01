package net.lucy.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.util.FileUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lucy.Reference;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Persistent, lightweight knowledge about chunks that the client has loaded.
 *
 * This is intentionally NOT a duplicate of Baritone's complete world cache.
 *
 * It records:
 *
 *  - chunks that have been observed
 *  - useful resource positions
 *  - the last time those resources were observed
 *
 * Baritone remains responsible for actual pathfinding and movement.
 */
public final class WorldKnowledge {
    private static final File FILE = new File(FileUtils.getConfigDirectory(), Reference.MOD_ID + "_world_knowledge.json");
    //dimension -> chunk long -> last seen time
    private static final Map<String, Map<Long, Long>> CHUNKS = new HashMap<>();
    //dimension -> resource key -> record
    private static final Map<String, Map<String, ResourceRecord>> RESOURCES = new HashMap<>();
    //Chunk section scanning is spread across ticks
    //so a newly loaded chunk does not cause a huge
    //single-frame spike.
    private static final Deque<ScanJob> SCAN_QUEUE = new ArrayDeque<>();
    private static final Set<String> QUEUED = new HashSet<>();
    //Number of 16x16x16 sections scanned per tick.
    private static int scanBudget = 2;
    private static long lastSave = 0L;
    private WorldKnowledge() {
    }

    public static void register() {
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> enqueue(world, chunk));
        ClientTickEvents.END_CLIENT_TICK.register(WorldKnowledge::tick);
    }

    public static void load() {
        CHUNKS.clear();
        RESOURCES.clear();
        if (!FILE.isFile()) {
            return;
        }

        JsonElement root;
        try {
            root = JsonParser.parseString(Files.readString(FILE.toPath()));
        } catch (Exception e) {
            System.err.println("[LMG] Could not load world knowledge: " + e.getMessage());
            return;
        }

        if (root == null || !root.isJsonObject()) {
            return;
        }

        JsonObject obj = root.getAsJsonObject();
        //Chunks
        JsonObject chunks = obj.has("chunks") && obj.get("chunks").isJsonObject() ? obj.getAsJsonObject("chunks") : new JsonObject();
        for (var dimension : chunks.entrySet()) {
            Map<Long, Long> map = CHUNKS.computeIfAbsent(dimension.getKey(), k -> new HashMap<>());
            for (var entry : dimension.getValue().getAsJsonObject().entrySet()) {
                map.put(Long.parseLong(entry.getKey()), entry.getValue().getAsLong());
            }
        }

        /*
         * Resources
         */
        JsonObject resources = obj.has("resources") && obj.get("resources").isJsonObject() ? obj.getAsJsonObject("resources") : new JsonObject();
        for (var dimension : resources.entrySet()) {
            Map<String, ResourceRecord> map = RESOURCES.computeIfAbsent(dimension.getKey(), k -> new HashMap<>());
            for (JsonElement element : dimension.getValue().getAsJsonArray()) {
                JsonObject r = element.getAsJsonObject();
                ResourceRecord record = new ResourceRecord(
                                r.get("item")
                                        .getAsString(),
                                r.get("source")
                                        .getAsString(),
                                r.get("x")
                                        .getAsInt(),
                                r.get("y")
                                        .getAsInt(),
                                r.get("z")
                                        .getAsInt(),
                                r.get("seen")
                                        .getAsLong());
                map.put(record.key(), record);
            }
        }
    }

    public static void save() {
        try {
            File directory = FILE.getParentFile();
            if (directory != null) {
                directory.mkdirs();
            }

            JsonObject root = new JsonObject();
            /*
             * Chunks
             */
            JsonObject chunks = new JsonObject();
            for (var dimension : CHUNKS.entrySet()) {
                JsonObject values = new JsonObject();
                for (var entry : dimension.getValue().entrySet()) {
                    values.addProperty(Long.toString(entry.getKey()), entry.getValue());
                }
                chunks.add(dimension.getKey(), values);
            }

            root.add("chunks", chunks);
            /*
             * Resources
             */
            JsonObject resources = new JsonObject();
            for (var dimension : RESOURCES.entrySet()) {
                JsonArray values = new JsonArray();
                for (ResourceRecord record : dimension.getValue().values()) {
                    JsonObject value = new JsonObject();
                    value.addProperty("item", record.item);
                    value.addProperty("source", record.source);
                    value.addProperty("x", record.x);
                    value.addProperty("y", record.y);
                    value.addProperty("z", record.z);
                    value.addProperty("seen", record.seen);
                    values.add(value);
                }
                resources.add(dimension.getKey(), values);
            }

            root.add("resources", resources);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(FILE.toPath(), gson.toJson(root));
            lastSave = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("[LMG] Could not save world knowledge: " + e.getMessage());
        }
    }
    /**
     * Re-scans currently loaded chunks around the player.
     *
     * radius = 1 means a 3x3 chunk area.
     */
    public static void rescanLoadedArea(int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        ChunkPos center = new ChunkPos(client.player.getBlockPos());
        int r = Math.max(0, Math.min(radius, 8));
        for (int cx = center.x - r; cx <= center.x + r; cx++) {
            for (int cz = center.z - r; cz <= center.z + r; cz++) {
                if (client.world.getChunkManager().isChunkLoaded(cx, cz)) {
                    WorldChunk chunk = client.world.getChunkManager().getWorldChunk(cx, cz);
                    if (chunk != null) {
                        enqueue(client.world, chunk);
                    }
                }
            }
        }
    }

    public static int getKnownChunkCount(
            String dimension
    ) {

        return CHUNKS.getOrDefault(dimension, Map.of()).size();
    }

    public static int getKnownResourceCount(
            String dimension,
            String item) {
        int count = 0;
        for (ResourceRecord record : RESOURCES.getOrDefault(dimension, Map.of()).values()) {
            if (record.item.equals(item)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Finds the closest known valid resource.
     *
     * This method intentionally does NOT attempt to reproduce
     * Baritone's pathfinder.
     *
     * It only selects a candidate.
     *
     * Baritone then determines the actual route.
     */
    public static Optional<BlockPos>
    findNearestResource(String dimension, String item, BlockPos from, ClientWorld world) {
        Map<String, ResourceRecord> map = RESOURCES.get(dimension);
        if (map == null) {
            return Optional.empty();
        }

        ResourceRecord best = null;
        double bestScore = Double.MAX_VALUE;
        List<String> stale = new ArrayList<>();
        for (ResourceRecord record : map.values()) {
            if (!record.item.equals(item)) {
                continue;
            }

            BlockPos pos = new BlockPos(record.x, record.y, record.z);
            /*
             * The resource may have been mined,
             * moved, replaced, etc.
             */
            if (!matchesTarget(world, pos, item)) {
                stale.add(record.key());
                continue;
            }

            double dx = record.x - from.getX();
            double dy = record.y - from.getY();
            double dz = record.z - from.getZ();
            /*
             * Candidate selection only.
             *
             * Baritone still determines the actual path.
             */
            double score = Math.sqrt(dx * dx + dz * dz) + Math.abs(dy) * 2.0;
            if (score < bestScore) {
                bestScore = score;
                best = record;
            }
        }

        for (String key : stale) {
            map.remove(key);
        }

        if (!stale.isEmpty()) {
            maybeSave();
        }

        if (best == null) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(best.x, best.y, best.z));
    }

    public static void recordResource(ClientWorld world, BlockPos pos, String item, String source) {
        String dimension = world.getRegistryKey().getValue().toString();
        ResourceRecord record = new ResourceRecord(item, source, pos.getX(), pos.getY(), pos.getZ(), System.currentTimeMillis());
        RESOURCES.computeIfAbsent(dimension, k -> new HashMap<>()).put(record.key(), record);
    }

    private static void enqueue(ClientWorld world, WorldChunk chunk) {
        String dimension = world.getRegistryKey().getValue().toString();
        long chunkKey = chunk.getPos().toLong();
        CHUNKS.computeIfAbsent(dimension, k -> new HashMap<>()).put(chunkKey, System.currentTimeMillis());
        ChunkSection[] sections = chunk.getSectionArray();
        for (int i = 0; i < sections.length; i++) {
            String key = dimension + ":" + chunkKey + ":" + i;
            if (QUEUED.add(key)) {
                SCAN_QUEUE.addLast(new ScanJob(world, chunk, i, key));
            }
        }
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null) {
            return;
        }

        for (int i = 0; i < scanBudget && !SCAN_QUEUE.isEmpty(); i++) {
            ScanJob job = SCAN_QUEUE.pollFirst();
            if (job == null) {
                break;
            }

            QUEUED.remove(job.key);
            scanSection(job.world, job.chunk, job.sectionIndex);
        }
        /*
         * Save approximately every five seconds.
         */
        if (System.currentTimeMillis() - lastSave > 5000L) {
            save();
        }
    }

    private static void scanSection(ClientWorld world, WorldChunk chunk, int sectionIndex) {
        /*
         * Only scan resources that the current material
         * calculation actually needs.
         */
        Set<String> targets = new HashSet<>(CalculationData.getRawMaterials().keySet());
        if (targets.isEmpty()) {
            return;
        }

        ChunkSection[] sections = chunk.getSectionArray();
        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            return;
        }

        ChunkSection section = sections[sectionIndex];
        if (section == null || section.isEmpty()) {
            return;
        }

        int sectionY = world.getBottomSectionCoord() + sectionIndex;
        int baseY = sectionY << 4;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        Map<String, Integer> foundInSection = new HashMap<>();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockState state = section.getBlockState(x, y, z);
                    if (state.isAir()) {
                        continue;
                    }

                    Identifier blockId = Registries.BLOCK.getId(state.getBlock());
                    Identifier itemId = Registries.ITEM.getId(state.getBlock().asItem());
                    String blockName = blockId.toString();
                    String itemName = itemId.toString();
                    for (
                            String target :
                            targets
                    ) {

                        if (itemName.equals(target) || blockName.equals(target) || isKnownOreSource(target, blockName)) {
                            int found = foundInSection.getOrDefault(target, 0);
                            /*
                             * Bulk blocks don't need thousands
                             * of individual saved coordinates.
                             */
                            int limit = isBulkResource(target) ? 1 : 32;
                            if (found < limit) {
                                pos.set(chunk.getPos().getStartX() + x, baseY + y, chunk.getPos().getStartZ() + z);
                                recordResource(world, pos, target, blockName);
                                foundInSection.put(target, found + 1);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean matchesTarget(ClientWorld world, BlockPos pos, String target) {

        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }

        String block = Registries.BLOCK.getId(state.getBlock()).toString();
        String item = Registries.ITEM.getId(state.getBlock().asItem()).toString();
        return item.equals(target) || block.equals(target) || isKnownOreSource(target, block);
    }

    private static boolean isBulkResource(String item) {
        return switch (item) {
            case
                    "minecraft:stone",
                    "minecraft:cobblestone",
                    "minecraft:deepslate",
                    "minecraft:dirt",
                    "minecraft:coarse_dirt",
                    "minecraft:grass_block",
                    "minecraft:sand",
                    "minecraft:red_sand",
                    "minecraft:gravel",
                    "minecraft:netherrack",
                    "minecraft:end_stone",
                    "minecraft:clay" -> true;
            default -> false;
        };
    }

    /**
     * Maps the item we want to obtain to blocks that
     * can provide it.
     *
     * This is intentionally small for now.
     *
     * Your existing MiningData/mining_drops system should
     * eventually become the authoritative source for this.
     */
    private static boolean isKnownOreSource(String item, String block) {
        return switch (item) {
            case "minecraft:coal" ->
                    block.equals("minecraft:coal_ore") || block.equals("minecraft:deepslate_coal_ore");
            case "minecraft:iron_ingot", "minecraft:iron_ore" ->
                    block.equals("minecraft:iron_ore") || block.equals("minecraft:deepslate_iron_ore");
            case "minecraft:gold_ingot", "minecraft:gold_ore" ->
                    block.equals("minecraft:gold_ore") || block.equals("minecraft:deepslate_gold_ore") || block.equals("minecraft:nether_gold_ore");
            case "minecraft:copper_ingot", "minecraft:copper_ore" ->
                    block.equals("minecraft:copper_ore") || block.equals("minecraft:deepslate_copper_ore");
            case "minecraft:diamond" ->
                    block.equals("minecraft:diamond_ore") || block.equals("minecraft:deepslate_diamond_ore");
            case "minecraft:emerald" ->
                    block.equals("minecraft:emerald_ore") || block.equals("minecraft:deepslate_emerald_ore");
            case "minecraft:lapis_lazuli" ->
                    block.equals("minecraft:lapis_ore") || block.equals("minecraft:deepslate_lapis_ore");
            case "minecraft:redstone" ->
                    block.equals("minecraft:redstone_ore") || block.equals("minecraft:deepslate_redstone_ore");
            case "minecraft:quartz" ->
                    block.equals("minecraft:nether_quartz_ore");
            case "minecraft:netherite_scrap" ->
                    block.equals("minecraft:ancient_debris");
            default -> false;
        };
    }

    private static void maybeSave() {
        if (System.currentTimeMillis() - lastSave > 1000L) {
            save();
        }
    }

    private record ScanJob(ClientWorld world, WorldChunk chunk, int sectionIndex, String key) {
    }

    private static final class ResourceRecord {
        final String item;
        final String source;
        final int x;
        final int y;
        final int z;
        final long seen;
        ResourceRecord(String item, String source, int x, int y, int z, long seen) {
            this.item = item;
            this.source = source;
            this.x = x;
            this.y = y;
            this.z = z;
            this.seen = seen;
        }

        String key() {
            return item + "@" + x + "," + y + "," + z;
        }
    }
}