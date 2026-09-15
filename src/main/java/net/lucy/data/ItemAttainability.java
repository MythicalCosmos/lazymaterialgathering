package net.lucy.data;

import net.lucy.model.AttainabilityType;
import java.util.HashMap;
import java.util.Map;

public class ItemAttainability {
    public static Map<String, AttainabilityType> attainability = new HashMap<>();

    static {
        attainability.put("barrier", AttainabilityType.UNOBTAINABLE);
        attainability.put("structure_void", AttainabilityType.UNOBTAINABLE);
        attainability.put("light", AttainabilityType.UNOBTAINABLE);
        attainability.put("impluse_command_block", AttainabilityType.UNOBTAINABLE);
        attainability.put("repeating_command_block", AttainabilityType.UNOBTAINABLE);
        attainability.put("chain_command_block", AttainabilityType.UNOBTAINABLE);
        attainability.put("structure_block", AttainabilityType.UNOBTAINABLE);
        attainability.put("jigsaw_block", AttainabilityType.UNOBTAINABLE);
        attainability.put("petrified_oak_slab", AttainabilityType.UNOBTAINABLE);

        attainability.put("bedrock", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("reinforced_deepslate", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("budding_amethyst", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("end_portal_frame", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("spawner", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("infested_stone", AttainabilityType.ENCOUNTERABLE_ONLY);

        attainability.put("player_head", AttainabilityType.SPECIAL_METHOD);
        attainability.put("wither_skeleton_skull", AttainabilityType.SPECIAL_METHOD);
        attainability.put("dragon_head", AttainabilityType.SPECIAL_METHOD);
    }
}