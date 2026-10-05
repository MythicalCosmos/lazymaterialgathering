package net.lucy.calc;

import net.lucy.config.Configs;

import java.util.List;

public final class MiningSourceResolver {

    private MiningSourceResolver() {
    }

    public static List<String> getPossibleSources(String item) {
        item = normalize(item);

        return switch (item) {

            /*
             * Stone without Silk Touch drops cobblestone.
             */
            case "minecraft:cobblestone" ->
                    Configs.Generic.USE_SILK_TOUCH.getBooleanValue()
                            ? List.of()
                            : List.of("minecraft:stone");

            case "minecraft:stone" ->
                    List.of("minecraft:stone");

            case "minecraft:deepslate" ->
                    List.of("minecraft:deepslate");

            case "minecraft:coal" ->
                    List.of(
                            "minecraft:coal_ore",
                            "minecraft:deepslate_coal_ore"
                    );

            case "minecraft:raw_iron",
                 "minecraft:iron_ore" ->
                    List.of(
                            "minecraft:iron_ore",
                            "minecraft:deepslate_iron_ore"
                    );

            case "minecraft:raw_gold",
                 "minecraft:gold_ore" ->
                    List.of(
                            "minecraft:gold_ore",
                            "minecraft:deepslate_gold_ore"
                    );

            case "minecraft:raw_copper",
                 "minecraft:copper_ore" ->
                    List.of(
                            "minecraft:copper_ore",
                            "minecraft:deepslate_copper_ore"
                    );

            case "minecraft:diamond" ->
                    List.of(
                            "minecraft:diamond_ore",
                            "minecraft:deepslate_diamond_ore"
                    );

            case "minecraft:emerald" ->
                    List.of(
                            "minecraft:emerald_ore",
                            "minecraft:deepslate_emerald_ore"
                    );

            case "minecraft:lapis_lazuli" ->
                    List.of(
                            "minecraft:lapis_ore",
                            "minecraft:deepslate_lapis_ore"
                    );

            case "minecraft:redstone" ->
                    List.of(
                            "minecraft:redstone_ore",
                            "minecraft:deepslate_redstone_ore"
                    );

            case "minecraft:quartz" ->
                    List.of("minecraft:nether_quartz_ore");

            case "minecraft:netherite_scrap" ->
                    List.of("minecraft:ancient_debris");

            default ->
                    List.of(item);
        };
    }

    public static String preferredSource(String item) {
        List<String> sources = getPossibleSources(item);

        if (sources.isEmpty()) {
            return null;
        }

        return sources.get(0);
    }

    private static String normalize(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }

        return id.startsWith("minecraft:")
                ? id
                : "minecraft:" + id;
    }
}