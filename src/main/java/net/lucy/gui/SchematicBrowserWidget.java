package net.lucy.gui;

import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import net.lucy.data.DataManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileFilter;

public class SchematicBrowserWidget extends WidgetFileBrowserBase {
    private static final FileFilter SCHEMATIC_FILTER = new SchematicFileFilter();

    public SchematicBrowserWidget(int x, int y, int width, int height,
                                  String browserContext, File defaultDirectory,
                                  @Nullable ISelectionListener<DirectoryEntry> selectionListener) {
        super(x, y, width, height, DataManager.getDirectoryCache(), browserContext,
                defaultDirectory, selectionListener, BrowserIcons.FILE);
    }

    @Override
    protected File getRootDirectory() {
        return DataManager.getSchematicsDirectory();
    }

    @Override
    protected FileFilter getFileFilter() {
        return SCHEMATIC_FILTER;
    }

    private static class SchematicFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            String name = file.getName().toLowerCase();

            return name.endsWith(".litematic") ||
                    name.endsWith(".schem") ||
                    name.endsWith(".schematic") ||
                    name.endsWith(".nbt");
        }
    }
}