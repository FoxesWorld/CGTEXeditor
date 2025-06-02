package org.foxesworld.cge.tools.cgtexEditor;

import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;
import org.foxesworld.cge.tools.cgtexEditor.preview.PreviewCell;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * PreviewPanel is a JPanel that displays a preview of the currently selected texture.
 */
public class PreviewPanel extends JPanel {

    /**
     * Constructs a PreviewPanel with a titled border labeled "Preview".
     */
    public PreviewPanel() {
        super(new BorderLayout());
        this.setBorder(new TitledBorder("Preview"));
    }

    /**
     * Updates the preview area to show the provided TextureInfo.
     * If the textureInfo parameter is null, the panel is cleared.
     * Otherwise, a new PreviewCell is created and added to the center of the panel.
     *
     * @param textureInfo the TextureInfo object to preview; if null, clears the panel
     */
    public void updatePreview(TextureInfo textureInfo) {
        this.removeAll();
        if (textureInfo != null) {
            try {
                PreviewCell cell = new PreviewCell(textureInfo);
                this.add(cell, BorderLayout.CENTER);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot display preview: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        this.revalidate();
        this.repaint();
    }
}
