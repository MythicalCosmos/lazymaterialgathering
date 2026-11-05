package net.lucy;

import net.lucy.calc.AttainabilityClassifier;
import net.lucy.calc.MiningResolver;
import net.lucy.calc.RawMaterials;
import net.lucy.config.Configs;
import net.lucy.data.CalculationData;
import net.sandrohc.schematic4j.schematic.Schematic;
import net.lucy.data.Recipes;
import net.lucy.data.CropAges;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SchemParser {
    public static Map<String, Long> blockCounts;
    public static String[] blocks;
    public static void parse(Schematic schematic) {
        Recipes.ensureLoaded();
        HashMap<String, String> remappables = new HashMap<>();
        remappables.put("dirt_path", "dirt");
        remappables.put("farmland", "dirt");
        remappables.put("chorus_plant", "chorus_flower");
        blockCounts = schematic.blocks()
                .filter(pair -> !pair.right.name.equals("minecraft:air"))
                .filter(pair -> AttainabilityClassifier.isSurvivalObtainable(pair.right.block.replace("minecraft:", "")))
                .filter(pair -> {
                    String levelStr = pair.right.states.get("level");
                    if (levelStr == null) return true;
                    return Integer.parseInt(levelStr) >= 8;
                })
                .map(pair -> {
                    String name = pair.right.block;
                    String immatureDrop = CropAges.getImmatureDrop(name, pair.right.states);
                    if (immatureDrop != null) {
                        return immatureDrop;
                    }

                    return remappables.getOrDefault(name, name);
                })
                .collect(Collectors.groupingBy(text -> text, TreeMap::new, Collectors.counting()));
        blocks = blockCounts.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).toArray(String[]::new);
        Map<String, Long> minedItems = MiningResolver.resolveMinedItems(blockCounts, Configs.Generic.USE_SILK_TOUCH.getBooleanValue());
        RawMaterials.Result result = RawMaterials.calculateDetailed(minedItems);
        CalculationData.setResults(blockCounts, result);
    }
}