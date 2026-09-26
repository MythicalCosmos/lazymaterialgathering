package net.lucy.gui;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IConfigString;

/**
 * Lets a Baritone setting be shown on a malilib config screen.
 * Baritone keeps its settings as public Settings.Setting<T> fields (not malilib config
 * objects), so this class reads .value directly for display, but every WRITE goes through
 * Baritone's own "set <name> <value>" command (via ICommandManager.execute), the exact
 * same thing typing "#set <name> <value>" in chat would do -- rather than poking the field
 * directly -- so anything Baritone itself does when a setting changes (like its own chat
 * confirmation) still happens.
 */
public abstract class BaritoneSettingWrapper implements IConfigBase
{
    protected final String displayName;   // shown on the screen, e.g. "Allow Parkour Place"
    protected final String commandName;   // Baritone's real setting name, e.g. "allowParkourPlace"
    protected final String comment;

    protected BaritoneSettingWrapper(String displayName, String commandName, String comment)
    {
        this.displayName = displayName;
        this.commandName = commandName;
        this.comment = comment;
    }

    @Override
    public String getName()
    {
        return this.displayName;
    }

    @Override
    public String getComment()
    {
        return this.comment;
    }

    /** Runs Baritone's own "set <name> <value>" command, exactly as if typed in chat. */
    protected void sendSetCommand(Object value)
    {
        BaritoneAPI.getProvider().getPrimaryBaritone()
                .getCommandManager()
                .execute("set " + this.commandName + " " + value);
    }

    // ---------- Boolean settings (allowParkour, allowBreak, ...) ----------

    public static class BooleanSetting extends BaritoneSettingWrapper implements IConfigBoolean
    {
        private final Settings.Setting<Boolean> setting;

        public BooleanSetting(String displayName, String commandName, String comment, Settings.Setting<Boolean> setting)
        {
            super(displayName, commandName, comment);
            this.setting = setting;
        }

        @Override
        public ConfigType getType() { return ConfigType.BOOLEAN; }

        @Override
        public boolean getBooleanValue() { return this.setting.value; }

        @Override
        public boolean getDefaultBooleanValue() { return this.setting.defaultValue; }

        @Override
        public void setBooleanValue(boolean value) { this.sendSetCommand(value); }

        @Override
        public boolean isModified() { return this.setting.value != this.setting.defaultValue; }

        @Override
        public void resetToDefault() { this.sendSetCommand(this.setting.defaultValue); }

        @Override
        public String getStringValue() { return String.valueOf(this.setting.value); }

        @Override
        public String getDefaultStringValue() { return String.valueOf(this.setting.defaultValue); }

        @Override
        public void setValueFromString(String value) { this.sendSetCommand(Boolean.parseBoolean(value)); }

        @Override
        public boolean isModified(String newValue) { return Boolean.parseBoolean(newValue) != this.setting.defaultValue; }

        @Override
        public void setValueFromJsonElement(JsonElement element) { this.sendSetCommand(element.getAsBoolean()); }

        @Override
        public JsonElement getAsJsonElement() { return new JsonPrimitive(this.setting.value); }
    }

    // ---------- Integer settings (blockBreakSpeed, ...) ----------

    public static class IntegerSetting extends BaritoneSettingWrapper implements IConfigInteger
    {
        private final Settings.Setting<Integer> setting;
        private final int min;
        private final int max;

        public IntegerSetting(String displayName, String commandName, String comment, Settings.Setting<Integer> setting, int min, int max)
        {
            super(displayName, commandName, comment);
            this.setting = setting;
            this.min = min;
            this.max = max;
        }

        @Override
        public ConfigType getType() { return ConfigType.INTEGER; }

        @Override
        public int getIntegerValue() { return this.setting.value; }

        @Override
        public int getDefaultIntegerValue() { return this.setting.defaultValue; }

        @Override
        public void setIntegerValue(int value) { this.sendSetCommand(value); }

        @Override
        public int getMinIntegerValue() { return this.min; }

        @Override
        public int getMaxIntegerValue() { return this.max; }

        @Override
        public boolean shouldUseSlider() { return true; }

        @Override
        public boolean isModified() { return !this.setting.value.equals(this.setting.defaultValue); }

        @Override
        public void resetToDefault() { this.sendSetCommand(this.setting.defaultValue); }

        @Override
        public String getStringValue() { return String.valueOf(this.setting.value); }

        @Override
        public String getDefaultStringValue() { return String.valueOf(this.setting.defaultValue); }

        @Override
        public void setValueFromString(String value) { this.sendSetCommand(Integer.parseInt(value)); }

        @Override
        public boolean isModified(String newValue) { return Integer.parseInt(newValue) != this.setting.defaultValue; }

        @Override
        public void setValueFromJsonElement(JsonElement element) { this.sendSetCommand(element.getAsInt()); }

        @Override
        public JsonElement getAsJsonElement() { return new JsonPrimitive(this.setting.value); }
    }

    // ---------- Double settings (blockPlacementPenalty, ...) ----------

    public static class DoubleSetting extends BaritoneSettingWrapper implements IConfigDouble
    {
        private final Settings.Setting<Double> setting;
        private final double min;
        private final double max;

        public DoubleSetting(String displayName, String commandName, String comment, Settings.Setting<Double> setting, double min, double max)
        {
            super(displayName, commandName, comment);
            this.setting = setting;
            this.min = min;
            this.max = max;
        }

        @Override
        public ConfigType getType() { return ConfigType.DOUBLE; }

        @Override
        public double getDoubleValue() { return this.setting.value; }

        @Override
        public double getDefaultDoubleValue() { return this.setting.defaultValue; }

        @Override
        public void setDoubleValue(double value) { this.sendSetCommand(value); }

        @Override
        public double getMinDoubleValue() { return this.min; }

        @Override
        public double getMaxDoubleValue() { return this.max; }

        @Override
        public boolean shouldUseSlider() { return true; }

        @Override
        public boolean isModified() { return !this.setting.value.equals(this.setting.defaultValue); }

        @Override
        public void resetToDefault() { this.sendSetCommand(this.setting.defaultValue); }

        @Override
        public String getStringValue() { return String.valueOf(this.setting.value); }

        @Override
        public String getDefaultStringValue() { return String.valueOf(this.setting.defaultValue); }

        @Override
        public void setValueFromString(String value) { this.sendSetCommand(Double.parseDouble(value)); }

        @Override
        public boolean isModified(String newValue) { return Double.parseDouble(newValue) != this.setting.defaultValue; }

        @Override
        public void setValueFromJsonElement(JsonElement element) { this.sendSetCommand(element.getAsDouble()); }

        @Override
        public JsonElement getAsJsonElement() { return new JsonPrimitive(this.setting.value); }
    }

    // ---------- Float settings (stored internally as Double so malilib can render them) ----------

    public static class FloatSetting extends BaritoneSettingWrapper implements IConfigDouble
    {
        private final Settings.Setting<Float> setting;
        private final double min;
        private final double max;

        public FloatSetting(String displayName, String commandName, String comment, Settings.Setting<Float> setting, double min, double max)
        {
            super(displayName, commandName, comment);
            this.setting = setting;
            this.min = min;
            this.max = max;
        }

        @Override
        public ConfigType getType() { return ConfigType.DOUBLE; }

        @Override
        public double getDoubleValue() { return this.setting.value; }

        @Override
        public double getDefaultDoubleValue() { return this.setting.defaultValue; }

        @Override
        public void setDoubleValue(double value) { this.sendSetCommand((float) value); }

        @Override
        public double getMinDoubleValue() { return this.min; }

        @Override
        public double getMaxDoubleValue() { return this.max; }

        @Override
        public boolean shouldUseSlider() { return true; }

        @Override
        public boolean isModified() { return !this.setting.value.equals(this.setting.defaultValue); }

        @Override
        public void resetToDefault() { this.sendSetCommand(this.setting.defaultValue); }

        @Override
        public String getStringValue() { return String.valueOf(this.setting.value); }

        @Override
        public String getDefaultStringValue() { return String.valueOf(this.setting.defaultValue); }

        @Override
        public void setValueFromString(String value) { this.sendSetCommand(Float.parseFloat(value)); }

        @Override
        public boolean isModified(String newValue) { return Float.parseFloat(newValue) != this.setting.defaultValue; }

        @Override
        public void setValueFromJsonElement(JsonElement element) { this.sendSetCommand(element.getAsFloat()); }

        @Override
        public JsonElement getAsJsonElement() { return new JsonPrimitive(this.setting.value); }
    }

    // ---------- Long settings (stored internally as Integer; values must fit in an int) ----------

    public static class LongSetting extends BaritoneSettingWrapper implements IConfigInteger
    {
        private final Settings.Setting<Long> setting;

        public LongSetting(String displayName, String commandName, String comment, Settings.Setting<Long> setting)
        {
            super(displayName, commandName, comment);
            this.setting = setting;
        }

        @Override
        public ConfigType getType() { return ConfigType.INTEGER; }

        @Override
        public int getIntegerValue() { return (int) (long) this.setting.value; }

        @Override
        public int getDefaultIntegerValue() { return (int) (long) this.setting.defaultValue; }

        @Override
        public void setIntegerValue(int value) { this.sendSetCommand((long) value); }

        @Override
        public int getMinIntegerValue() { return 0; }

        @Override
        public int getMaxIntegerValue() { return Integer.MAX_VALUE; }

        @Override
        public boolean shouldUseSlider() { return false; }

        @Override
        public boolean isModified() { return !this.setting.value.equals(this.setting.defaultValue); }

        @Override
        public void resetToDefault() { this.sendSetCommand(this.setting.defaultValue); }

        @Override
        public String getStringValue() { return String.valueOf(this.setting.value); }

        @Override
        public String getDefaultStringValue() { return String.valueOf(this.setting.defaultValue); }

        @Override
        public void setValueFromString(String value) { this.sendSetCommand(Long.parseLong(value)); }

        @Override
        public boolean isModified(String newValue) { return Long.parseLong(newValue) != this.setting.defaultValue; }

        @Override
        public void setValueFromJsonElement(JsonElement element) { this.sendSetCommand(element.getAsLong()); }

        @Override
        public JsonElement getAsJsonElement() { return new JsonPrimitive(this.setting.value); }
    }

    // ---------- String settings ----------

    public static class StringSetting extends BaritoneSettingWrapper implements IConfigString
    {
        private final Settings.Setting<String> setting;

        public StringSetting(String displayName, String commandName, String comment, Settings.Setting<String> setting)
        {
            super(displayName, commandName, comment);
            this.setting = setting;
        }

        @Override
        public ConfigType getType() { return ConfigType.STRING; }

        @Override
        public String getStringValue() { return this.setting.value; }

        @Override
        public String getDefaultStringValue() { return this.setting.defaultValue; }

        @Override
        public void setValueFromString(String value) { this.sendSetCommand(value); }

        @Override
        public boolean isModified() { return !this.setting.value.equals(this.setting.defaultValue); }

        @Override
        public boolean isModified(String newValue) { return !newValue.equals(this.setting.defaultValue); }

        @Override
        public void resetToDefault() { this.sendSetCommand(this.setting.defaultValue); }

        @Override
        public void setValueFromJsonElement(JsonElement element) { this.sendSetCommand(element.getAsString()); }

        @Override
        public JsonElement getAsJsonElement() { return new JsonPrimitive(this.setting.value); }
    }
}