package net.lucy.data;

import net.lucy.model.SourceType;
import java.util.HashMap;
import java.util.Map;

public class ItemSources {
    public static Map<String, SourceType> sources = new HashMap<>();

    static {
        sources.put("cobblestone", SourceType.MINED);
        sources.put("raw_iron", SourceType.MINED);
        sources.put("coal", SourceType.MINED);
        sources.put("redstone", SourceType.MINED);
        sources.put("diamond", SourceType.MINED);
        sources.put("string", SourceType.MOB_DROP);
        sources.put("feather", SourceType.MOB_DROP);
        sources.put("gunpowder", SourceType.MOB_DROP);
        sources.put("bone", SourceType.MOB_DROP);
        sources.put("wheat", SourceType.FARMED);
        sources.put("oak_log", SourceType.NATURAL);
        sources.put("sand", SourceType.NATURAL);
        sources.put("dirt", SourceType.NATURAL);
    }
}