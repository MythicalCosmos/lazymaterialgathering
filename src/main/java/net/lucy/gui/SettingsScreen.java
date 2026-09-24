package net.lucy.gui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.lucy.Reference;
import net.lucy.config.Configs;
import net.lucy.config.Hotkeys;
import net.lucy.data.DataManager;

import java.util.Collections;
import java.util.List;


public class SettingsScreen extends GuiConfigsBase {
    public SettingsScreen() {
        super(10, 50, Reference.MOD_ID, null, "Lazy Material Gathering Settings");
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;

        for (ConfigGuiTab tab : ConfigGuiTab.values()) {
            x += this.createTabButton(x, y, tab);
        }
    }

    private int createTabButton(int x, int y, ConfigGuiTab tab)
    {
        String label = tab.getDisplayName();
        int width = this.getStringWidth(label) + 10;

        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        button.setEnabled(DataManager.getConfigGuiTab() != tab);
        this.addButton(button, new TabButtonListener(tab, this));

        return width + 2;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<? extends IConfigBase> configs;

        switch (DataManager.getConfigGuiTab()) {
            case GENERIC:
                configs = Configs.Generic.OPTIONS;
                break;
            case INFO_OVERLAYS:
                configs = Configs.InfoOverlays.OPTIONS;
                break;
            case VISUALS:
                configs = Configs.Visuals.OPTIONS;
                break;
            case COLORS:
                configs = Configs.Colors.OPTIONS;
                break;
            case HOTKEYS:
                configs = Hotkeys.HOTKEY_LIST;
                break;
            default:
                return Collections.emptyList();
        }

        return ConfigOptionWrapper.createFor(configs);
    }

    @Override
    protected int getConfigWidth() {
        ConfigGuiTab tab = DataManager.getConfigGuiTab();

        if (tab == ConfigGuiTab.GENERIC || tab == ConfigGuiTab.INFO_OVERLAYS || tab == ConfigGuiTab.VISUALS) {
            return 140;
        }

        if (tab == ConfigGuiTab.COLORS) {
            return 100;
        }

        return super.getConfigWidth();
    }

    @Override
    protected boolean useKeybindSearch() {
        return DataManager.getConfigGuiTab() == ConfigGuiTab.HOTKEYS;
    }

    private static class TabButtonListener implements IButtonActionListener {
        private final ConfigGuiTab tab;
        private final SettingsScreen parent;

        public TabButtonListener(ConfigGuiTab tab, SettingsScreen parent) {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            DataManager.setConfigGuiTab(this.tab);

            this.parent.reCreateListWidget();
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }
}