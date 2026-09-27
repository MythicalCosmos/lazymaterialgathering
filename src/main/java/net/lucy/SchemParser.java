package net.lucy;

import net.lucy.calc.AttainabilityClassifier;
import net.lucy.calc.MiningResolver;
import net.lucy.calc.RawMaterials;
import net.lucy.config.Configs;
import net.lucy.data.DataManager;
import net.lucy.data.ItemEnchantRequirements;
import net.lucy.model.AttainabilityType;
import net.minecraft.enchantment.SilkTouchEnchantment;
import net.sandrohc.schematic4j.schematic.Schematic;
import net.sandrohc.schematic4j.schematic.types.SchematicBlockEntity;
import net.sandrohc.schematic4j.schematic.types.SchematicEntity;
import net.lucy.data.Recipes;
import net.lucy.data.CropAges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SchemParser {

    public static Map<String, Long> blockCounts;
    public static String[] blocks;
    public static String attainabilityReportText;
    public static String rawMaterialsText;
    public static String blockEntitiesText;
    public static String entitiesText;

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
                    String name = pair.right.block.replace("minecraft:", "");

                    // A young wheat/beetroot/torchflower plant gives you the seed, not
                    // the grown crop -- count it as that instead, when we can tell its age.
                    String immatureDrop = CropAges.getImmatureDrop(name, pair.right.states);
                    if (immatureDrop != null) {
                        return immatureDrop;
                    }

                    return remappables.getOrDefault(name, name);
                })
                .collect(Collectors.groupingBy(text -> text, TreeMap::new, Collectors.counting()));

        attainabilityReportText = buildAttainabilityReport(blockCounts);

        blocks = blockCounts.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .toArray(String[]::new);

        blockEntitiesText = schematic.blockEntities().map(SchematicBlockEntity::toString).collect(Collectors.joining("\n"));
        entitiesText = schematic.entities().map(SchematicEntity::toString).collect(Collectors.joining("\n"));

        Map<String, Long> minedItems = MiningResolver.resolveMinedItems(blockCounts, Configs.Generic.USE_SILK_TOUCH.getBooleanValue());
        RawMaterials.Result result = RawMaterials.calculateDetailed(minedItems);
        DataManager.setResults(blockCounts, result);

        rawMaterialsText = result.totals.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));

        writeToFile("blocks.txt", String.join("\n", blocks));
        writeToFile("blockentities.txt", blockEntitiesText);
        writeToFile("entities.txt", entitiesText);
        writeToFile("attainability_report.txt", attainabilityReportText);
        writeToFile("raw_materials.txt", rawMaterialsText);
    }

    private static String buildAttainabilityReport(Map<String, Long> counts) {
        Map<AttainabilityType, Map<String, Long>> byAttainability = new EnumMap<>(AttainabilityType.class);

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            AttainabilityType type = AttainabilityClassifier.getAttainability(entry.getKey());
            byAttainability.computeIfAbsent(type, t -> new TreeMap<>()).put(entry.getKey(), entry.getValue());
        }

        StringBuilder report = new StringBuilder();
        for (Map.Entry<AttainabilityType, Map<String, Long>> categoryEntry : byAttainability.entrySet()) {
            report.append("--- ").append(categoryEntry.getKey()).append(" ---\n");
            categoryEntry.getValue().entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(itemEntry -> report.append("  ").append(itemEntry.getKey())
                            .append(": ").append(itemEntry.getValue()).append("\n"));
        }
        return report.toString();
    }

    private static void writeToFile(String filename, String content) {
        try {
            String directory = Configs.Generic.OUTPUT_DIRECTORY.getStringValue();
            Path outputDir = directory.isBlank()
                    ? Path.of(".")
                    : Path.of(directory);
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve(filename), content);
            System.out.println("Successfully saved " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}