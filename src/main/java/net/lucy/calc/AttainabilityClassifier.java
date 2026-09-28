package net.lucy.calc;

import net.lucy.data.AttainabilityData;
import net.lucy.model.AttainabilityType;

/**
 * Determines whether an item can be obtained in Survival.
 *
 * The actual exception data lives in AttainabilityData.
 * This class contains the classification rules.
 */
public final class AttainabilityClassifier {
    private AttainabilityClassifier() {
    }

    public static AttainabilityType getAttainability(String itemName) {
        return AttainabilityData.get(itemName);
    }

    public static boolean isSurvivalObtainable(String itemName) {
        AttainabilityType type = getAttainability(itemName);
        return type == AttainabilityType.OBTAINABLE || type == AttainabilityType.SPECIAL_METHOD;
    }
}