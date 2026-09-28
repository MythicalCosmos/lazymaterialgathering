package net.lucy.gui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.lucy.Reference;
import net.lucy.data.GuiState;

import java.util.List;

/**
 * Every one of Baritone's own settings that this mod can show on a plain config screen
 * (see BaritoneSettingsRegistry for how they're found and which ones are left out), laid
 * out the same way as the mod's own settings screen: a row of tabs at the top, a scrolling
 * option list below. Changing a value here runs Baritone's real "set" command, so it takes
 * effect immediately and shows Baritone's own confirmation in chat, the same as typing it
 * yourself would.
 */
public class BaritoneSettingsScreen extends GuiConfigsBase {
    public BaritoneSettingsScreen() {
        super(10, 50, Reference.MOD_ID, null, "Baritone Settings");
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();
        int x = 10;
        int y = 26;

        for (String tabName : BaritoneSettingsRegistry.getTabNames()) {
            x += this.createTabButton(x, y, tabName);
        }

        String mainMenuLabel = "Main Menu";
        int mainMenuWidth = this.getStringWidth(mainMenuLabel) + 20;
        ButtonGeneric mainMenuButton = new ButtonGeneric(this.width - mainMenuWidth - 10, y, mainMenuWidth, 20, mainMenuLabel);
        this.addButton(mainMenuButton, (button, mouseButton) -> GuiBase.openGui(new MainScreen()));
    }

    private int createTabButton(int x, int y, String tabName) {
        int width = this.getStringWidth(tabName) + 10;
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tabName);
        button.setEnabled(GuiState.getBaritoneTabName().equals(tabName) == false); // grey out the current tab
        this.addButton(button, new TabButtonListener(tabName, this));
        return width + 2;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<IConfigBase> options = BaritoneSettingsRegistry.getSettingsForTab(GuiState.getBaritoneTabName());
        return ConfigOptionWrapper.createFor(options);
    }

    private static class TabButtonListener implements IButtonActionListener {
        private final String tabName;
        private final BaritoneSettingsScreen parent;

        public TabButtonListener(String tabName, BaritoneSettingsScreen parent) {
            this.tabName = tabName;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            GuiState.setBaritoneTabName(this.tabName);
            this.parent.reCreateListWidget();
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }
}