package net.lucy.gui;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.lucy.Reference;
import net.lucy.config.Configs;

import java.util.List;

/**
 * The settings that control how Baritone should gather the materials.
 * These are the same options as on the Generic tab of the settings screen,
 * so changing one here changes it there too.
 */
public class BaritoneSettingsScreen extends GuiConfigsBase
{
    private static final List<IConfigBase> OPTIONS = ImmutableList.<IConfigBase>of(
            Configs.Generic.MAX_RISK_AMOUNT,
            Configs.Generic.MULTI_DIMENSIONAL_SIMULTANIOUSLY
    );

    public BaritoneSettingsScreen()
    {
        super(10, 50, Reference.MOD_ID, null, "Baritone Settings");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearOptions();

        String label = "Main Menu";
        int width = this.getStringWidth(label) + 20;
        ButtonGeneric button = new ButtonGeneric(10, 26, width, 20, label);
        this.addButton(button, (b, mouseButton) -> GuiBase.openGui(new MainScreen()));
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        return ConfigOptionWrapper.createFor(OPTIONS);
    }
}