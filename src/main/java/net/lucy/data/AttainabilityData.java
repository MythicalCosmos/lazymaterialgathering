package net.lucy.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lucy.model.AttainabilityType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the small set of special attainability exceptions from
 * attainability.json.
 *
 * Items not present in the file are considered OBTAINABLE.
 */
public final class AttainabilityData {
    private static final String RESOURCE_PATH = "/data/lazymaterialgathering/attainability.json";
    private static final Map<String, AttainabilityType> DATA = load();
    private AttainabilityData() {
    }

    /**
     * Returns the explicitly configured attainability type.
     *
     * Items not present in the data file default to OBTAINABLE.
     */
    public static AttainabilityType get(String itemName) {
        return DATA.getOrDefault(itemName, AttainabilityType.OBTAINABLE);
    }

    /**
     * Returns all explicitly-defined attainability exceptions.
     */
    public static Map<String, AttainabilityType> getAll() {
        return DATA;
    }

    private static Map<String, AttainabilityType> load() {
        try (InputStream input = AttainabilityData.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                throw new IllegalStateException("Missing attainability data resource: " + RESOURCE_PATH);
            }

            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    throw new IllegalStateException("Attainability data root must be a JSON object");
                }

                JsonObject root = element.getAsJsonObject();
                Map<String, AttainabilityType> result = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    if (!entry.getValue().isJsonPrimitive()) {
                        throw new IllegalStateException("Attainability value for '" + entry.getKey() + "' must be a string");
                    }

                    String value = entry.getValue().getAsString();
                    try {
                        result.put(entry.getKey(), AttainabilityType.valueOf(value));
                    }
                    catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Unknown attainability type '" + value + "' for item '" + entry.getKey() + "'", e);
                    }
                }
                return Collections.unmodifiableMap(result);
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load attainability data: " + RESOURCE_PATH, e);
        }
    }
}