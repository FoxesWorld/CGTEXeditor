package org.foxesworld.cge.tools.cgtexEditor.preview;

import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * PreviewCell is a JPanel that displays a scaled preview image of a texture
 * along with basic information (format and dimensions). It also provides a hover effect.
 */
public class PreviewCell extends JPanel {
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 250;
    private static final Color BACKGROUND_COLOR = new Color(30, 30, 30);
    private static final Color HOVER_COLOR = new Color(50, 50, 50);
    private static final Color BORDER_COLOR = new Color(70, 70, 70);
    private static final Color INFO_TEXT_COLOR = new Color(130, 130, 130);
    private static final Font INFO_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final int PREVIEW_IMAGE_WIDTH = 180;

    /**
     * Constructs a PreviewCell for the given TextureInfo. The panel will display
     * a scaled preview image and a small info section showing format and dimensions.
     *
     * @param ti the TextureInfo containing image data, format, and dimensions
     */
    public PreviewCell(TextureInfo ti) {
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 2, true));

        JLabel previewLabel = createPreviewLabel(ti);
        add(previewLabel, BorderLayout.CENTER);

        JPanel infoPanel = createInfoPanel(ti);
        add(infoPanel, BorderLayout.SOUTH);

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                setBackground(HOVER_COLOR);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                setBackground(BACKGROUND_COLOR);
            }
        });
    }

    /**
     * Creates a JLabel containing a scaled preview image extracted from the TextureInfo.
     *
     * @param ti the TextureInfo providing the source BufferedImage
     * @return a JLabel configured with the scaled preview image
     */
    private JLabel createPreviewLabel(TextureInfo ti) {
        BufferedImage img = ti.getPreviewImage();
        Image scaledImage = img.getScaledInstance(PREVIEW_IMAGE_WIDTH, -1, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaledImage);

        JLabel previewLabel = new JLabel(icon);
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true));
        previewLabel.setBackground(Color.BLACK);
        previewLabel.setOpaque(true);

        return previewLabel;
    }

    /**
     * Creates a panel displaying texture format and dimensions in a vertical layout.
     *
     * @param ti the TextureInfo providing format code, width, and height
     * @return a JPanel containing formatted labels for texture info
     */
    private JPanel createInfoPanel(TextureInfo ti) {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel typeLabel = new JLabel("Format: DXT" + ti.getFormatCode());
        typeLabel.setFont(INFO_FONT);
        typeLabel.setForeground(INFO_TEXT_COLOR);
        infoPanel.add(typeLabel);

        JLabel sizeLabel = new JLabel(String.format("Size: %dx%d", ti.getWidth(), ti.getHeight()));
        sizeLabel.setFont(INFO_FONT);
        sizeLabel.setForeground(INFO_TEXT_COLOR);
        infoPanel.add(sizeLabel);

        return infoPanel;
    }
}
