package net.lucy.gui;

import fi.dy.masa.malilib.gui.interfaces.IFileBrowserIconProvider;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.render.RenderUtils;
import net.lucy.Reference;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.io.File;

public enum BrowserIcons implements IGuiIcon, IFileBrowserIconProvider {
    FILE            (0),
    DIRECTORY       (12),
    DIRECTORY_UP    (24),
    DIRECTORY_ROOT  (36),
    CREATE_DIRECTORY(48),
    SEARCH          (60);

    public static final Identifier TEXTURE = new Identifier(Reference.MOD_ID, "textures/gui/gui_widgets.png");
    private static final int SIZE = 12;

    private final int u;

    BrowserIcons(int u) {
        this.u = u;
    }


    @Override
    public int getWidth() {
        return SIZE;
    }

    @Override
    public int getHeight() {
        return SIZE;
    }

    @Override
    public int getU() {
        return this.u;
    }

    @Override
    public int getV() {
        return 0;
    }

    @Override
    public void renderAt(int x, int y, float zLevel, boolean enabled, boolean selected) {
        RenderUtils.drawTexturedRect(x, y, this.u, 0, SIZE, SIZE, zLevel);
    }

    @Override
    public Identifier getTexture() {
        return TEXTURE;
    }


    @Override
    public IGuiIcon getIconRoot() {
        return DIRECTORY_ROOT;
    }

    @Override
    public IGuiIcon getIconUp() {
        return DIRECTORY_UP;
    }

    @Override
    public IGuiIcon getIconCreateDirectory() {
        return CREATE_DIRECTORY;
    }

    @Override
    public IGuiIcon getIconSearch() {
        return SEARCH;
    }

    @Override
    public IGuiIcon getIconDirectory() {
        return DIRECTORY;
    }

    @Nullable
    @Override
    public IGuiIcon getIconForFile(File file) {
        return FILE;
    }
}