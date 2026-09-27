package net.lucy.data;

import net.lucy.model.AttainabilityType;
import java.util.HashMap;
import java.util.Map;

public class ItemAttainability {
    public static Map<String, AttainabilityType> attainability = new HashMap<>();

    static {
        // Command/structure/testing blocks: creative-only, no loot table at all
        attainability.put("command_block", AttainabilityType.UNOBTAINABLE);
        attainability.put("chain_command_block", AttainabilityType.UNOBTAINABLE);
        attainability.put("repeating_command_block", AttainabilityType.UNOBTAINABLE);
        attainability.put("structure_block", AttainabilityType.UNOBTAINABLE);
        attainability.put("structure_void", AttainabilityType.UNOBTAINABLE);
        attainability.put("jigsaw", AttainabilityType.UNOBTAINABLE);
        attainability.put("barrier", AttainabilityType.UNOBTAINABLE);
        attainability.put("light", AttainabilityType.UNOBTAINABLE);

        // Exist only as part of a bigger mechanism; the game never lets these be mined as
        // themselves (confirmed against 1.20.1's own loot table data: none of these have one)
        attainability.put("piston_head", AttainabilityType.UNOBTAINABLE);
        attainability.put("moving_piston", AttainabilityType.UNOBTAINABLE);
        attainability.put("end_portal", AttainabilityType.UNOBTAINABLE);
        attainability.put("end_gateway", AttainabilityType.UNOBTAINABLE);
        attainability.put("nether_portal", AttainabilityType.UNOBTAINABLE);
        attainability.put("bubble_column", AttainabilityType.UNOBTAINABLE);

        // Fluids and fire
        attainability.put("water", AttainabilityType.UNOBTAINABLE);
        attainability.put("lava", AttainabilityType.UNOBTAINABLE);
        attainability.put("fire", AttainabilityType.UNOBTAINABLE);
        attainability.put("soul_fire", AttainabilityType.UNOBTAINABLE);

        // The "hidden" air-like blocks a schematic can technically contain
        attainability.put("cave_air", AttainabilityType.UNOBTAINABLE);
        attainability.put("void_air", AttainabilityType.UNOBTAINABLE);

        // Can only be found in the world, or need a special method (a mob head from a
        // specific mob, bedrock from creative/void edges, etc.)
        attainability.put("bedrock", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("reinforced_deepslate", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("budding_amethyst", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("end_portal_frame", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("spawner", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("infested_stone", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("infested_cobblestone", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("infested_stone_bricks", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("infested_mossy_stone_bricks", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("infested_cracked_stone_bricks", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("infested_chiseled_stone_bricks", AttainabilityType.ENCOUNTERABLE_ONLY);
        attainability.put("infested_deepslate", AttainabilityType.ENCOUNTERABLE_ONLY);

        attainability.put("player_head", AttainabilityType.SPECIAL_METHOD);
        attainability.put("wither_skeleton_skull", AttainabilityType.SPECIAL_METHOD);
        attainability.put("dragon_head", AttainabilityType.SPECIAL_METHOD);
    }
}