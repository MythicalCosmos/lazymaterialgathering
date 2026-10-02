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
import net.minecraft.world.Heightmap;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Persistent knowledge about chunks, resources, biomes and scan coverage.
 *
 * WorldKnowledge is deliberately lightweight:
 *
 *  - Baritone handles pathfinding.
 *  - This class remembers what the client has actually seen.
 *  - Resource locations are persisted.
 *  - Biome/resource statistics are persisted.
 *  - Scan coverage is tracked so partially scanned areas have lower confidence.
 */
public final class WorldKnowledge {
    private static final File FILE = new File(FileUtils.getConfigDirectory(), Reference.MOD_ID + "_world_knowledge.json");
    /*
     * dimension -> chunk long -> chunk knowledge
     */
    private static final Map<String, Map<Long, ChunkKnowledge>> CHUNKS = new HashMap<>();
    /*
     * dimension -> resource key -> resource record
     */
    private static final Map<String, Map<String, ResourceRecord>> RESOURCES = new HashMap<>();
    /*
     * dimension -> biome -> biome statistics
     */
    private static final Map<String, Map<String, BiomeStats>> BIOMES = new HashMap<>();
    /*
     * Scanning is spread across ticks.
     */
    private static final Deque<ScanJob> SCAN_QUEUE = new ArrayDeque<>();
    private static final Set<String> QUEUED = new HashSet<>();
    /*
     * Number of 16x16x16 sections scanned per tick.
     */
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
        BIOMES.clear();
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
        loadChunks(obj.has("chunks") ? obj.getAsJsonObject("chunks") : new JsonObject());
        loadResources(obj.has("resources") ? obj.getAsJsonObject("resources") : new JsonObject());
        loadBiomes(obj.has("biomes") ? obj.getAsJsonObject("biomes") : new JsonObject());
    }

    private static void loadChunks(JsonObject chunks) {
        for (var dimension : chunks.entrySet()) {
            Map<Long, ChunkKnowledge> map = CHUNKS.computeIfAbsent(dimension.getKey(), ignored -> new HashMap<>());
            if (!dimension.getValue().isJsonObject()) {
                continue;
            }

            for (var entry : dimension.getValue().getAsJsonObject().entrySet()) {
                long chunkKey;
                try {
                    chunkKey = Long.parseLong(entry.getKey());
                } catch (NumberFormatException ignored) {
                    continue;
                }

                JsonObject value = entry.getValue().isJsonObject() ? entry.getValue().getAsJsonObject() : null;
                if (value == null) {
                    /*
                     * Backwards compatibility with the original format:
                     *
                     * "chunkLong": timestamp
                     */
                    map.put(chunkKey, new ChunkKnowledge(chunkKey, entry.getValue().getAsLong(), null, 0, 0));
                    continue;
                }

                long seen = getLong(value, "seen", System.currentTimeMillis());
                String biome = getString(value, "biome", null);
                int totalSections = getInt(value, "totalSections", 0);
                int scannedSections = getInt(value, "scannedSections", 0);
                map.put(chunkKey, new ChunkKnowledge(chunkKey, seen, biome, totalSections, scannedSections));
            }
        }
    }

    private static void loadResources(JsonObject resources) {
        for (var dimension : resources.entrySet()) {
            Map<String, ResourceRecord> map = RESOURCES.computeIfAbsent(dimension.getKey(), ignored -> new HashMap<>());
            if (!dimension.getValue().isJsonArray()) {
                continue;
            }

            for (JsonElement element : dimension.getValue().getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject r = element.getAsJsonObject();
                try {
                    ResourceRecord record = new ResourceRecord(r.get("item").getAsString(), r.get("source").getAsString(), r.get("x").getAsInt(), r.get("y").getAsInt(), r.get("z").getAsInt(), r.get("seen").getAsLong());
                    map.put(record.key(), record);
                } catch (Exception ignored) {
                    /*
                     * Ignore malformed individual resource records
                     * rather than invalidating the entire knowledge file.
                     */
                }
            }
        }
    }

    private static void loadBiomes(JsonObject biomes) {
        for (var dimension : biomes.entrySet()) {
            Map<String, BiomeStats> map = BIOMES.computeIfAbsent(dimension.getKey(), ignored -> new HashMap<>());
            if (!dimension.getValue().isJsonObject()) {
                continue;
            }

            for (var entry : dimension.getValue().getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject object = entry.getValue().getAsJsonObject();
                BiomeStats stats = new BiomeStats(getLong(object, "observedSections", 0L), getLong(object, "totalSections", 0L));
                if (object.has("resources") && object.get("resources").isJsonObject()) {
                    for (var resource : object.getAsJsonObject("resources").entrySet()) {
                        stats.resourceOccurrences.put(resource.getKey(), resource.getValue().getAsLong());
                    }
                }
                map.put(entry.getKey(), stats);
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
                    ChunkKnowledge knowledge = entry.getValue();
                    JsonObject value = new JsonObject();
                    value.addProperty("seen", knowledge.lastSeen);
                    if (knowledge.biome != null) {
                        value.addProperty("biome", knowledge.biome);
                    }

                    value.addProperty("totalSections", knowledge.totalSections);
                    value.addProperty("scannedSections", knowledge.scannedSections);
                    values.add(Long.toString(entry.getKey()), value);
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

            /*
             * Biome statistics
             */
            JsonObject biomes = new JsonObject();
            for (var dimension : BIOMES.entrySet()) {
                JsonObject dimensionBiomes = new JsonObject();
                for (var biome : dimension.getValue().entrySet()) {
                    BiomeStats stats = biome.getValue();
                    JsonObject value = new JsonObject();
                    value.addProperty("observedSections", stats.observedSections);
                    value.addProperty("totalSections", stats.totalSections);
                    JsonObject resourcesObject = new JsonObject();
                    for (var resource : stats.resourceOccurrences.entrySet()) {
                        resourcesObject.addProperty(resource.getKey(), resource.getValue());
                    }

                    value.add("resources", resourcesObject);
                    dimensionBiomes.add(biome.getKey(), value);
                }
                biomes.add(dimension.getKey(), dimensionBiomes);
            }

            root.add("biomes", biomes);
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

    public static int getKnownChunkCount(String dimension) {
        return CHUNKS.getOrDefault(dimension, Map.of()).size();
    }

    public static int getKnownResourceCount(String dimension, String item) {
        int count = 0;
        for (ResourceRecord record : RESOURCES.getOrDefault(dimension, Map.of()).values()) {
            if (record.item.equals(item)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the closest valid known resource.
     *
     * The biome/resource score is used as a small tie-breaker,
     * but distance remains the primary factor.
     */
    public static Optional<BlockPos> findNearestResource(String dimension, String item, BlockPos from, ClientWorld world) {
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
            if (!matchesTarget(world, pos, item)) {
                stale.add(record.key());
                continue;
            }

            double dx = record.x - from.getX();
            double dy = record.y - from.getY();
            double dz = record.z - from.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz) + Math.abs(dy) * 2.0;
            String biome = getBiomeAt(world, pos);
            double biomeScore = getBiomeResourceScore(dimension, biome, item);
            /*
             * A very good biome can reduce the effective cost
             * slightly, but it cannot make a resource farther
             * away preferable just because its biome is good.
             */
            double adjustedScore = distance * (1.0 - (Math.min(30.0, biomeScore) / 1000.0));
            if (adjustedScore < bestScore) {
                bestScore = adjustedScore;
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

    /**
     * Records a discovered resource.
     */
    public static void recordResource(ClientWorld world, BlockPos pos, String item, String source) {
        String dimension = world.getRegistryKey().getValue().toString();
        ResourceRecord record = new ResourceRecord(item, source, pos.getX(), pos.getY(), pos.getZ(), System.currentTimeMillis());
        RESOURCES.computeIfAbsent(dimension, ignored -> new HashMap<>()).put(record.key(), record);
    }

    /**
     * Returns the biome identifier at a position.
     */
    public static String getBiomeAt(ClientWorld world, BlockPos pos) {
        try {
            return world.getBiome(pos).getKey().map(key -> key.getValue().toString()).orElse("unknown");
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    /**
     * Returns the scan confidence for a biome.
     *
     * This is NOT the percentage of the entire biome in the world.
     *
     * It is the percentage of sections in the currently observed
     * chunks belonging to this biome that we have actually scanned.
     */
    public static double getBiomeScanConfidence(String dimension, String biome) {
        BiomeStats stats = BIOMES.getOrDefault(dimension, Map.of()).get(biome);
        if (stats == null || stats.totalSections <= 0) {
            return 0.0;
        }

        return clamp(((double) stats.observedSections / stats.totalSections) * 100.0, 0.0, 100.0);
    }

    /**
     * Returns how frequently an item has actually been observed
     * in scanned sections of a biome.
     *
     * This is an empirical density, not a vanilla generation rate.
     */
    public static double getBiomeResourceDensity(String dimension, String biome, String item) {
        BiomeStats stats = BIOMES.getOrDefault(dimension, Map.of()).get(biome);
        if (stats == null || stats.observedSections <= 0) {
            return 0.0;
        }

        long occurrences = stats.resourceOccurrences.getOrDefault(item, 0L);
        return ((double) occurrences / stats.observedSections);
    }

    /**
     * Produces a 0-100 score describing how promising
     * this biome currently looks for an item.
     *
     * The score deliberately combines:
     *
     *   1. observed resource density
     *   2. scan confidence
     *
     * This means a partially scanned biome cannot obtain
     * a falsely high confidence merely because one small
     * scanned section happened to contain the resource.
     */
    public static double getBiomeResourceScore(String dimension, String biome, String item) {
        if (biome == null || biome.equals("unknown")) {
            return 0.0;
        }

        double density = getBiomeResourceDensity(dimension, biome, item);
        double confidence = getBiomeScanConfidence(dimension, biome);
        /*
         * Convert density to a useful 0-100 curve.
         *
         * We intentionally use a logarithmic curve because
         * a biome with 100 observed occurrences should not
         * be treated as 100x better than one with 1.
         */
        double densityScore = Math.min(100.0, Math.log1p(density * 100.0) / Math.log1p(100.0) * 100.0);
        /*
         * Confidence is deliberately weighted strongly.
         *
         * Example:
         *
         * densityScore = 90
         * confidence  = 25%
         *
         * final ≈ 32
         *
         * This says:
         * "we saw something promising, but haven't scanned
         * enough of the biome to trust it yet."
         */
        double confidenceMultiplier = 0.25 + (confidence / 100.0 * 0.75);
        return clamp(densityScore * confidenceMultiplier, 0.0, 100.0);
    }

    /**
     * Returns all known biome scores for a resource.
     *
     * Highest score first.
     */
    public static List<BiomeScore> getBiomeScores(String dimension, String item) {
        List<BiomeScore> result = new ArrayList<>();
        Map<String, BiomeStats> biomes = BIOMES.getOrDefault(dimension, Map.of());
        for (String biome : biomes.keySet()) {
            double density = getBiomeResourceDensity(dimension, biome, item);
            double confidence = getBiomeScanConfidence(dimension, biome);
            double score = getBiomeResourceScore(dimension, biome, item);
            result.add(new BiomeScore(biome, score, density, confidence));
        }

        result.sort(Comparator.comparingDouble(BiomeScore::score).reversed());
        return result;
    }

    /**
     * Returns the best currently known biome for a resource.
     */
    public static Optional<BiomeScore> getBestBiomeForResource(String dimension, String item) {
        return getBiomeScores(dimension, item).stream().findFirst();
    }

    /**
     * Returns the score of a known resource location,
     * taking its biome into account.
     */
    public static double getResourceLocationScore(String dimension, String item, BlockPos position, BlockPos player, ClientWorld world) {
        double dx = position.getX() - player.getX();
        double dy = position.getY() - player.getY();
        double dz = position.getZ() - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz) + Math.abs(dy) * 2.0;
        String biome = getBiomeAt(world, position);
        double biomeScore = getBiomeResourceScore(dimension, biome, item);
        /*
         * Distance dominates.
         *
         * The biome score is useful when choosing between
         * otherwise similar resource candidates.
         */
        return distance * (1.0 - Math.min(0.25, biomeScore / 400.0));
    }

    private static void enqueue(ClientWorld world, WorldChunk chunk) {
        String dimension = world.getRegistryKey().getValue().toString();
        long chunkKey = chunk.getPos().toLong();
        ChunkSection[] sections = chunk.getSectionArray();
        String biome = getBiomeAt(world, new BlockPos(chunk.getPos().getStartX() + 8, world.getSeaLevel(), chunk.getPos().getStartZ() + 8));
        ChunkKnowledge knowledge = CHUNKS.computeIfAbsent(dimension, ignored -> new HashMap<>()).computeIfAbsent(chunkKey, ignored -> new ChunkKnowledge(chunkKey, System.currentTimeMillis(), biome, sections.length, 0));
        knowledge.lastSeen = System.currentTimeMillis();
        knowledge.biome = biome;
        knowledge.totalSections = sections.length;
        /*
         * A chunk can be loaded again after it was already scanned.
         * Only enqueue sections that are not currently pending.
         */
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
        Set<String> targets = new HashSet<>(CalculationData.getRawMaterials().keySet());
        if (targets.isEmpty()) {
            /*
             * Still mark the section as scanned.
             * This matters for biome coverage.
             */
            markSectionScanned(world, chunk, sectionIndex);
            return;
        }

        ChunkSection[] sections = chunk.getSectionArray();
        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            return;
        }

        ChunkSection section = sections[sectionIndex];
        /*
         * An empty section has still been fully inspected.
         */
        if (section == null || section.isEmpty()) {
            markSectionScanned(world, chunk, sectionIndex);
            return;
        }

        int sectionY = world.getBottomSectionCoord() + sectionIndex;
        int baseY = sectionY << 4;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        Map<String, Integer> foundInSection = new HashMap<>();
        String biome = getBiomeAt(world, new BlockPos(chunk.getPos().getStartX() + 8, baseY + 8, chunk.getPos().getStartZ() + 8));
        /*
         * Make sure biome statistics exist.
         */
        BiomeStats biomeStats = BIOMES.computeIfAbsent(world.getRegistryKey().getValue().toString(),
                ignored -> new HashMap<>()).computeIfAbsent(biome, ignored -> new BiomeStats());
        biomeStats.totalSections++;
        /*
         * We count resource occurrences independently from
         * saved coordinates. This lets the biome score measure
         * actual density without storing thousands of positions.
         */
        Map<String, Long> occurrences = new HashMap<>();
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
                    for (String target : targets) {
                        if (itemName.equals(target) || blockName.equals(target) || isKnownOreSource(target, blockName)) {
                            occurrences.merge(target, 1L, Long::sum);
                            int found = foundInSection.getOrDefault(target, 0);
                            /*
                             * Bulk resources only need one saved
                             * navigation target per section.
                             *
                             * Rare resources retain more candidates.
                             */
                            int limit = isBulkResource(target) ? 1 : 32;
                            if (found < limit) {
                                pos.set(chunk.getPos().getStartX() + x, baseY + y, chunk.getPos().getStartZ() + z);
                                recordResource(world, pos, target, blockName);
                                foundInSection.put(target, found + 1);
                            }
                            /*
                             * A block should only contribute once
                             * to a target.
                             */
                            break;
                        }
                    }
                }
            }
        }

        /*
         * Add this section's observations to the biome statistics.
         */
        biomeStats.observedSections++;
        for (var entry : occurrences.entrySet()) {
            biomeStats.resourceOccurrences.merge(entry.getKey(), entry.getValue(), Long::sum);
        }

        markSectionScanned(world, chunk, sectionIndex);
    }

    private static void markSectionScanned(ClientWorld world, WorldChunk chunk, int sectionIndex) {
        String dimension = world.getRegistryKey().getValue().toString();
        long chunkKey = chunk.getPos().toLong();
        ChunkKnowledge knowledge = CHUNKS.computeIfAbsent(dimension, ignored -> new HashMap<>()).computeIfAbsent(chunkKey, ignored -> new ChunkKnowledge(chunkKey, System.currentTimeMillis(), null, chunk.getSectionArray().length, 0));
        /*
         * Prevent double counting if a section is accidentally
         * queued twice during a reload.
         */
        String scanKey = dimension + ":" + chunkKey + ":" + sectionIndex;
        if (knowledge.scannedSectionKeys.add(scanKey)) {
            knowledge.scannedSections++;
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
                    "minecraft:clay" ->
                    true;
            default ->
                    false;
        };
    }

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
            default ->
                    false;
        };
    }

    private static void maybeSave() {
        if (System.currentTimeMillis() - lastSave > 1000L) {
            save();
        }
    }

    private static double clamp(double value, double min, double max) {

        return Math.max(min, Math.min(max, value));
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    private static long getLong(JsonObject object, String key, long fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsLong();
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsInt();
    }

    public record BiomeScore(String biome, double score, double density, double scanConfidence) {
    }

    private record ScanJob(ClientWorld world, WorldChunk chunk, int sectionIndex, String key) {
    }

    private static final class ChunkKnowledge {
        final long chunkKey;
        long lastSeen;
        String biome;
        int totalSections;
        int scannedSections;
        /*
         * Runtime-only protection against duplicate scan accounting.
         *
         * It is intentionally rebuilt after loading; persisted
         * scannedSections is still retained for the overall coverage.
         */
        final Set<String> scannedSectionKeys = new HashSet<>();
        ChunkKnowledge(long chunkKey, long lastSeen, String biome, int totalSections, int scannedSections) {
            this.chunkKey = chunkKey;
            this.lastSeen = lastSeen;
            this.biome = biome;
            this.totalSections = totalSections;
            this.scannedSections = scannedSections;
        }
    }

    private static final class BiomeStats {
        long observedSections;
        long totalSections;
        final Map<String, Long> resourceOccurrences = new HashMap<>();
        BiomeStats() {
        }

        BiomeStats(long observedSections, long totalSections) {
            this.observedSections = observedSections;
            this.totalSections = totalSections;
        }
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

    /**
     * Finds a promising location to explore for a resource.
     *
     * The returned position is the center of a known chunk whose biome:
     *
     *  - has been observed to contain the requested resource,
     *  - has not been completely scanned yet,
     *  - is reasonably close to the player.
     *
     * We intentionally prefer partially scanned areas because they contain
     * useful information while still having unexplored territory.
     */
    public static Optional<BlockPos> findBestExplorationTarget(String dimension, String item, BlockPos from, ClientWorld world) {
        Map<Long, ChunkKnowledge> chunks = CHUNKS.getOrDefault(dimension, Map.of());
        if (chunks.isEmpty()) {
            return Optional.empty();
        }

        BlockPos bestPosition = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (ChunkKnowledge knowledge : chunks.values()) {
            if (knowledge.biome == null || knowledge.biome.isBlank()) {
                continue;
            }

            /*
             * Completely scanned chunks aren't useful as exploration
             * targets. We want somewhere where additional scanning can
             * still teach us something.
             */
            if (knowledge.totalSections > 0 && knowledge.scannedSections >= knowledge.totalSections) {
                continue;
            }

            ChunkPos chunk = new ChunkPos(knowledge.chunkKey);
            int x = chunk.getStartX() + 8;
            int z = chunk.getStartZ() + 8;
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos target = new BlockPos(x, y, z);
            double distance = Math.sqrt(target.getSquaredDistance(from));
            double resourceScore = getBiomeResourceScore(dimension, knowledge.biome, item);
            double confidence = getBiomeScanConfidence(dimension, knowledge.biome);
            /*
             * Exploration value:
             *
             *  - resourceScore makes productive biomes attractive
             *  - incompleteScore makes partially scanned areas attractive
             *  - distance prevents the bot from crossing the world
             *    merely because a biome has a slightly better score
             */
            double incompleteScore = knowledge.totalSections <= 0 ? 50.0 : 100.0 * (1.0 - ((double) knowledge.scannedSections / knowledge.totalSections));
            double distancePenalty = Math.min(100.0, distance / 100.0);
            double score = resourceScore * 0.60 + incompleteScore * 0.30 + (100.0 - confidence) * 0.10 - distancePenalty;
            if (score > bestScore) {
                bestScore = score;
                bestPosition = target;
            }
        }
        return Optional.ofNullable(bestPosition);
    }

    /**
     * Returns true when the biome has enough evidence to be considered
     * worth actively exploring.
     */
    public static boolean shouldExploreBiome(String dimension, String biome, String item) {
        if (biome == null || biome.isBlank()) {
            return false;
        }

        double score = getBiomeResourceScore(dimension, biome, item);
        double confidence = getBiomeScanConfidence(dimension, biome);
        /*
         * Low-confidence areas are still allowed to be explored if
         * they have some evidence of containing the resource.
         */
        return score >= 15.0 || (confidence < 50.0 && getBiomeResourceDensity(dimension, biome, item) > 0.0);
    }
}