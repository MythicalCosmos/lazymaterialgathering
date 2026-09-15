package net.lucy.calc;

import net.lucy.data.ItemAttainability;
import net.lucy.model.AttainabilityType;

public class AttainabilityClassifier {

    public static AttainabilityType getAttainability(String itemName) {
        return ItemAttainability.attainability.getOrDefault(itemName, AttainabilityType.OBTAINABLE);
    }

    public static boolean isSurvivalObtainable(String itemName) {
        AttainabilityType type = getAttainability(itemName);
        return type == AttainabilityType.OBTAINABLE || type == AttainabilityType.SPECIAL_METHOD;
    }
}