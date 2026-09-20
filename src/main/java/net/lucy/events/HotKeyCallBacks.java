package net.lucy.events;

import net.lucy.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.lucy.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import net.lucy.config.Configs;


public class HotKeyCallBacks {
    public static void init(MinecraftClient mc) {
        IHotkeyCallback callbackHotkeys = new KeyCallbackHotkeys(mc);
        IHotkeyCallback callbackMessage = new KeyCallbackToggleMessage(mc);
        ValueChangeCallback valueChangeCallback = new ValueChangeCallback();

        Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind().setCallback(callbackHotkeys);
    }

    private static class ValueChangeCallback implements IValueChangeCallback<ConfigString>
    {
        @Override
        public void onValueChanged(ConfigString config)
        {
            if (config == Configs.Generic.PICK_BLOCKABLE_SLOTS)
            {
                InventoryUtils.setPickBlockableSlots(Configs.Generic.PICK_BLOCKABLE_SLOTS.getStringValue());
            }
        }
    }

    private static class RenderToggle extends KeyCallbackToggleBooleanConfigWithMessage
    {
        public RenderToggle(IConfigBoolean config)
        {
            super(config);
        }

        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            super.onKeyAction(action, key);

            if (this.config.getBooleanValue())
            {
                SchematicWorldRefresher.INSTANCE.updateAll();
            }

            return true;
        }
        private static class KeyCallbackHotkeys implements IHotkeyCallback
        {
            private final MinecraftClient mc;

            public KeyCallbackHotkeys(MinecraftClient mc)
            {
                this.mc = mc;
            }

            @Override
            public boolean onKeyAction(KeyAction action, IKeybind key)
            {
                if (this.mc.player == null || this.mc.world == null)
                {
                    return false;
                    if (mode.getUsesAreaSelection())
                    {
                        return DataManager.getSelectionManager().createNewSubRegion(this.mc, true);
                    }
                }
                else if (key == Hotkeys.DELETE_SELECTION_BOX.getKeybind())
                {
                    if (mode.getUsesAreaSelection())
                    {
                        SelectionManager sm = DataManager.getSelectionManager();
                        AreaSelection selection = sm.getCurrentSelection();

                        if (selection != null)
                        {
                            if (selection.isOriginSelected())
                            {
                                selection.setExplicitOrigin(null);
                                selection.setOriginSelected(false);
                                InfoUtils.printActionbarMessage("litematica.message.removed_area_origin");
                            }
                            else
                            {
                                String name = selection.getCurrentSubRegionBoxName();

                                if (name != null && selection.removeSelectedSubRegionBox())
                                {
                                    InfoUtils.printActionbarMessage("litematica.message.removed_selection_box", name);
                                    return true;
                                }
                            }
                        }
                    }
                }
                else if (key == Hotkeys.MOVE_ENTIRE_SELECTION.getKeybind())
                {
                    if (mode.getUsesAreaSelection())
                    {
                        SelectionManager sm = DataManager.getSelectionManager();
                        AreaSelection selection = sm.getCurrentSelection();

                        if (selection != null)
                        {
                            BlockPos pos = BlockPos.ofFloored(this.mc.player.getPos());

                            if (mode == ToolMode.MOVE)
                            {
                                SchematicUtils.moveCurrentlySelectedWorldRegionTo(pos, this.mc);
                            }
                            else
                            {
                                selection.moveEntireSelectionTo(pos, true);
                            }

                            return true;
                        }
                    }
                    else if (mode.getUsesSchematic())
                    {
                        BlockPos pos = BlockPos.ofFloored(this.mc.player.getPos());
                        DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionTo(pos, this.mc);
                        return true;
                    }
                }
                else if (key == Hotkeys.SELECTION_MODE_CYCLE.getKeybind())
                {
                    if (mode == ToolMode.DELETE)
                    {
                        ToolModeData.DELETE.toggleUsePlacement();
                    }
                    else if (mode == ToolMode.PASTE_SCHEMATIC)
                    {
                        Configs.Generic.PASTE_REPLACE_BEHAVIOR.setOptionListValue(Configs.Generic.PASTE_REPLACE_BEHAVIOR.getOptionListValue().cycle(false));
                    }
                    else if (mode.getUsesAreaSelection())
                    {
                        Configs.Generic.SELECTION_CORNERS_MODE.setOptionListValue(Configs.Generic.SELECTION_CORNERS_MODE.getOptionListValue().cycle(false));
                    }

                    return true;
                }
                else if (key == Hotkeys.SET_AREA_ORIGIN.getKeybind())
                {
                    if (mode.getUsesAreaSelection())
                    {
                        SelectionManager sm = DataManager.getSelectionManager();
                        AreaSelection area = sm.getCurrentSelection();

                        if (area != null)
                        {
                            BlockPos pos = BlockPos.ofFloored(this.mc.player.getPos());
                            area.setExplicitOrigin(pos);
                            String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                            InfoUtils.printActionbarMessage("litematica.message.set_area_origin", posStr);
                            return true;
                        }
                    }
                }
                else if (key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() ||
                        key == Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind())
                {
                    if (mode.getUsesAreaSelection())
                    {
                        SelectionManager sm = DataManager.getSelectionManager();
                        AreaSelection area = sm.getCurrentSelection();

                        if (area != null && area.getSelectedSubRegionBox() != null)
                        {
                            BlockPos pos = BlockPos.ofFloored(this.mc.player.getPos());
                            Corner corner = key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() ? Corner.CORNER_1 : Corner.CORNER_2;
                            area.setSelectedSubRegionCornerPos(pos, corner);

                            String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                            InfoUtils.printActionbarMessage("litematica.message.set_selection_box_point", corner.ordinal(), posStr);
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }