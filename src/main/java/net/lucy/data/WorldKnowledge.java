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
import net.lucy.calc.MiningSourceResolver;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

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
 * Persistent knowledge of:
 *
 *  - loaded/scanned chunks
 *  - scanned sections
 *  - resource source blocks
 *  - resource coordinates
 *  - biome/resource observations
 *  - exploration targets
 *
 * Baritone remains responsible for movement and pathfinding.
 *
 * Important:
 *
 * WorldKnowledge stores the requested inventory item AND the
 * physical source block separately.
 *
 * Example:
 *
 *     raw_iron
 *         -> iron_ore
 *         -> deepslate_iron_ore
 */
public final class WorldKnowledge {

    private static final int FORMAT_VERSION = 2;

    private static final File FILE =
            new File(
                    FileUtils.getConfigDirectory(),
                    Reference.MOD_ID
                            + "_world_knowledge.json"
            );

    /*
     * dimension -> chunk key -> knowledge
     */
    private static final Map<
            String,
            Map<Long, ChunkKnowledge>
            > CHUNKS = new HashMap<>();

    /*
     * dimension -> resource key -> record
     */
    private static final Map<
            String,
            Map<String, ResourceRecord>
            > RESOURCES = new HashMap<>();

    /*
     * dimension -> biome -> statistics
     */
    private static final Map<
            String,
            Map<String, BiomeStats>
            > BIOMES = new HashMap<>();

    private static final Deque<ScanJob> SCAN_QUEUE =
            new ArrayDeque<>();

    private static final Set<String> QUEUED =
            new HashSet<>();

    private static final int SCAN_BUDGET = 2;

    private static long lastSave;

    private WorldKnowledge() {
    }

    public record ResourceInfo(
            String dimension,
            String item,
            String source,
            BlockPos position
    ) {
    }

    public record ResourceTarget(
            String item,
            String source,
            BlockPos position
    ) {
    }

    public record BiomeScore(
            String biome,
            double score,
            double density,
            double scanConfidence
    ) {
    }

    public static void register() {

        ClientChunkEvents.CHUNK_LOAD.register(
                WorldKnowledge::enqueue
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                WorldKnowledge::tick
        );
    }

    /*
     * ============================================================
     * Loading / saving
     * ============================================================
     */

    public static void load() {

        CHUNKS.clear();
        RESOURCES.clear();
        BIOMES.clear();

        if (!FILE.isFile()) {
            return;
        }

        try {

            JsonElement root =
                    JsonParser.parseString(
                            Files.readString(
                                    FILE.toPath()
                            )
                    );

            if (!root.isJsonObject()) {
                return;
            }

            JsonObject object =
                    root.getAsJsonObject();

            int version =
                    getInt(
                            object,
                            "formatVersion",
                            1
                    );

            loadChunks(
                    object.has("chunks")
                            ? object.getAsJsonObject("chunks")
                            : new JsonObject()
            );

            loadResources(
                    object.has("resources")
                            ? object.getAsJsonObject("resources")
                            : new JsonObject()
            );

            if (version >= FORMAT_VERSION) {

                loadBiomes(
                        object.has("biomes")
                                ? object.getAsJsonObject("biomes")
                                : new JsonObject()
                );

            } else {

                BIOMES.clear();

                for (Map<Long, ChunkKnowledge> chunks :
                        CHUNKS.values()) {

                    for (ChunkKnowledge chunk :
                            chunks.values()) {

                        chunk.scannedSections.clear();
                        chunk.scannedSectionsCount = 0;
                    }
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "[LMG] Could not load world knowledge: "
                            + e.getMessage()
            );
        }
    }

    private static void loadChunks(
            JsonObject chunks
    ) {

        for (var dimension :
                chunks.entrySet()) {

            if (!dimension.getValue().isJsonObject()) {
                continue;
            }

            Map<Long, ChunkKnowledge> map =
                    CHUNKS.computeIfAbsent(
                            dimension.getKey(),
                            ignored -> new HashMap<>()
                    );

            for (var entry :
                    dimension.getValue()
                            .getAsJsonObject()
                            .entrySet()) {

                long key;

                try {
                    key = Long.parseLong(
                            entry.getKey()
                    );
                } catch (NumberFormatException e) {
                    continue;
                }

                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject value =
                        entry.getValue()
                                .getAsJsonObject();

                ChunkKnowledge knowledge =
                        new ChunkKnowledge();

                knowledge.chunkKey = key;

                knowledge.lastSeen =
                        getLong(
                                value,
                                "seen",
                                System.currentTimeMillis()
                        );

                knowledge.biome =
                        getString(
                                value,
                                "biome",
                                null
                        );

                knowledge.totalSections =
                        getInt(
                                value,
                                "totalSections",
                                0
                        );

                if (value.has("scannedSections")
                        && value.get("scannedSections")
                        .isJsonArray()) {

                    for (JsonElement element :
                            value.getAsJsonArray(
                                    "scannedSections"
                            )) {

                        knowledge.scannedSections.add(
                                element.getAsInt()
                        );
                    }
                }

                knowledge.scannedSectionsCount =
                        knowledge.scannedSections.size();

                map.put(
                        key,
                        knowledge
                );
            }
        }
    }

    private static void loadResources(
            JsonObject resources
    ) {

        for (var dimension :
                resources.entrySet()) {

            if (!dimension.getValue().isJsonArray()) {
                continue;
            }

            Map<String, ResourceRecord> map =
                    RESOURCES.computeIfAbsent(
                            dimension.getKey(),
                            ignored -> new HashMap<>()
                    );

            for (JsonElement element :
                    dimension.getValue()
                            .getAsJsonArray()) {

                if (!element.isJsonObject()) {
                    continue;
                }

                try {

                    JsonObject object =
                            element.getAsJsonObject();

                    ResourceRecord record =
                            new ResourceRecord(
                                    object.get("item")
                                            .getAsString(),
                                    object.get("source")
                                            .getAsString(),
                                    object.get("x")
                                            .getAsInt(),
                                    object.get("y")
                                            .getAsInt(),
                                    object.get("z")
                                            .getAsInt(),
                                    object.get("seen")
                                            .getAsLong()
                            );

                    map.put(
                            record.key(),
                            record
                    );

                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void loadBiomes(
            JsonObject biomes
    ) {

        for (var dimension :
                biomes.entrySet()) {

            if (!dimension.getValue().isJsonObject()) {
                continue;
            }

            Map<String, BiomeStats> map =
                    BIOMES.computeIfAbsent(
                            dimension.getKey(),
                            ignored -> new HashMap<>()
                    );

            for (var entry :
                    dimension.getValue()
                            .getAsJsonObject()
                            .entrySet()) {

                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject object =
                        entry.getValue()
                                .getAsJsonObject();

                BiomeStats stats =
                        new BiomeStats();

                stats.observedSections =
                        getLong(
                                object,
                                "observedSections",
                                0L
                        );

                if (object.has("resources")
                        && object.get("resources")
                        .isJsonObject()) {

                    for (var resource :
                            object.getAsJsonObject(
                                    "resources"
                            ).entrySet()) {

                        stats.resourceSections.put(
                                resource.getKey(),
                                resource.getValue()
                                        .getAsLong()
                        );
                    }
                }

                map.put(
                        entry.getKey(),
                        stats
                );
            }
        }
    }

    public static void save() {

        try {

            File directory =
                    FILE.getParentFile();

            if (directory != null) {
                directory.mkdirs();
            }

            JsonObject root =
                    new JsonObject();

            root.addProperty(
                    "formatVersion",
                    FORMAT_VERSION
            );

            JsonObject chunks =
                    new JsonObject();

            for (var dimension :
                    CHUNKS.entrySet()) {

                JsonObject values =
                        new JsonObject();

                for (var entry :
                        dimension.getValue()
                                .entrySet()) {

                    ChunkKnowledge knowledge =
                            entry.getValue();

                    JsonObject value =
                            new JsonObject();

                    value.addProperty(
                            "seen",
                            knowledge.lastSeen
                    );

                    if (knowledge.biome != null) {

                        value.addProperty(
                                "biome",
                                knowledge.biome
                        );
                    }

                    value.addProperty(
                            "totalSections",
                            knowledge.totalSections
                    );

                    JsonArray scanned =
                            new JsonArray();

                    for (Integer index :
                            knowledge.scannedSections) {

                        scanned.add(index);
                    }

                    value.add(
                            "scannedSections",
                            scanned
                    );

                    values.add(
                            Long.toString(
                                    entry.getKey()
                            ),
                            value
                    );
                }

                chunks.add(
                        dimension.getKey(),
                        values
                );
            }

            root.add(
                    "chunks",
                    chunks
            );

            JsonObject resources =
                    new JsonObject();

            for (var dimension :
                    RESOURCES.entrySet()) {

                JsonArray values =
                        new JsonArray();

                for (ResourceRecord record :
                        dimension.getValue()
                                .values()) {

                    JsonObject value =
                            new JsonObject();

                    value.addProperty(
                            "item",
                            record.item
                    );

                    value.addProperty(
                            "source",
                            record.source
                    );

                    value.addProperty(
                            "x",
                            record.x
                    );

                    value.addProperty(
                            "y",
                            record.y
                    );

                    value.addProperty(
                            "z",
                            record.z
                    );

                    value.addProperty(
                            "seen",
                            record.seen
                    );

                    values.add(value);
                }

                resources.add(
                        dimension.getKey(),
                        values
                );
            }

            root.add(
                    "resources",
                    resources
            );

            JsonObject biomes =
                    new JsonObject();

            for (var dimension :
                    BIOMES.entrySet()) {

                JsonObject dimensionBiomes =
                        new JsonObject();

                for (var biome :
                        dimension.getValue()
                                .entrySet()) {

                    BiomeStats stats =
                            biome.getValue();

                    JsonObject value =
                            new JsonObject();

                    value.addProperty(
                            "observedSections",
                            stats.observedSections
                    );

                    JsonObject resourceObject =
                            new JsonObject();

                    for (var resource :
                            stats.resourceSections
                                    .entrySet()) {

                        resourceObject.addProperty(
                                resource.getKey(),
                                resource.getValue()
                        );
                    }

                    value.add(
                            "resources",
                            resourceObject
                    );

                    dimensionBiomes.add(
                            biome.getKey(),
                            value
                    );
                }

                biomes.add(
                        dimension.getKey(),
                        dimensionBiomes
                );
            }

            root.add(
                    "biomes",
                    biomes
            );

            Gson gson =
                    new GsonBuilder()
                            .setPrettyPrinting()
                            .create();

            Files.writeString(
                    FILE.toPath(),
                    gson.toJson(root)
            );

            lastSave =
                    System.currentTimeMillis();

        } catch (Exception e) {

            System.err.println(
                    "[LMG] Could not save world knowledge: "
                            + e.getMessage()
            );
        }
    }

    /*
     * ============================================================
     * Public information
     * ============================================================
     */

    public static List<ResourceInfo> getKnownResources(
            String dimension
    ) {

        List<ResourceInfo> result =
                new ArrayList<>();

        for (ResourceRecord record :
                RESOURCES
                        .getOrDefault(
                                dimension,
                                Map.of()
                        )
                        .values()) {

            result.add(
                    new ResourceInfo(
                            dimension,
                            record.item,
                            record.source,
                            new BlockPos(
                                    record.x,
                                    record.y,
                                    record.z
                            )
                    )
            );
        }

        return result;
    }

    public static int getKnownChunkCount(
            String dimension
    ) {

        return CHUNKS
                .getOrDefault(
                        dimension,
                        Map.of()
                )
                .size();
    }

    public static int getKnownResourceCount(
            String dimension,
            String item
    ) {

        int count = 0;

        for (ResourceRecord record :
                RESOURCES
                        .getOrDefault(
                                dimension,
                                Map.of()
                        )
                        .values()) {

            if (record.item.equals(item)) {
                count++;
            }
        }

        return count;
    }

    public static String getBiomeAt(
            ClientWorld world,
            BlockPos pos
    ) {

        try {

            return world
                    .getBiome(pos)
                    .getKey()
                    .map(key ->
                            key.getValue().toString()
                    )
                    .orElse("unknown");

        } catch (Exception e) {

            return "unknown";
        }
    }

    /*
     * ============================================================
     * Resource lookup
     * ============================================================
     */

    public static Optional<ResourceTarget>
    findNearestResourceTarget(
            String dimension,
            String item,
            String source,
            BlockPos from,
            ClientWorld world
    ) {

        return findNearestResourceTarget(
                dimension,
                item,
                List.of(source),
                Set.of(),
                from,
                world
        );
    }

    public static Optional<ResourceTarget>
    findNearestResourceTarget(
            String dimension,
            String item,
            List<String> sources,
            BlockPos from,
            ClientWorld world
    ) {

        return findNearestResourceTarget(
                dimension,
                item,
                sources,
                Set.of(),
                from,
                world
        );
    }

    public static Optional<ResourceTarget>
    findNearestResourceTarget(
            String dimension,
            String item,
            List<String> sources,
            Set<String> excludedKeys,
            BlockPos from,
            ClientWorld world
    ) {

        if (item == null
                || item.isBlank()
                || sources == null
                || sources.isEmpty()
                || from == null
                || world == null) {

            return Optional.empty();
        }

        Set<String> validSources =
                new HashSet<>(sources);

        Map<String, ResourceRecord> map =
                RESOURCES.getOrDefault(
                        dimension,
                        Map.of()
                );

        ResourceRecord best = null;

        double bestScore =
                Double.MAX_VALUE;

        List<String> stale =
                new ArrayList<>();

        for (ResourceRecord record :
                map.values()) {

            if (!record.item.equals(item)) {
                continue;
            }

            if (!validSources.contains(
                    record.source
            )) {
                continue;
            }

            BlockPos pos =
                    new BlockPos(
                            record.x,
                            record.y,
                            record.z
                    );

            String key =
                    resourceTargetKey(
                            record.item,
                            record.source,
                            pos
                    );

            if (excludedKeys.contains(key)) {
                continue;
            }

            /*
             * If the chunk is loaded, the physical block MUST
             * still be the recorded source.
             */
            if (world.isChunkLoaded(
                    pos.getX() >> 4,
                    pos.getZ() >> 4
            )) {

                if (!matchesSource(
                        world,
                        pos,
                        record.source
                )) {

                    stale.add(record.key());
                    continue;
                }
            }

            double score =
                    getResourceLocationScore(
                            dimension,
                            item,
                            pos,
                            from,
                            world
                    );

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

        return Optional.of(
                new ResourceTarget(
                        best.item,
                        best.source,
                        new BlockPos(
                                best.x,
                                best.y,
                                best.z
                        )
                )
        );
    }

    /**
     * Compatibility API.
     */
    public static Optional<BlockPos>
    findNearestResourceTarget(
            String dimension,
            String item,
            BlockPos from,
            ClientWorld world
    ) {

        List<String> sources =
                MiningSourceResolver
                        .getPossibleSources(item);

        return findNearestResourceTarget(
                dimension,
                item,
                sources,
                Set.of(),
                from,
                world
        ).map(ResourceTarget::position);
    }

    public static void recordResource(
            ClientWorld world,
            BlockPos pos,
            String item,
            String source
    ) {

        if (world == null
                || pos == null
                || item == null
                || source == null
                || item.isBlank()
                || source.isBlank()) {

            return;
        }

        String dimension =
                world.getRegistryKey()
                        .getValue()
                        .toString();

        ResourceRecord record =
                new ResourceRecord(
                        item,
                        source,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        System.currentTimeMillis()
                );

        RESOURCES
                .computeIfAbsent(
                        dimension,
                        ignored -> new HashMap<>()
                )
                .put(
                        record.key(),
                        record
                );
    }

    /*
     * ============================================================
     * Biome statistics
     * ============================================================
     */

    public static double getBiomeScanConfidence(
            String dimension,
            String biome
    ) {

        BiomeStats stats =
                BIOMES
                        .getOrDefault(
                                dimension,
                                Map.of()
                        )
                        .get(biome);

        if (stats == null) {
            return 0.0;
        }

        double confidence =
                100.0 *
                        (
                                1.0 -
                                        Math.exp(
                                                -stats.observedSections
                                                        / 16.0
                                        )
                        );

        return clamp(
                confidence,
                0.0,
                100.0
        );
    }

    public static double getBiomeResourceDensity(
            String dimension,
            String biome,
            String item
    ) {

        BiomeStats stats =
                BIOMES
                        .getOrDefault(
                                dimension,
                                Map.of()
                        )
                        .get(biome);

        if (stats == null
                || stats.observedSections <= 0) {

            return 0.0;
        }

        long resourceSections =
                stats.resourceSections
                        .getOrDefault(
                                item,
                                0L
                        );

        return (double) resourceSections
                / (double) stats.observedSections;
    }

    public static double getBiomeResourceScore(
            String dimension,
            String biome,
            String item
    ) {

        if (biome == null
                || biome.equals("unknown")) {

            return 0.0;
        }

        double density =
                getBiomeResourceDensity(
                        dimension,
                        biome,
                        item
                );

        double confidence =
                getBiomeScanConfidence(
                        dimension,
                        biome
                );

        double densityPercent =
                clamp(
                        density * 100.0,
                        0.0,
                        100.0
                );

        double confidenceMultiplier =
                0.25
                        + confidence / 100.0 * 0.75;

        return clamp(
                densityPercent
                        * confidenceMultiplier,
                0.0,
                100.0
        );
    }

    public static List<BiomeScore>
    getBiomeScores(
            String dimension,
            String item
    ) {

        List<BiomeScore> result =
                new ArrayList<>();

        for (String biome :
                BIOMES
                        .getOrDefault(
                                dimension,
                                Map.of()
                        )
                        .keySet()) {

            result.add(
                    new BiomeScore(
                            biome,
                            getBiomeResourceScore(
                                    dimension,
                                    biome,
                                    item
                            ),
                            getBiomeResourceDensity(
                                    dimension,
                                    biome,
                                    item
                            ),
                            getBiomeScanConfidence(
                                    dimension,
                                    biome
                            )
                    )
            );
        }

        result.sort(
                Comparator.comparingDouble(
                        BiomeScore::score
                ).reversed()
        );

        return result;
    }

    public static Optional<BiomeScore>
    getBestBiomeForResource(
            String dimension,
            String item
    ) {

        return getBiomeScores(
                dimension,
                item
        ).stream().findFirst();
    }

    public static double getResourceLocationScore(
            String dimension,
            String item,
            BlockPos position,
            BlockPos player,
            ClientWorld world
    ) {

        double dx =
                position.getX()
                        - player.getX();

        double dy =
                position.getY()
                        - player.getY();

        double dz =
                position.getZ()
                        - player.getZ();

        double distance =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                )
                        + Math.abs(dy) * 2.0;

        String biome =
                getBiomeAt(
                        world,
                        position
                );

        double biomeScore =
                getBiomeResourceScore(
                        dimension,
                        biome,
                        item
                );

        return distance
                * (
                1.0
                        - Math.min(
                        0.25,
                        biomeScore / 400.0
                )
        );
    }

    /*
     * ============================================================
     * Scanning
     * ============================================================
     */

    public static void rescanLoadedArea(
            int radius
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.world == null
                || client.player == null) {
            return;
        }

        ChunkPos center =
                new ChunkPos(
                        client.player.getBlockPos()
                );

        int r =
                Math.max(
                        0,
                        Math.min(
                                radius,
                                8
                        )
                );

        for (int cx =
             center.x - r;
             cx <= center.x + r;
             cx++) {

            for (int cz =
                 center.z - r;
                 cz <= center.z + r;
                 cz++) {

                if (!client.world
                        .getChunkManager()
                        .isChunkLoaded(
                                cx,
                                cz
                        )) {
                    continue;
                }

                WorldChunk chunk =
                        client.world
                                .getChunkManager()
                                .getWorldChunk(
                                        cx,
                                        cz
                                );

                if (chunk != null) {
                    enqueue(
                            client.world,
                            chunk
                    );
                }
            }
        }
    }

    private static void enqueue(
            ClientWorld world,
            WorldChunk chunk
    ) {

        String dimension =
                world.getRegistryKey()
                        .getValue()
                        .toString();

        long chunkKey =
                chunk.getPos().toLong();

        ChunkSection[] sections =
                chunk.getSectionArray();

        String biome =
                getBiomeAt(
                        world,
                        new BlockPos(
                                chunk.getPos()
                                        .getStartX() + 8,
                                world.getSeaLevel(),
                                chunk.getPos()
                                        .getStartZ() + 8
                        )
                );

        ChunkKnowledge knowledge =
                CHUNKS
                        .computeIfAbsent(
                                dimension,
                                ignored -> new HashMap<>()
                        )
                        .computeIfAbsent(
                                chunkKey,
                                ignored -> new ChunkKnowledge()
                        );

        knowledge.chunkKey = chunkKey;
        knowledge.lastSeen =
                System.currentTimeMillis();
        knowledge.biome = biome;
        knowledge.totalSections =
                sections.length;

        for (int i = 0;
             i < sections.length;
             i++) {

            if (knowledge.scannedSections
                    .contains(i)) {
                continue;
            }

            String key =
                    dimension
                            + ":"
                            + chunkKey
                            + ":"
                            + i;

            if (QUEUED.add(key)) {

                SCAN_QUEUE.addLast(
                        new ScanJob(
                                world,
                                chunk,
                                i,
                                key
                        )
                );
            }
        }
    }

    private static void tick(
            MinecraftClient client
    ) {

        if (client.world == null) {
            return;
        }

        for (int i = 0;
             i < SCAN_BUDGET
                     && !SCAN_QUEUE.isEmpty();
             i++) {

            ScanJob job =
                    SCAN_QUEUE.pollFirst();

            if (job == null) {
                break;
            }

            QUEUED.remove(job.key);

            if (!job.world
                    .getChunkManager()
                    .isChunkLoaded(
                            job.chunk.getPos().x,
                            job.chunk.getPos().z
                    )) {
                continue;
            }

            scanSection(
                    job.world,
                    job.chunk,
                    job.sectionIndex
            );
        }

        if (System.currentTimeMillis()
                - lastSave > 5000L) {

            save();
        }
    }

    private static void scanSection(
            ClientWorld world,
            WorldChunk chunk,
            int sectionIndex
    ) {

        ChunkSection[] sections =
                chunk.getSectionArray();

        if (sectionIndex < 0
                || sectionIndex >= sections.length) {
            return;
        }

        String dimension =
                world.getRegistryKey()
                        .getValue()
                        .toString();

        long chunkKey =
                chunk.getPos().toLong();

        ChunkKnowledge knowledge =
                CHUNKS
                        .computeIfAbsent(
                                dimension,
                                ignored -> new HashMap<>()
                        )
                        .computeIfAbsent(
                                chunkKey,
                                ignored -> new ChunkKnowledge()
                        );

        if (knowledge.scannedSections
                .contains(sectionIndex)) {
            return;
        }

        ChunkSection section =
                sections[sectionIndex];

        int sectionY =
                world.getBottomSectionCoord()
                        + sectionIndex;

        int baseY =
                sectionY << 4;

        String biome =
                getBiomeAt(
                        world,
                        new BlockPos(
                                chunk.getPos()
                                        .getStartX() + 8,
                                baseY + 8,
                                chunk.getPos()
                                        .getStartZ() + 8
                        )
                );

        Set<String> targets =
                new HashSet<>(
                        CalculationData
                                .getRawMaterials()
                                .keySet()
                );

        if (section == null
                || section.isEmpty()
                || targets.isEmpty()) {

            markSectionScanned(
                    world,
                    chunk,
                    sectionIndex,
                    biome,
                    Set.of()
            );

            return;
        }

        BlockPos.Mutable pos =
                new BlockPos.Mutable();

        Set<String> resourcesInSection =
                new HashSet<>();

        for (int x = 0;
             x < 16;
             x++) {

            for (int y = 0;
                 y < 16;
                 y++) {

                for (int z = 0;
                     z < 16;
                     z++) {

                    BlockState state =
                            section.getBlockState(
                                    x,
                                    y,
                                    z
                            );

                    if (state.isAir()) {
                        continue;
                    }

                    String blockName =
                            Registries.BLOCK
                                    .getId(
                                            state.getBlock()
                                    )
                                    .toString();

                    for (String target :
                            targets) {

                        List<String> sources =
                                MiningSourceResolver
                                        .getPossibleSources(
                                                target
                                        );

                        if (!sources.contains(
                                blockName
                        )) {
                            continue;
                        }

                        pos.set(
                                chunk.getPos()
                                        .getStartX() + x,
                                baseY + y,
                                chunk.getPos()
                                        .getStartZ() + z
                        );

                        recordResource(
                                world,
                                pos,
                                target,
                                blockName
                        );

                        resourcesInSection.add(
                                target
                        );
                    }
                }
            }
        }

        markSectionScanned(
                world,
                chunk,
                sectionIndex,
                biome,
                resourcesInSection
        );
    }

    private static void markSectionScanned(
            ClientWorld world,
            WorldChunk chunk,
            int sectionIndex,
            String biome,
            Set<String> resources
    ) {

        String dimension =
                world.getRegistryKey()
                        .getValue()
                        .toString();

        long chunkKey =
                chunk.getPos().toLong();

        ChunkKnowledge knowledge =
                CHUNKS
                        .computeIfAbsent(
                                dimension,
                                ignored -> new HashMap<>()
                        )
                        .computeIfAbsent(
                                chunkKey,
                                ignored -> new ChunkKnowledge()
                        );

        if (!knowledge.scannedSections
                .add(sectionIndex)) {
            return;
        }

        knowledge.scannedSectionsCount =
                knowledge.scannedSections.size();

        knowledge.totalSections =
                chunk.getSectionArray().length;

        BiomeStats biomeStats =
                BIOMES
                        .computeIfAbsent(
                                dimension,
                                ignored -> new HashMap<>()
                        )
                        .computeIfAbsent(
                                biome,
                                ignored -> new BiomeStats()
                        );

        biomeStats.observedSections++;

        for (String resource :
                resources) {

            biomeStats.resourceSections.merge(
                    resource,
                    1L,
                    Long::sum
            );
        }
    }

    /*
     * ============================================================
     * Exploration
     * ============================================================
     */

    /**
     * Compatibility overload.
     */
    public static Optional<BlockPos>
    findBestExplorationTarget(
            String dimension,
            String item,
            BlockPos from,
            ClientWorld world
    ) {

        return findBestExplorationTarget(
                dimension,
                item,
                MiningSourceResolver
                        .getPossibleSources(item),
                from,
                world
        );
    }

    /**
     * Source-aware exploration.
     *
     * Exploration never assumes that the destination itself
     * contains a mineable block.
     *
     * It selects an incomplete/unknown area where the requested
     * physical sources are worth looking for.
     */
    public static Optional<BlockPos>
    findBestExplorationTarget(
            String dimension,
            String item,
            List<String> sources,
            BlockPos from,
            ClientWorld world
    ) {

        if (sources == null
                || sources.isEmpty()
                || from == null
                || world == null) {

            return Optional.empty();
        }

        Map<Long, ChunkKnowledge> chunks =
                CHUNKS.getOrDefault(
                        dimension,
                        Map.of()
                );

        BlockPos best =
                null;

        double bestScore =
                Double.NEGATIVE_INFINITY;

        for (ChunkKnowledge knowledge :
                chunks.values()) {

            if (knowledge.totalSections <= 0) {
                continue;
            }

            if (knowledge.scannedSectionsCount
                    >= knowledge.totalSections) {
                continue;
            }

            ChunkPos chunk =
                    new ChunkPos(
                            knowledge.chunkKey
                    );

            int x =
                    chunk.getStartX() + 8;

            int z =
                    chunk.getStartZ() + 8;

            int y =
                    world.getTopY(
                            Heightmap.Type
                                    .MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            BlockPos target =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            double distance =
                    Math.sqrt(
                            target.getSquaredDistance(
                                    from
                            )
                    );

            double incomplete =
                    100.0 *
                            (
                                    1.0 -
                                            (
                                                    (double)
                                                            knowledge.scannedSectionsCount
                                                            /
                                                            knowledge.totalSections
                                            )
                            );

            /*
             * The biome statistics are keyed by the requested
             * inventory item because that is what the scanner
             * actually records.
             */
            double biomeScore =
                    getBiomeResourceScore(
                            dimension,
                            knowledge.biome,
                            item
                    );

            /*
             * Give source-specific biome observations a chance
             * to contribute when such statistics exist.
             */
            for (String source :
                    sources) {

                biomeScore =
                        Math.max(
                                biomeScore,
                                getBiomeResourceScore(
                                        dimension,
                                        knowledge.biome,
                                        source
                                )
                        );
            }

            double confidence =
                    getBiomeScanConfidence(
                            dimension,
                            knowledge.biome
                    );

            double score =
                    biomeScore * 0.55
                            + incomplete * 0.30
                            + (100.0 - confidence)
                            * 0.15
                            - Math.min(
                            100.0,
                            distance / 100.0
                    );

            if (score > bestScore) {

                bestScore = score;
                best = target;
            }
        }

        if (best != null) {
            return Optional.of(best);
        }

        return findFrontierTarget(
                world,
                from
        );
    }

    private static Optional<BlockPos>
    findFrontierTarget(
            ClientWorld world,
            BlockPos from
    ) {

        ChunkPos center =
                new ChunkPos(from);

        int[] radii = {
                4,
                6,
                8,
                12,
                16
        };

        for (int radius : radii) {

            BlockPos candidate =
                    findUnknownFrontier(
                            world,
                            center,
                            radius
                    );

            if (candidate != null) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    private static BlockPos
    findUnknownFrontier(
            ClientWorld world,
            ChunkPos center,
            int radius
    ) {

        BlockPos best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        String dimension =
                world.getRegistryKey()
                        .getValue()
                        .toString();

        for (int dx = -radius;
             dx <= radius;
             dx++) {

            for (int dz = -radius;
                 dz <= radius;
                 dz++) {

                if (Math.abs(dx) != radius
                        && Math.abs(dz) != radius) {
                    continue;
                }

                int cx =
                        center.x + dx;

                int cz =
                        center.z + dz;

                long key =
                        ChunkPos.toLong(
                                cx,
                                cz
                        );

                ChunkKnowledge known =
                        CHUNKS
                                .getOrDefault(
                                        dimension,
                                        Map.of()
                                )
                                .get(key);

                if (known != null) {
                    continue;
                }

                int x =
                        cx * 16 + 8;

                int z =
                        cz * 16 + 8;

                int y;

                try {

                    y =
                            world.getTopY(
                                    Heightmap.Type
                                            .MOTION_BLOCKING_NO_LEAVES,
                                    x,
                                    z
                            );

                } catch (Exception e) {

                    y =
                            world.getSeaLevel();
                }

                BlockPos target =
                        new BlockPos(
                                x,
                                y,
                                z
                        );

                double distance =
                        target.getSquaredDistance(
                                from
                        );

                if (distance < bestDistance) {

                    bestDistance = distance;
                    best = target;
                }
            }
        }

        return best;
    }

    /*
     * ============================================================
     * Validation
     * ============================================================
     */

    private static boolean matchesSource(
            ClientWorld world,
            BlockPos pos,
            String source
    ) {

        if (source == null
                || source.isBlank()) {
            return false;
        }

        BlockState state =
                world.getBlockState(pos);

        if (state.isAir()) {
            return false;
        }

        String actual =
                Registries.BLOCK
                        .getId(
                                state.getBlock()
                        )
                        .toString();

        return actual.equals(source);
    }

    private static String resourceTargetKey(
            String item,
            String source,
            BlockPos position
    ) {

        return item
                + "|"
                + source
                + "|"
                + position.getX()
                + ","
                + position.getY()
                + ","
                + position.getZ();
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    private static String getString(
            JsonObject object,
            String key,
            String fallback
    ) {

        if (!object.has(key)
                || object.get(key).isJsonNull()) {

            return fallback;
        }

        return object.get(key)
                .getAsString();
    }

    private static long getLong(
            JsonObject object,
            String key,
            long fallback
    ) {

        if (!object.has(key)
                || object.get(key).isJsonNull()) {

            return fallback;
        }

        return object.get(key)
                .getAsLong();
    }

    private static int getInt(
            JsonObject object,
            String key,
            int fallback
    ) {

        if (!object.has(key)
                || object.get(key).isJsonNull()) {

            return fallback;
        }

        return object.get(key)
                .getAsInt();
    }

    private static void maybeSave() {

        if (System.currentTimeMillis()
                - lastSave > 1000L) {

            save();
        }
    }

    /*
     * ============================================================
     * Internal records
     * ============================================================
     */

    private record ScanJob(
            ClientWorld world,
            WorldChunk chunk,
            int sectionIndex,
            String key
    ) {
    }

    private static final class ChunkKnowledge {

        long chunkKey;
        long lastSeen;
        String biome;

        int totalSections;
        int scannedSectionsCount;

        final Set<Integer> scannedSections =
                new HashSet<>();
    }

    private static final class BiomeStats {

        long observedSections;

        final Map<String, Long> resourceSections =
                new HashMap<>();
    }

    private static final class ResourceRecord {

        final String item;
        final String source;

        final int x;
        final int y;
        final int z;

        final long seen;

        ResourceRecord(
                String item,
                String source,
                int x,
                int y,
                int z,
                long seen
        ) {

            this.item = item;
            this.source = source;

            this.x = x;
            this.y = y;
            this.z = z;

            this.seen = seen;
        }

        String key() {

            return item
                    + "@"
                    + x
                    + ","
                    + y
                    + ","
                    + z;
        }
    }
}