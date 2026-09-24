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

/**
 * The settings screen. The column of buttons on the left switches between tabs,
 * and the list to the right shows the options that belong to the selected tab.
 */
public class SettingsScreen extends GuiConfigsBase
{
    public SettingsScreen()
    {
        // list starts right of the tab-button column
        super(SideButtonBar.WIDTH + 10, 30, Reference.MOD_ID, null, "Lazy Material Gathering Settings");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearOptions();

        int y = SideButtonBar.START_Y;

        for (ConfigGuiTab tab : ConfigGuiTab.values())
        {
            y += this.createTabButton(y, tab);
        }

        String mainMenuLabel = "Main Menu";
        int mainMenuWidth = Math.max(SideButtonBar.BUTTON_WIDTH, this.getStringWidth(mainMenuLabel) + 10);
        ButtonGeneric mainMenuButton = new ButtonGeneric(SideButtonBar.X, y + 6, mainMenuWidth, 20, mainMenuLabel);
        this.addButton(mainMenuButton, (b, mouseButton) -> fi.dy.masa.malilib.gui.GuiBase.openGui(new MainScreen()));
    }

    private int createTabButton(int y, ConfigGuiTab tab)
    {
        String label = tab.getDisplayName();
        int width = Math.max(SideButtonBar.BUTTON_WIDTH, this.getStringWidth(label) + 10);

        ButtonGeneric button = new ButtonGeneric(SideButtonBar.X, y, width, 20, label);
        button.setEnabled(DataManager.getConfigGuiTab() != tab); // the current tab's button is greyed out
        this.addButton(button, new TabButtonListener(tab, this));

        return SideButtonBar.SPACING;
    }

    // Which options to show for the selected tab
    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        List<? extends IConfigBase> configs;

        switch (DataManager.getConfigGuiTab())
        {
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

    // Width of the value column, so long values (like a folder path) are readable
    @Override
    protected int getConfigWidth()
    {
        ConfigGuiTab tab = DataManager.getConfigGuiTab();

        if (tab == ConfigGuiTab.GENERIC || tab == ConfigGuiTab.INFO_OVERLAYS || tab == ConfigGuiTab.VISUALS)
        {
            return 140;
        }

        if (tab == ConfigGuiTab.COLORS)
        {
            return 100;
        }

        return super.getConfigWidth();
    }

    // Only the hotkeys tab needs the "search by key" box
    @Override
    protected boolean useKeybindSearch()
    {
        return DataManager.getConfigGuiTab() == ConfigGuiTab.HOTKEYS;
    }

    private static class TabButtonListener implements IButtonActionListener
    {
        private final ConfigGuiTab tab;
        private final SettingsScreen parent;

        public TabButtonListener(ConfigGuiTab tab, SettingsScreen parent)
        {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            DataManager.setConfigGuiTab(this.tab);

            // Rebuild the list for the new tab, scroll back to the top, and redraw the buttons
            this.parent.reCreateListWidget();
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }
}