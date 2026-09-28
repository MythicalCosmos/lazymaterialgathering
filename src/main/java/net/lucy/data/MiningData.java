package net.lucy.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lucy.model.MiningDrop;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads static Minecraft mining/drop data from mining_drops.json.
 *
 * This class contains data-access logic only. The actual mining calculations
 * belong in MiningResolver.
 */
public final class MiningData {
    private static final String RESOURCE_PATH = "/data/lazymaterialgathering/mining_drops.json";
    private static final Map<String, MiningDrop> DROPS;
    private static final Map<String, String> POTTED_PLANTS;

    static {
        LoadedData data = load();
        DROPS = Collections.unmodifiableMap(data.drops);
        POTTED_PLANTS = Collections.unmodifiableMap(data.pottedPlants);
    }

    private MiningData() {
    }

    /**
     * Returns the mining data for a block, or null if the block has no
     * special mining/drop definition.
     */
    public static MiningDrop getDrop(String blockName) {
        return DROPS.get(blockName);
    }

    /**
     * Returns all explicitly-defined mining drops.
     */
    public static Map<String, MiningDrop> getDrops() {
        return DROPS;
    }

    /**
     * Returns the plant contained by a potted plant block.
     *
     * Example:
     * potted_oak_sapling -> oak_sapling
     */
    public static String getPottedPlant(String blockName) {
        return POTTED_PLANTS.get(blockName);
    }

    /**
     * Returns all potted-plant mappings.
     */
    public static Map<String, String> getPottedPlants() {
        return POTTED_PLANTS;
    }

    private static LoadedData load() {
        try (InputStream input = MiningData.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                throw new IllegalStateException("Missing mining data resource: " + RESOURCE_PATH);
            }

            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    throw new IllegalStateException("Mining data root must be a JSON object");
                }

                JsonObject root = element.getAsJsonObject();
                Map<String, MiningDrop> drops = loadDrops(root);
                Map<String, String> pottedPlants = loadPottedPlants(root);
                return new LoadedData(drops, pottedPlants);
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load mining data: " + RESOURCE_PATH, e);
        }
        catch (RuntimeException e) {
            throw new IllegalStateException("Invalid mining data: " + RESOURCE_PATH, e);
        }
    }

    private static Map<String, MiningDrop> loadDrops(JsonObject root) {
        Map<String, MiningDrop> result = new LinkedHashMap<>();
        JsonElement dropsElement = root.get("drops");
        if (dropsElement == null || dropsElement.isJsonNull()) {
            return result;
        }

        if (!dropsElement.isJsonObject()) {
            throw new IllegalStateException("'drops' must be a JSON object");
        }

        JsonObject dropsObject = dropsElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : dropsObject.entrySet()) {
            String blockName = entry.getKey();
            JsonElement value = entry.getValue();
            if (!value.isJsonObject()) {
                throw new IllegalStateException("Mining drop for '" + blockName + "' must be a JSON object");
            }

            JsonObject dropObject = value.getAsJsonObject();
            String normalDrop = getNullableString(dropObject, "normal_drop");
            int normalCount = getInt(dropObject, "normal_count", 1);
            String silkTouchDrop = getNullableString(dropObject, "silk_touch_drop");
            int silkTouchCount = getInt(dropObject, "silk_touch_count", 1);
            boolean shearsAlsoWork = getBoolean(dropObject,"shears_also_work", false);
            result.put(blockName, new MiningDrop(normalDrop, normalCount, silkTouchDrop, silkTouchCount, shearsAlsoWork));
        }
        return result;
    }

    private static Map<String, String> loadPottedPlants(JsonObject root) {
        Map<String, String> result = new LinkedHashMap<>();
        JsonElement plantsElement = root.get("potted_plants");
        if (plantsElement == null || plantsElement.isJsonNull()) {
            return result;
        }

        if (!plantsElement.isJsonObject()) {
            throw new IllegalStateException("'potted_plants' must be a JSON object");
        }

        JsonObject plantsObject = plantsElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : plantsObject.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) {
                throw new IllegalStateException("Potted plant value for '" + entry.getKey() + "' must be a string");
            }
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return result;
    }

    private static String getNullableString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (!element.isJsonPrimitive()) {
            throw new IllegalStateException("'" + key + "' must be a string or null");
        }
        return element.getAsString();
    }

    private static int getInt(JsonObject object, String key, int defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }

        if (!element.isJsonPrimitive()) {
            throw new IllegalStateException("'" + key + "' must be an integer");
        }
        return element.getAsInt();
    }

    private static boolean getBoolean(JsonObject object, String key, boolean defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }

        if (!element.isJsonPrimitive()) {
            throw new IllegalStateException("'" + key + "' must be a boolean");
        }
        return element.getAsBoolean();
    }

    private record LoadedData(Map<String, MiningDrop> drops, Map<String, String> pottedPlants) {}
}