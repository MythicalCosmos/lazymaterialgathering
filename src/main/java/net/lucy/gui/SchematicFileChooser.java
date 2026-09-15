package net.lucy.gui;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class SchematicFileChooser {

    private SchematicFileChooser() {
    }

    /**
     * Opens a Windows file chooser for .litematic files.
     *
     * This is intentionally kept separate from the Minecraft GUI.
     */
    public static File choose() {

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Select a Litematic schematic"
        );

        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        "Litematic files (*.litematic)",
                        "litematic"
                )
        );

        chooser.setAcceptAllFileFilterUsed(false);

        int result =
                chooser.showOpenDialog(null);

        if (result ==
                JFileChooser.APPROVE_OPTION) {

            return chooser.getSelectedFile();
        }

        return null;
    }
}
