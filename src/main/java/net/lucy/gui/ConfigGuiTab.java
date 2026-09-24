package net.lucy.gui;

public enum ConfigGuiTab {
    GENERIC("Generic"),
    INFO_OVERLAYS("Info Overlays"),
    VISUALS("Visuals"),
    COLORS("Colors"),
    HOTKEYS("Hotkeys");

    private final String displayName;

    ConfigGuiTab(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}