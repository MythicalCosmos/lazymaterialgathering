package net.lucy.gui;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.lucy.Reference;
import net.lucy.data.DataManager;

import java.util.List;

/**
 * Baritone's pathfinding settings, laid out the same way as the mod's own settings
 * screen (a column of tabs on the left next to a scrolling option list). This edits
 * Baritone's real settings object directly, so a change here takes effect immediately,
 * the same as typing a Baritone chat command would.
 */
public class BaritoneSettingsScreen extends GuiConfigsBase
{
    public BaritoneSettingsScreen()
    {
        super(SideButtonBar.WIDTH + 10, 30, Reference.MOD_ID, null, "Baritone Settings");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearOptions();

        int y = SideButtonBar.START_Y;

        for (BaritoneConfigTab tab : BaritoneConfigTab.values())
        {
            y += this.createTabButton(y, tab);
        }

        String mainMenuLabel = "Main Menu";
        int mainMenuWidth = Math.max(SideButtonBar.BUTTON_WIDTH, this.getStringWidth(mainMenuLabel) + 10);
        ButtonGeneric mainMenuButton = new ButtonGeneric(SideButtonBar.X, y + 6, mainMenuWidth, 20, mainMenuLabel);
        this.addButton(mainMenuButton, (button, mouseButton) -> GuiBase.openGui(new MainScreen()));
    }

    private int createTabButton(int y, BaritoneConfigTab tab)
    {
        String label = tab.getDisplayName();
        int width = Math.max(SideButtonBar.BUTTON_WIDTH, this.getStringWidth(label) + 10);

        ButtonGeneric button = new ButtonGeneric(SideButtonBar.X, y, width, 20, label);
        button.setEnabled(DataManager.getBaritoneConfigTab() != tab); // grey out the current tab
        this.addButton(button, new TabButtonListener(tab, this));

        return SideButtonBar.SPACING;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        Settings s = BaritoneAPI.getSettings();
        List<IConfigBase> options;

        switch (DataManager.getBaritoneConfigTab())
        {
            case MOVEMENT:
                options = ImmutableList.of(
                        new BaritoneSettingWrapper.BooleanSetting("allowSprint",
                                "Whether Baritone is allowed to sprint.", s.allowSprint),
                        new BaritoneSettingWrapper.BooleanSetting("allowParkour",
                                "Whether Baritone is allowed to jump across gaps.", s.allowParkour),
                        new BaritoneSettingWrapper.BooleanSetting("allowParkourPlace",
                                "Whether Baritone can place a block to parkour off of, mid-jump.", s.allowParkourPlace),
                        new BaritoneSettingWrapper.BooleanSetting("allowDiagonalAscend",
                                "Whether Baritone can climb up diagonally.", s.allowDiagonalAscend),
                        new BaritoneSettingWrapper.BooleanSetting("allowDiagonalDescend",
                                "Whether Baritone can climb down diagonally.", s.allowDiagonalDescend),
                        new BaritoneSettingWrapper.BooleanSetting("allowJumpAt256",
                                "Whether Baritone can jump when it's at the build height limit.", s.allowJumpAt256),
                        new BaritoneSettingWrapper.BooleanSetting("assumeWalkOnWater",
                                "Whether Baritone assumes it's safe to walk on water (for example with Frost Walker).", s.assumeWalkOnWater)
                );
                break;

            case MINING:
                options = ImmutableList.of(
                        new BaritoneSettingWrapper.BooleanSetting("allowBreak",
                                "Whether Baritone is allowed to break blocks at all. Turn this off for a pure walk-and-collect run.", s.allowBreak),
                        new BaritoneSettingWrapper.BooleanSetting("allowPlace",
                                "Whether Baritone is allowed to place blocks (for scaffolding and parkour placement).", s.allowPlace),
                        new BaritoneSettingWrapper.BooleanSetting("allowInventory",
                                "Whether Baritone can move items in your inventory (for example to keep a pickaxe in hand).", s.allowInventory),
                        new BaritoneSettingWrapper.IntegerSetting("blockBreakSpeed",
                                "How many ticks Baritone waits between starting to break each block. Higher is slower but safer on bad connections.",
                                s.blockBreakSpeed, 1, 20),
                        new BaritoneSettingWrapper.BooleanSetting("allowOnlyExposedOres",
                                "Only mine ores that are already exposed to air, instead of digging toward hidden ones.", s.allowOnlyExposedOres),
                        new BaritoneSettingWrapper.IntegerSetting("allowOnlyExposedOresDistance",
                                "How many blocks of cover still counts as \"exposed\" when the setting above is on.",
                                s.allowOnlyExposedOresDistance, 1, 6),
                        new BaritoneSettingWrapper.BooleanSetting("mineScanDroppedItems",
                                "Whether Baritone should path toward dropped items while mining, so ore drops aren't left behind.", s.mineScanDroppedItems)
                );
                break;

            case PATHING:
                options = ImmutableList.of(
                        new BaritoneSettingWrapper.DoubleSetting("blockPlacementPenalty",
                                "Extra path cost for a route that requires placing a block. Raise this to make Baritone place blocks only when there's no other way.",
                                s.blockPlacementPenalty, 0.0, 100.0),
                        new BaritoneSettingWrapper.DoubleSetting("blockBreakAdditionalPenalty",
                                "Extra path cost for a route that requires breaking a block. This is the risk amount you set on the Generic tab.",
                                s.blockBreakAdditionalPenalty, 0.0, 100.0),
                        new BaritoneSettingWrapper.DoubleSetting("avoidBreakingMultiplier",
                                "How much cheaper it is to walk around a block Baritone doesn't want to break (like ores it's saving for later), instead of breaking it. Lower means it avoids breaking those blocks more strongly.",
                                s.avoidBreakingMultiplier, 0.0, 1.0),
                        new BaritoneSettingWrapper.DoubleSetting("walkOnWaterOnePenalty",
                                "Extra path cost for walking across water using a single block placement.",
                                s.walkOnWaterOnePenalty, 0.0, 50.0)
                );
                break;

            case RENDERING:
                options = ImmutableList.of(
                        new BaritoneSettingWrapper.BooleanSetting("renderPath",
                                "Whether Baritone draws its planned route on screen.", s.renderPath),
                        new BaritoneSettingWrapper.BooleanSetting("renderPathAsLine",
                                "Draw the path as a thin line instead of a row of highlighted blocks.", s.renderPathAsLine),
                        new BaritoneSettingWrapper.BooleanSetting("renderPathIgnoreDepth",
                                "Draw the path through walls, instead of only where you could actually see it.", s.renderPathIgnoreDepth),
                        new BaritoneSettingWrapper.BooleanSetting("freeLook",
                                "Let Baritone move the camera to look where it's going while it walks.", s.freeLook)
                );
                break;

            default:
                return java.util.Collections.emptyList();
        }

        return ConfigOptionWrapper.createFor(options);
    }

    private static class TabButtonListener implements IButtonActionListener
    {
        private final BaritoneConfigTab tab;
        private final BaritoneSettingsScreen parent;

        public TabButtonListener(BaritoneConfigTab tab, BaritoneSettingsScreen parent)
        {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            DataManager.setBaritoneConfigTab(this.tab);

            this.parent.reCreateListWidget();
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }
}