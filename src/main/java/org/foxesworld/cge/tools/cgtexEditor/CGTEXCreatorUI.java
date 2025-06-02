package org.foxesworld.cge.tools.cgtexEditor;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatPropertiesLaf;
import org.foxesworld.cge.ICOParser;
import org.foxesworld.cge.core.file.cgtex.CGTEXFile;
import org.foxesworld.cge.core.file.cgtex.TextureEntry;
import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;
import org.foxesworld.cge.tools.cgtexEditor.preview.DDSParser;
import org.foxesworld.cge.tools.cgtexEditor.preview.PreviewCell;
import org.foxesworld.cge.tools.cgtexEditor.preview.TextureCellRenderer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * CGTEXCreatorUI — graphical user interface for creating and editing
 * .cgtex files based on DDS textures.
 */
public class CGTEXCreatorUI extends JFrame {

    // === Constants ===
    private static final String ICONS_PATH = "icons/";
    private static final String THEME_ICON_PATH = "theme/engineLogo.ico";

    private static final String FILTER_DDS = "dds";
    private static final String FILTER_CGTEX = "cgtex";

    private static final int ICON_SIZE = 32;
    private static final int DEFAULT_WIDTH = 900;
    private static final int DEFAULT_HEIGHT = 600;
    private static final int COUNT_PADDING = 10; // padding from edges for count label

    // === Texture list (data model) ===
    private final List<TextureInfo> textures = new ArrayList<>();
    private final DefaultListModel<TextureInfo> listModel = new DefaultListModel<>();

    // === UI components ===
    private final JList<TextureInfo> fileList = new JList<>(listModel);
    private final JLabel countLabel = new JLabel("0"); // Shows the total count
    private final JPanel previewPanel = new JPanel(new BorderLayout());
    private JLayeredPane leftLayer; // Moved to field so it can be accessed in updateCountLabel

    private File selectedCgtFile;

    public CGTEXCreatorUI() {
        super("CGTEX Creator");
        configureFrame();
        setIconFromIcoResource();
        initUI();
    }

    /**
     * Configure main JFrame settings.
     */
    private void configureFrame() {
        System.setProperty("log.dir", System.getProperty("user.dir"));
        System.setProperty("log.level", "DEBUG");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);
    }

    /**
     * Loads and sets the window icon from an .ico resource.
     */
    private void setIconFromIcoResource() {
        try (InputStream icoStream = getClass().getClassLoader().getResourceAsStream(THEME_ICON_PATH)) {
            if (icoStream != null) {
                ICOParser parser = new ICOParser();
                List<BufferedImage> iconsList = parser.parse(icoStream);
                BufferedImage bestIcon = parser.getBestIcon(iconsList);
                if (bestIcon != null) {
                    setIconImages(List.of(bestIcon));
                }
            } else {
                System.err.println("ICO icon resource not found: " + THEME_ICON_PATH);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes all UI components and layout.
     * Adds a single, large countLabel in the bottom-right corner of the texture list.
     */
    private void initUI() {
        // --- Left panel: a layered pane with the list of DDS files and a big count label overlaid ---
        setupFileList();

        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(new TitledBorder("Texture Files"));

        // Prepare countLabel: large semi-transparent font in bottom-right
        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 48f));
        countLabel.setForeground(new Color(0, 0, 0, 80)); // semi-transparent dark gray
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        // Create a JLayeredPane to overlay countLabel atop the scroll pane
        leftLayer = new JLayeredPane();
        leftLayer.setPreferredSize(new Dimension(DEFAULT_WIDTH / 3, DEFAULT_HEIGHT));
        leftLayer.setLayout(null);

        // Add scroll pane at layer 0
        listScroll.setBounds(0, 0, leftLayer.getPreferredSize().width, leftLayer.getPreferredSize().height);
        leftLayer.add(listScroll, Integer.valueOf(0));

        // Add countLabel at layer 1 (will be positioned in componentResized listener and updateCountLabel)
        leftLayer.add(countLabel, Integer.valueOf(1));

        // When the layered pane is resized (e.g., window resized), update bounds of components
        leftLayer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = leftLayer.getWidth();
                int h = leftLayer.getHeight();

                // Resize scroll pane to fill entire layered pane
                listScroll.setBounds(0, 0, w, h);

                // Re-position countLabel in bottom-right
                repositionCountLabel();
            }
        });

        // --- Buttons panel on the left (Add/Remove) above the layered pane ---
        JButton addBtn = createIconButton("Add DDS", "add_icon.png");
        JButton remBtn = createIconButton("Remove", "remove_icon.png");
        addBtn.addActionListener(e -> onAdd());
        remBtn.addActionListener(e -> onRemove());
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttonsPanel.add(addBtn);
        buttonsPanel.add(remBtn);

        // Combine buttons and layered list into a single leftPanel
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.add(buttonsPanel, BorderLayout.NORTH);
        leftPanel.add(leftLayer, BorderLayout.CENTER);

        // --- Top panel: Read .cgtex button ---
        JButton readBtn = createIconButton("Read CGTEX", "read_icon.png");
        readBtn.addActionListener(e -> onReadCGTEX());
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topButtons.add(readBtn);

        // --- Center panel: preview of selected texture ---
        previewPanel.setBorder(new TitledBorder("Preview"));
        JScrollPane previewScroll = new JScrollPane(previewPanel);

        // --- Bottom panel: Save .cgtex button ---
        JButton saveBtn = createIconButton("Save .cgtex", "save_icon.png");
        saveBtn.addActionListener(e -> onSave());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bottomPanel.add(saveBtn);

        // --- Assemble the entire window ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, previewScroll);
        splitPane.setResizeWeight(0.3);

        Container cp = getContentPane();
        cp.setLayout(new BorderLayout(5, 5));
        cp.add(topButtons, BorderLayout.NORTH);
        cp.add(splitPane, BorderLayout.CENTER);
        cp.add(bottomPanel, BorderLayout.SOUTH);

        // Initialize empty list at startup
        refreshFileList(new ArrayList<>());
    }

    /**
     * Sets up JList to display TextureInfo list.
     */
    private void setupFileList() {
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer(new TextureCellRenderer());
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                TextureInfo info = fileList.getSelectedValue();
                updatePreview(info);
            }
        });
    }

    /**
     * Refreshes the list model contents from the provided list,
     * and updates the count label.
     */
    private void refreshFileList(List<TextureInfo> newList) {
        listModel.clear();
        for (TextureInfo info : newList) {
            listModel.addElement(info);
        }
        updateCountLabel();
    }

    /**
     * Updates the count label to reflect current number of textures,
     * and immediately repositions it so no ellipsis appears.
     */
    private void updateCountLabel() {
        countLabel.setText(String.valueOf(textures.size()));
        repositionCountLabel();
    }

    /**
     * Repositions the countLabel in the bottom-right corner of leftLayer.
     * Uses the current preferred size of countLabel to avoid truncation.
     */
    private void repositionCountLabel() {
        if (leftLayer == null) return;

        int w = leftLayer.getWidth();
        int h = leftLayer.getHeight();

        // Get the updated preferred size for the current text
        Dimension sz = countLabel.getPreferredSize();

        int x = w - sz.width - COUNT_PADDING;
        int y = h - sz.height - COUNT_PADDING;
        countLabel.setBounds(x, y, sz.width, sz.height);
    }

    /**
     * Creates a JButton with an icon from resources and text.
     */
    private JButton createIconButton(String text, String iconName) {
        ImageIcon icon = loadIcon(ICONS_PATH + iconName);
        JButton button = new JButton(text, icon);
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setIconTextGap(10);
        return button;
    }

    /**
     * Loads an icon image from resources and scales it to ICON_SIZE.
     */
    private ImageIcon loadIcon(String resourcePath) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                Image scaled = img.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (IOException ignored) { }
        // If loading fails, return an empty icon
        return new ImageIcon(new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB));
    }

    /**
     * Handler for Add DDS button.
     * User selects one or more DDS files, we parse them and add to the list.
     */
    private void onAdd() {
        JFileChooser chooser = createFileChooser("Select DDS Files", FILTER_DDS, true);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        for (File file : chooser.getSelectedFiles()) {
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                TextureInfo ti = DDSParser.parseBytes(fileBytes);
                ti.setName(stripExtension(file.getName()));
                textures.add(ti);
                listModel.addElement(ti);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot parse DDS: " + file.getName() + "\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        updateCountLabel();
    }

    /**
     * Handler for Remove button.
     * Removes selected list items from the model and from the textures list,
     * and updates the count label.
     */
    private void onRemove() {
        List<TextureInfo> selected = fileList.getSelectedValuesList();
        for (TextureInfo info : selected) {
            textures.removeIf(ti -> ti.getName().equals(info.getName()));
            listModel.removeElement(info);
        }
        updateCountLabel();
    }

    /**
     * Handler for Read CGTEX button.
     * Opens a dialog to select a .cgtex file, reads it and populates texture list,
     * then updates the count label.
     */
    private void onReadCGTEX() {
        JFileChooser chooser = createFileChooser("Select CGTEX File", FILTER_CGTEX, false);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        selectedCgtFile = chooser.getSelectedFile();
        try (CGTEXFile reader = new CGTEXFile(selectedCgtFile, "r")) {
            List<TextureEntry> entries = reader.readFile().getTextures();
            List<TextureInfo> loaded = new ArrayList<>();

            for (TextureEntry entry : entries) {
                TextureInfo info = convertToTextureInfo(entry);
                loaded.add(info);
            }

            textures.clear();
            textures.addAll(loaded);
            refreshFileList(loaded);

            //JOptionPane.showMessageDialog(this, "Loaded CGTEX: " + selectedCgtFile.getName(), "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot read CGTEX: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }

    /**
     * Converts TextureEntry (from .cgtex file) to TextureInfo (for UI).
     */
    private TextureInfo convertToTextureInfo(TextureEntry entry) {
        return new TextureInfo(
                new File(entry.getName()),
                entry.getWidth(),
                entry.getHeight(),
                entry.getName(),
                entry.getFormat(),
                entry.getCompressedData()
        );
    }

    /**
     * Updates the preview panel when a texture is selected from the list.
     * If textureInfo is null, displays an error.
     */
    private void updatePreview(TextureInfo textureInfo) {
        previewPanel.removeAll();
        if (textureInfo == null) {
            //JOptionPane.showMessageDialog(this, "Texture not found.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            try {
                PreviewCell cell = new PreviewCell(textureInfo);
                previewPanel.add(cell, BorderLayout.CENTER);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot display preview: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        previewPanel.revalidate();
        previewPanel.repaint();
    }

    /**
     * Handler for Save .cgtex button.
     * If the list is empty, shows an error. Otherwise saves/overwrites the selected .cgtex file.
     */
    private void onSave() {
        if (textures.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No textures to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedCgtFile == null) {
            JFileChooser chooser = createFileChooser("Save CGTEX File", FILTER_CGTEX, false);
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            selectedCgtFile = chooser.getSelectedFile();
        }

        // Ensure .cgtex extension
        if (!selectedCgtFile.getName().toLowerCase().endsWith("." + FILTER_CGTEX)) {
            selectedCgtFile = new File(
                    selectedCgtFile.getParentFile(),
                    selectedCgtFile.getName() + "." + FILTER_CGTEX
            );
        }

        try (CGTEXFile writer = new CGTEXFile(selectedCgtFile, "rw")) {
            List<TextureEntry> entries = new ArrayList<>();
            for (TextureInfo ti : textures) {
                entries.add(new TextureEntry(
                        ti.getWidth(),
                        ti.getHeight(),
                        ti.getName(),
                        ti.getFormatCode(),
                        ti.getData()
                ));
            }
            writer.writeFile(entries);

            JOptionPane.showMessageDialog(
                    this,
                    "Saved: " + selectedCgtFile.getName(),
                    "OK",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot save CGTEX: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }

    /**
     * Creates and configures a JFileChooser with the required filter.
     *
     * @param dialogTitle Dialog title
     * @param extension   File extension (without dot), e.g. "dds" or "cgtex"
     * @param multiSelect Allow multiple selection?
     * @return Configured JFileChooser
     */
    private JFileChooser createFileChooser(String dialogTitle, String extension, boolean multiSelect) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setMultiSelectionEnabled(multiSelect);
        chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase(), extension));
        return chooser;
    }

    /**
     * Removes the file extension from the name (assumes only one dot).
     */
    private String stripExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx > 0) ? filename.substring(0, idx) : filename;
    }

    public File getSelectedFile() {
        return selectedCgtFile;
    }

    public static void main(String[] args) {
        //FlatDarkLaf.setup(new FlatDarkLaf());
        setupTheme("theme/calista.properties");
        SwingUtilities.invokeLater(() -> {
            CGTEXCreatorUI ui = new CGTEXCreatorUI();
            ui.setVisible(true);
        });
    }

    public static void setupTheme(String theme) {
        try {
            InputStream themeStream = CGTEXCreatorUI.class.getClassLoader().getResourceAsStream(theme);

            if(themeStream == null) {
                throw new RuntimeException("Theme file not found in resources");
            }

            FlatPropertiesLaf laf = new FlatPropertiesLaf("Dark Theme", themeStream);
            FlatLaf.setup(laf);

        } catch(Exception ex) {
            // Fallback на стандартную темную тему
            FlatLaf.setup(new FlatDarkLaf());
            ex.printStackTrace();
        }

    }
}
