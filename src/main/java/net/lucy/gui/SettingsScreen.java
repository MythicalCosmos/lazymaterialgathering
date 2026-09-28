package net.lucy.gui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.lucy.Reference;
import net.lucy.config.Configs;
import net.lucy.config.Hotkeys;
import net.lucy.data.GuiState;

import java.util.Collections;
import java.util.List;

/**
 * The settings screen. The row of buttons at the top switches between tabs, the same
 * layout Litematica's own settings screen (GuiConfigs/GuiRenderLayer) uses, and the list
 * below shows the options that belong to the selected tab.
 */
public class SettingsScreen extends GuiConfigsBase {
    public SettingsScreen() {
        // list starts at x=10, y=50 (leaving room for the tab buttons above it)
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

        String mainMenuLabel = "Main Menu";
        int mainMenuWidth = this.getStringWidth(mainMenuLabel) + 20;
        ButtonGeneric mainMenuButton = new ButtonGeneric(this.width - mainMenuWidth - 10, y, mainMenuWidth, 20, mainMenuLabel);
        this.addButton(mainMenuButton, (b, mouseButton) -> GuiBase.openGui(new MainScreen()));
    }

    private int createTabButton(int x, int y, ConfigGuiTab tab) {
        String label = tab.getDisplayName();
        int width = this.getStringWidth(label) + 10;
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        button.setEnabled(GuiState.getConfigGuiTab() != tab); // the current tab's button is greyed out
        this.addButton(button, new TabButtonListener(tab, this));
        return width + 2;
    }

    // Which options to show for the selected tab
    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<? extends IConfigBase> configs;
        switch (GuiState.getConfigGuiTab()) {
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
        ConfigGuiTab tab = GuiState.getConfigGuiTab();
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
        return GuiState.getConfigGuiTab() == ConfigGuiTab.HOTKEYS;
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
            GuiState.setConfigGuiTab(this.tab);
            this.parent.reCreateListWidget();
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }
}