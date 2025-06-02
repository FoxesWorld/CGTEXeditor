package org.foxesworld.cge.tools.cgtexEditor;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatPropertiesLaf;
import org.foxesworld.cge.ICOParser;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods for loading icons, setting up themes, creating file choosers, and related UI tasks.
 */
public class UIUtils {

    private static final String ICONS_PATH = "icons/";
    private static final String THEME_ICON_PATH = "theme/engineLogo.ico";
    private static final int ICON_SIZE = 32;

    /**
     * Configures the Look and Feel for Swing using FlatLaf.
     * If the specified theme resource cannot be loaded, falls back to a basic dark theme.
     *
     * @param themeResourcePath the path to the theme properties file in resources (e.g., "theme/calista.properties")
     */
    public static void setupTheme(String themeResourcePath) {
        try (InputStream themeStream = UIUtils.class.getClassLoader().getResourceAsStream(themeResourcePath)) {
            if (themeStream == null) {
                throw new RuntimeException("Theme file not found in resources: " + themeResourcePath);
            }
            FlatPropertiesLaf laf = new FlatPropertiesLaf("Dark Theme", themeStream);
            FlatLaf.setup(laf);
        } catch (Exception ex) {
            FlatLaf.setup(new FlatDarkLaf());
            ex.printStackTrace();
        }
    }

    /**
     * Loads an .ico icon from resources and sets it as the icon for the given JFrame.
     * Attempts to select the best-resolution image from the .ico file.
     *
     * @param frame the JFrame on which to set the window icon
     */
    public static void setFrameIconFromICO(JFrame frame) {
        try (InputStream icoStream = UIUtils.class.getClassLoader().getResourceAsStream(THEME_ICON_PATH)) {
            if (icoStream != null) {
                ICOParser parser = new ICOParser();
                java.util.List<BufferedImage> iconsList = parser.parse(icoStream);
                BufferedImage bestIcon = parser.getBestIcon(iconsList);
                if (bestIcon != null) {
                    frame.setIconImages(java.util.List.of(bestIcon));
                }
            } else {
                System.err.println("ICO icon resource not found: " + THEME_ICON_PATH);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads an ImageIcon from the icons directory in resources and scales it to a fixed size.
     * Returns a transparent placeholder icon if loading fails.
     *
     * @param iconFilename the filename of the icon under the icons folder (e.g., "add_icon.png")
     * @return a scaled ImageIcon, or a blank 32×32 icon if loading fails
     */
    public static ImageIcon loadIcon(String iconFilename) {
        try (InputStream is = UIUtils.class.getClassLoader().getResourceAsStream(ICONS_PATH + iconFilename)) {
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                Image scaled = img.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (IOException ignored) {
        }
        BufferedImage empty = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        return new ImageIcon(empty);
    }

    /**
     * Creates and configures a JFileChooser with a filter for the specified file extension.
     *
     * @param dialogTitle the title to display on the file chooser dialog
     * @param extension   the file extension to filter by (without the dot), e.g., "dds" or "cgtex"
     * @param multiSelect true to allow multiple selection; false for single selection only
     * @return a configured JFileChooser instance
     */
    public static JFileChooser createFileChooser(String dialogTitle, String extension, boolean multiSelect) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setMultiSelectionEnabled(multiSelect);
        chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase(), extension));
        return chooser;
    }

    /**
     * Removes the file extension from the given filename string.
     *
     * @param filename the filename from which to strip the extension
     * @return the filename without its extension, or the original string if no '.' is found
     */
    public static String stripExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx > 0) ? filename.substring(0, idx) : filename;
    }
}
