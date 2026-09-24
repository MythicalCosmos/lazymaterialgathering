package net.lucy.gui;

import baritone.api.Settings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigInteger;

/**
 * Lets a Baritone setting be shown on a malilib config screen.
 * Baritone keeps its settings as public Settings.Setting<T> fields (not malilib config objects),
 * so this class reads and writes .value on the given field directly. There's one subclass per
 * value type, since malilib's config screen decides which widget to draw from which interface
 * (IConfigBoolean, IConfigInteger, IConfigDouble) a config implements.
 */
public abstract class BaritoneSettingWrapper implements IConfigBase
{
    protected final String name;
    protected final String comment;

    protected BaritoneSettingWrapper(String name, String comment)
    {
        this.name = name;
        this.comment = comment;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getComment()
    {
        return this.comment;
    }

    // ---------- Boolean settings (allowParkour, allowBreak, ...) ----------

    public static class BooleanSetting extends BaritoneSettingWrapper implements IConfigBoolean
    {
        private final Settings.Setting<Boolean> setting;

        public BooleanSetting(String name, String comment, Settings.Setting<Boolean> setting)
        {
            super(name, comment);
            this.setting = setting;
        }

        @Override
        public ConfigType getType() { return ConfigType.BOOLEAN; }

        @Override
        public boolean getBooleanValue() { return this.setting.value; }

        @Override
        public boolean getDefaultBooleanValue() { return this.setting.defaultValue; }

        @Override
        public void setBooleanValue(boolean value) { this.setting.value = value; }

        @Override
        public boolean isModified() { return this.setting.value != this.setting.defaultValue; }

        @Override
        public void resetToDefault() { this.setting.value = this.setting.defaultValue; }

        @Override
        public String getStringValue() { return String.valueOf(this.setting.value); }

        @Override
        public String getDefaultStringValue() { return String.valueOf(this.setting.defaultValue); }

        @Override
        public void setValueFromString(String value) { this.setting.value = Boolean.parseBoolean(value); }

        @Override
        public boolean isModified(String newValue) { return Boolean.parseBoolean(newValue) != this.setting.defaultValue; }

        @Override
        public void setValueFromJsonElement(JsonElement element) { this.setting.value = element.getAsBoolean(); }

        @Override
        public JsonElement getAsJsonElement() { return new JsonPrimitive(this.setting.value); }
    }

    // ---------- Integer settings (blockBreakSpeed, ...) ----------

    public static class IntegerSetting extends BaritoneSettingWrapper implements IConfigInteger
    {
        private final Settings.Setting<Integer> setting;
        private final int min;
        private final int max;

        public IntegerSetting(String name, String comment, Settings.Setting<Integer> setting, int min, int max)
        {
            super(name, comment);
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
        public void setIntegerValue(int value) { this.setting.value = value; }

        @Override
        public int getMinIntegerValue() { return this.min; }

        @Override
        public int getMaxIntegerValue() { return this.max; }

        @Override
        public boolean shouldUseSlider() { return true; }

        @Override
        public boolean isModified() { return !this.setting.value.equals(this.setting.defaultValue); }

        @Override
        public void resetToDefault() { this.setting.value = this.setting.defaultValue; }

        @Override
        public String getStringValue() { return String.valueOf(this.setting.value); }

        @Override
        public String getDefaultStringValue() { return String.valueOf(this.setting.defaultValue); }

        @Override
        public void setValueFromString(String value) { this.setting.value = Integer.parseInt(value); }

        @Override
        public boolean isModified(String newValue) { return Integer.parseInt(newValue) != this.setting.defaultValue; }

        @Override
        public void setValueFromJsonElement(JsonElement element) { this.setting.value = element.getAsInt(); }

        @Override
        public JsonElement getAsJsonElement() { return new JsonPrimitive(this.setting.value); }
    }

    // ---------- Double settings (blockPlacementPenalty, ...) ----------

    public static class DoubleSetting extends BaritoneSettingWrapper implements IConfigDouble
    {
        private final Settings.Setting<Double> setting;
        private final double min;
        private final double max;

        public DoubleSetting(String name, String comment, Settings.Setting<Double> setting, double min, double max)
        {
            super(name, comment);
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
        public void setDoubleValue(double value) { this.setting.value = value; }

        @Override
        public double getMinDoubleValue() { return this.min; }

        @Override
        public double getMaxDoubleValue() { return this.max; }

        @Override
        public boolean shouldUseSlider() { return true; }

        @Override
        public boolean isModified() { return !this.setting.value.equals(this.setting.defaultValue); }

        @Override
        public void resetToDefault() { this.setting.value = this.setting.defaultValue; }

        @Override
        public String getStringValue() { return String.valueOf(this.setting.value); }

        @Override
        public String getDefaultStringValue() { return String.valueOf(this.setting.defaultValue); }

        @Override
        public void setValueFromString(String value) { this.setting.value = Double.parseDouble(value); }

        @Override
        public boolean isModified(String newValue) { return Double.parseDouble(newValue) != this.setting.defaultValue; }

        @Override
        public void setValueFromJsonElement(JsonElement element) { this.setting.value = element.getAsDouble(); }

        @Override
        public JsonElement getAsJsonElement() { return new JsonPrimitive(this.setting.value); }
    }
}