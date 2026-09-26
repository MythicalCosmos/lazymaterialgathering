package net.lucy.gui;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import fi.dy.masa.malilib.config.IConfigBase;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds every one of Baritone's own settings by reflection and wraps each one for display,
 * instead of a hand-picked subset. Baritone doesn't expose its settings' real descriptions
 * at runtime (those only exist as comments in its own source), so each one is labelled
 * with its setting name only -- turned into something readable, like "allowParkourPlace"
 * -> "Allow Parkour Place" -- not a real explanation of what it does. For the handful you
 * use often, it's worth checking Baritone's own settings.md/wiki once and knowing what
 * they do; this screen's job is coverage, not documentation.
 *
 * Settings are bucketed into tabs by simple keyword matching on the field name, since that
 * grouping isn't available at runtime either -- treat the tabs as a rough sort, not
 * Baritone's own categorisation.
 *
 * A few of Baritone's setting types (colors, the "acceptableThrowawayItems" block list, and
 * a couple of others with no plain number/text/toggle form) aren't shown here at all --
 * they don't have a simple widget to edit them with. Boolean, integer, double, float, long
 * and String settings are all covered.
 */
public class BaritoneSettingsRegistry
{
    private static Map<String, List<IConfigBase>> tabs;

    public static List<String> getTabNames()
    {
        ensureBuilt();
        return new ArrayList<>(tabs.keySet());
    }

    public static List<IConfigBase> getSettingsForTab(String tabName)
    {
        ensureBuilt();
        return tabs.getOrDefault(tabName, List.of());
    }

    private static void ensureBuilt()
    {
        if (tabs != null)
        {
            return;
        }

        tabs = new LinkedHashMap<>();
        // Fixed order, so the tab list doesn't reshuffle between sessions
        for (String tabName : new String[] { "Movement", "Mining & Building", "Pathing Cost", "Rendering", "Other" })
        {
            tabs.put(tabName, new ArrayList<>());
        }

        Settings settings = BaritoneAPI.getSettings();

        for (Field field : Settings.class.getFields())
        {
            if (Settings.Setting.class.isAssignableFrom(field.getType()) == false)
            {
                continue;
            }

            try
            {
                Settings.Setting<?> setting = (Settings.Setting<?>) field.get(settings);
                IConfigBase wrapper = wrap(field.getName(), setting);

                if (wrapper != null)
                {
                    tabs.get(categorize(field.getName())).add(wrapper);
                }
            }
            catch (IllegalAccessException | ClassCastException ignored)
            {
                // Not a setting we can read this way; skip it rather than crash the whole screen
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static IConfigBase wrap(String commandName, Settings.Setting<?> setting)
    {
        String displayName = toDisplayName(commandName);
        String comment = "Baritone's own \"" + commandName + "\" setting.";
        Object value = setting.value;

        if (value instanceof Boolean)
        {
            return new BaritoneSettingWrapper.BooleanSetting(displayName, commandName, comment, (Settings.Setting<Boolean>) setting);
        }
        if (value instanceof Integer)
        {
            return new BaritoneSettingWrapper.IntegerSetting(displayName, commandName, comment, (Settings.Setting<Integer>) setting, -1000, 1000);
        }
        if (value instanceof Double)
        {
            return new BaritoneSettingWrapper.DoubleSetting(displayName, commandName, comment, (Settings.Setting<Double>) setting, -100.0, 100.0);
        }
        if (value instanceof Float)
        {
            return new BaritoneSettingWrapper.FloatSetting(displayName, commandName, comment, (Settings.Setting<Float>) setting, -100.0, 100.0);
        }
        if (value instanceof Long)
        {
            return new BaritoneSettingWrapper.LongSetting(displayName, commandName, comment, (Settings.Setting<Long>) setting);
        }
        if (value instanceof String)
        {
            return new BaritoneSettingWrapper.StringSetting(displayName, commandName, comment, (Settings.Setting<String>) setting);
        }

        return null; // Color, Vec3i, Rotation, Mirror, lists, ... -- no plain widget for these
    }

    // Rough, name-based sort. Genuinely just keyword matching -- Baritone's own grouping
    // (if it has one) isn't something this reflection approach can see.
    private static String categorize(String fieldName)
    {
        String lower = fieldName.toLowerCase();

        if (lower.contains("render") || lower.contains("color") || lower.contains("path") && lower.contains("render"))
        {
            return "Rendering";
        }
        if (lower.contains("penalty") || lower.contains("cost") || lower.contains("weight") || lower.contains("favor"))
        {
            return "Pathing Cost";
        }
        if (lower.contains("break") || lower.contains("mine") || lower.contains("place") || lower.contains("build")
                || lower.contains("ore") || lower.contains("scaffold") || lower.contains("inventory"))
        {
            return "Mining & Building";
        }
        if (lower.contains("sprint") || lower.contains("jump") || lower.contains("walk") || lower.contains("sneak")
                || lower.contains("parkour") || lower.contains("diagonal") || lower.contains("ascend") || lower.contains("descend")
                || lower.contains("swim") || lower.contains("climb"))
        {
            return "Movement";
        }

        return "Other";
    }

    /** Readable label for a setting name: "allowParkourPlace" -> "Allow Parkour Place". */
    public static String toDisplayName(String fieldName)
    {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < fieldName.length(); i++)
        {
            char c = fieldName.charAt(i);

            if (i == 0)
            {
                result.append(Character.toUpperCase(c));
            }
            else if (Character.isUpperCase(c))
            {
                result.append(' ').append(c);
            }
            else
            {
                result.append(c);
            }
        }

        return result.toString();
    }
}