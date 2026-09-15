package net.lucy.calc;

import net.lucy.data.ObtainMethods;
import net.lucy.model.ObtainCategory;
import net.lucy.model.ObtainMethod;

import java.util.List;
import java.util.Map;

public class ObtainMethodReport {

    public static String listByCategory(ObtainCategory category) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<ObtainMethod>> entry : ObtainMethods.methods.entrySet()) {
            for (ObtainMethod method : entry.getValue()) {
                if (method.category == category) {
                    sb.append(entry.getKey())
                            .append(" - ").append(method.description)
                            .append("\n");
                }
            }
        }
        return sb.toString();
    }
}