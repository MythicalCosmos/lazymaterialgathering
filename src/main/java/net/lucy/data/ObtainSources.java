package net.lucy.data;

import net.lucy.model.ObtainSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ObtainSources {

    public static final Map<String, Set<ObtainSource>> sources = new HashMap<>();

    static {
        // Stone
        sources.put("stone", Set.of(
                new ObtainSource(
                        ObtainSource.Type.MINED,
                        "SILK_TOUCH"
                ),
                new ObtainSource(
                        ObtainSource.Type.SMELTED
                )
        ));

        // Charcoal
        sources.put("charcoal", Set.of(
                new ObtainSource(
                        ObtainSource.Type.NATURAL
                ),
                new ObtainSource(
                        ObtainSource.Type.SMELTED
                )
        ));

        // Diamond
        sources.put("diamond", Set.of(
                new ObtainSource(
                        ObtainSource.Type.MINED
                )
        ));
    }

    public static boolean canObtainVia(
            String item,
            ObtainSource.Type type
    ) {
        Set<ObtainSource> itemSources =
                sources.get(item.toLowerCase());

        if (itemSources == null) {
            return false;
        }

        return itemSources.stream()
                .anyMatch(source -> source.getType() == type);
    }

    public static Set<ObtainSource> getSources(String item) {
        return sources.getOrDefault(
                item.toLowerCase(),
                Set.of()
        );
    }
}