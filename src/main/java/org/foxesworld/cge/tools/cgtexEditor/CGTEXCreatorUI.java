package org.foxesworld.cge.tools.cgtexEditor;

import org.foxesworld.cge.core.file.cgtex.CGTEXFile;
import org.foxesworld.cge.core.file.cgtex.TextureEntry;
import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CGTEXCreatorUI is the main application window that combines a FileListPanel
 * for managing DDS textures, a PreviewPanel for displaying the selected texture,
 * and buttons to read from or save to a .cgtex file.
 */
public class CGTEXCreatorUI extends JFrame {

    private static final String FILTER_CGTEX = "cgtex";

    private final FileListPanel fileListPanel = new FileListPanel();
    private final PreviewPanel previewPanel = new PreviewPanel();
    private File selectedCgtFile;

    /**
     * Constructs the main UI window, sets up frame properties, window icon,
     * and initializes all UI components.
     */
    public CGTEXCreatorUI() {
        super("CGTEX Creator");
        configureFrame();
        UIUtils.setFrameIconFromICO(this);
        initUI();
    }

    /**
     * Configures basic JFrame properties such as size, close operation,
     * and debug-related system properties.
     */
    private void configureFrame() {
        System.setProperty("log.dir", System.getProperty("user.dir"));
        System.setProperty("log.level", "DEBUG");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    /**
     * Initializes and arranges UI components:
     * <ul>
     *     <li>A "Read CGTEX" button at the top to open and parse a .cgtex file.</li>
     *     <li>A JSplitPane dividing the FileListPanel (left) and PreviewPanel (right).</li>
     *     <li>A "Save .cgtex" button at the bottom to write out the current textures.</li>
     * </ul>
     * Also connects list selection events to update the preview.
     */
    private void initUI() {
        JButton readBtn = new JButton("Read CGTEX", UIUtils.loadIcon("read_icon.png"));
        readBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        readBtn.setIconTextGap(10);
        readBtn.addActionListener(e -> onReadCGTEX());

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topButtons.add(readBtn);

        fileListPanel.getFileList().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                TextureInfo info = fileListPanel.getSelectedTexture();
                previewPanel.updatePreview(info);
            }
        });

        JPanel leftWrapper = new JPanel(new BorderLayout());
        leftWrapper.add(fileListPanel, BorderLayout.CENTER);

        JScrollPane previewScroll = new JScrollPane(previewPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftWrapper, previewScroll);
        splitPane.setResizeWeight(0.3);

        JButton saveBtn = new JButton("Save .cgtex", UIUtils.loadIcon("save_icon.png"));
        saveBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        saveBtn.setIconTextGap(10);
        saveBtn.addActionListener(e -> onSaveCGTEX());

        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bottomButtons.add(saveBtn);

        Container cp = getContentPane();
        cp.setLayout(new BorderLayout(5, 5));
        cp.add(topButtons, BorderLayout.NORTH);
        cp.add(splitPane, BorderLayout.CENTER);
        cp.add(bottomButtons, BorderLayout.SOUTH);
    }

    /**
     * Handles the "Read CGTEX" action by opening a file chooser to select a .cgtex file,
     * reading its contents via CGTEXFile, converting each TextureEntry to a TextureInfo,
     * and populating the FileListPanel with the loaded textures.
     */
    private void onReadCGTEX() {
        JFileChooser chooser = UIUtils.createFileChooser("Select CGTEX File", FILTER_CGTEX, false);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        selectedCgtFile = chooser.getSelectedFile();

        try (CGTEXFile reader = new CGTEXFile(selectedCgtFile, "r")) {
            List<TextureEntry> entries = reader.readFile().getTextures();
            List<TextureInfo> loaded = new ArrayList<>(entries.size());
            for (TextureEntry entry : entries) {
                TextureInfo info = new TextureInfo(
                        new File(entry.getName()),
                        entry.getWidth(),
                        entry.getHeight(),
                        entry.getName(),
                        entry.getFormat(),
                        entry.getCompressedData()
                );
                loaded.add(info);
            }
            fileListPanel.refreshFileList(loaded);
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
     * Handles the "Save .cgtex" action by gathering all TextureInfo instances
     * from the FileListPanel, prompting the user for a target .cgtex file if necessary,
     * and writing out a new CGTEXFile containing TextureEntry objects converted from TextureInfo.
     */
    private void onSaveCGTEX() {
        List<TextureInfo> textures = fileListPanel.getAllTextures();
        if (textures.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No textures to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedCgtFile == null) {
            JFileChooser chooser = UIUtils.createFileChooser("Save CGTEX File", FILTER_CGTEX, false);
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            selectedCgtFile = chooser.getSelectedFile();
        }

        if (!selectedCgtFile.getName().toLowerCase().endsWith("." + FILTER_CGTEX)) {
            selectedCgtFile = new File(
                    selectedCgtFile.getParentFile(),
                    selectedCgtFile.getName() + "." + FILTER_CGTEX
            );
        }

        try (CGTEXFile writer = new CGTEXFile(selectedCgtFile, "rw")) {
            List<TextureEntry> entries = new ArrayList<>(textures.size());
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
     * The entry point of the application, which sets up the theme and launches the main UI.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        UIUtils.setupTheme("theme/calista.properties");
        SwingUtilities.invokeLater(() -> {
            CGTEXCreatorUI ui = new CGTEXCreatorUI();
            ui.setVisible(true);
        });
    }
}
