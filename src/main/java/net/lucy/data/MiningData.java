package net.lucy.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lucy.model.MiningDrop;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public static MiningDrop getDrop(String blockName) {
        return DROPS.get(blockName);
    }

    public static Map<String, MiningDrop> getDrops() {
        return DROPS;
    }

    public static String getPottedPlant(String blockName) {
        return POTTED_PLANTS.get(blockName);
    }

    public static Map<String, String>
    getPottedPlants() {
        return POTTED_PLANTS;
    }

    /**
     * Returns true when the requested item can be obtained by directly
     * mining a block under the configured tool assumptions.
     *
     * Explicit MiningData entries always win.
     *
     * For blocks without a special drop entry, a registered block whose
     * block item has the same ID is treated as self-dropping.
     */
    public static boolean isDirectlyMineable(String itemName, boolean silkTouch, boolean shears) {
        MiningDrop drop = DROPS.get(itemName);
        if (drop != null) {
            String result = silkTouch ? drop.getSilkTouchDrop() : drop.getNormalDrop();
            if (result != null) {
                return result.equals(itemName);
            }

            if (shears && drop.shearsAlsoWork() && drop.getSilkTouchDrop() != null) {
                return drop.getSilkTouchDrop().equals(itemName);
            }
            return false;
        }

        Identifier id = toIdentifier(itemName);
        if (id == null) {
            return false;
        }

        Block block = Registries.BLOCK.get(id);
        if (block == null) {
            return false;
        }

        Identifier blockItemId = Registries.ITEM.getId(block.asItem());
        return id.equals(blockItemId);
    }

    private static Identifier toIdentifier(String name) {
        try {
            if (name.contains(":")) {
                return new Identifier(name);
            }
            return new Identifier("minecraft", name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LoadedData load() {
        try (InputStream input = MiningData.class.getResourceAsStream(RESOURCE_PATH)) {
            /*
             * Do not crash the whole mod if the optional static data file
             * hasn't been installed yet.
             */
            if (input == null) {
                return new LoadedData(new LinkedHashMap<>(), new LinkedHashMap<>());
            }

            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    throw new IllegalStateException("Mining data root must be a JSON object");
                }

                JsonObject root = element.getAsJsonObject();
                return new LoadedData(loadDrops(root), loadPottedPlants(root));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mining data: " + RESOURCE_PATH, e);
        } catch (RuntimeException e) {
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
                throw new IllegalStateException("Mining drop for '" + blockName + "' must be an object");
            }

            JsonObject object = value.getAsJsonObject();
            String normalDrop = getNullableString(object, "normal_drop");
            int normalCount = getInt(object, "normal_count", 1);
            String silkDrop = getNullableString(object, "silk_touch_drop");
            int silkCount = getInt(object, "silk_touch_count", 1);
            boolean shears = getBoolean(object, "shears_also_work", false);
            result.put(blockName, new MiningDrop(normalDrop, normalCount, silkDrop, silkCount, shears));
        }
        return result;
    }

    private static Map<String, String>
    loadPottedPlants(JsonObject root) {
        Map<String, String> result = new LinkedHashMap<>();
        JsonElement element = root.get("potted_plants");
        if (element == null || element.isJsonNull()) {
            return result;
        }

        if (!element.isJsonObject()) {
            throw new IllegalStateException("'potted_plants' must be a JSON object");
        }

        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
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
        return element.getAsString();
    }

    private static int getInt(JsonObject object, String key, int defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        return element.getAsInt();
    }

    private static boolean getBoolean(JsonObject object, String key, boolean defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        return element.getAsBoolean();
    }

    private record LoadedData(Map<String, MiningDrop> drops, Map<String, String> pottedPlants) {
    }
}