package org.foxesworld.cge.tools.cgtexEditor;

import org.foxesworld.cge.core.file.cgtex.CGTEXFile;
import org.foxesworld.cge.core.file.cgtex.TextureEntry;
import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;
import org.foxesworld.cge.tools.cgtexEditor.panels.FileListPanel;
import org.foxesworld.cge.tools.cgtexEditor.panels.PreviewPanel;
import org.foxesworld.cge.tools.cgtexEditor.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Main application window for creating and editing .cgtex files.
 * <p>
 * Combines:
 * <ul>
 *     <li>FileListPanel for managing DDS textures</li>
 *     <li>PreviewPanel for displaying the selected texture</li>
 *     <li>Buttons to read from/save to a .cgtex file and to add/remove DDS textures</li>
 * </ul>
 * All JButton instances are created via the createButton(...) factory to eliminate redundant code.
 */
public class CGTEXCreatorUI extends JFrame {
    private static final String FILTER_CGTEX = "cgtex";
    private JButton addBtn, remBtn, readBtn, saveBtn;
    private final FileListPanel fileListPanel;
    private final PreviewPanel previewPanel;
    private File selectedCgtFile;

    /**
     * Constructs the main UI, configures frame properties, initializes buttons,
     * and builds the user interface.
     */
    public CGTEXCreatorUI() {
        super("CGTEX Creator");
        configureFrame();
        initButtons();
        this.previewPanel = new PreviewPanel(this);
        this.fileListPanel = new FileListPanel(this);
        UIUtils.setFrameIconFromICO(this);
        initUI();
    }

    /**
     * Configures basic JFrame properties such as size, close operation, and logging system properties.
     */
    private void configureFrame() {
        //System.setProperty("log.dir", System.getProperty("%APPDATA%/cgtex"));
        initLogDirectory("APPDATA/FoxesWorld/CGTEX");
        System.setProperty("log.level", "DEBUG");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    /**
     * Initializes all buttons using the createButton factory method.
     */
    private void initButtons() {
        saveBtn = createButton(
                "Save .cgtex",
                "save_icon.png",
                "#336699",
                e -> onSaveCGTEX(),
                true
        );

        readBtn = createButton(
                "Read CGTEX",
                "read_icon.png",
                "#2ccfb7",
                e -> onReadCGTEX(),
                true
        );

        addBtn = createButton(
                "Add DDS",
                "add_icon.png",
                "#1bcc36bd",
                null,
                true
        );

        remBtn = createButton(
                "Remove",
                "remove_icon.png",
                "#c51e3ebd",
                null,
                false
        );
    }

    /**
     * Factory method for creating a JButton with consistent styling.
     *
     * @param text     the button label
     * @param iconName the filename for the icon (loaded via UIUtils.loadIcon)
     * @param bgColor  the background color in hex format (e.g., "#336699")
     * @param listener ActionListener for the button (may be null)
     * @param enabled  initial enabled state of the button
     * @return a configured JButton instance
     */
    private JButton createButton(String text, String iconName, String bgColor, ActionListener listener, boolean enabled) {
        JButton btn = new JButton(text, UIUtils.loadIcon(iconName));
        btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        btn.setIconTextGap(10);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("FlatLaf.style", "background: " + bgColor + ";");
        btn.setEnabled(enabled);
        if (listener != null) {
            btn.addActionListener(listener);
        }
        return btn;
    }

    /**
     * Builds and arranges the UI components:
     * <ul>
     *     <li>Top panel with Read and Save buttons</li>
     *     <li>Split pane separating FileListPanel (left) and PreviewPanel (right)</li>
     *     <li>Bottom panel with Add and Remove buttons</li>
     * </ul>
     */
    private void initUI() {
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topButtons.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "File Actions",
                TitledBorder.LEADING,
                TitledBorder.TOP
        ));
        topButtons.add(readBtn);
        topButtons.add(saveBtn);

        fileListPanel.getFileList().addListSelectionListener(e -> {
            remBtn.setEnabled(this.fileListPanel.getSelectedTexture() != null);
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

        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bottomButtons.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Texture Actions",
                TitledBorder.LEADING,
                TitledBorder.TOP
        ));
        bottomButtons.add(addBtn);
        bottomButtons.add(remBtn);

        Container cp = getContentPane();
        cp.setLayout(new BorderLayout(5, 5));
        cp.add(topButtons, BorderLayout.NORTH);
        cp.add(splitPane, BorderLayout.CENTER);
        cp.add(bottomButtons, BorderLayout.SOUTH);
    }

    /**
     * Handles the "Read CGTEX" button action:
     * Opens a file chooser, reads the selected .cgtex file,
     * converts TextureEntry objects to TextureInfo, and populates the FileListPanel.
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
     * Handles the "Save .cgtex" button action:
     * Gathers all TextureInfo instances from the FileListPanel,
     * prompts for a target .cgtex file if necessary, converts TextureInfo to TextureEntry,
     * and writes them using CGTEXFile.
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
     * Application entry point. Sets up the theme and launches the UI on the Event Dispatch Thread.
     *
     * @param args unused command-line arguments
     */
    public static void main(String[] args) {
        UIUtils.setupTheme("theme/calista.properties");
        SwingUtilities.invokeLater(() -> {
            CGTEXCreatorUI ui = new CGTEXCreatorUI();
            ui.setVisible(true);
        });
    }

    public void initLogDirectory(String pathWithEnv) {
        String[] parts = pathWithEnv.split("[/\\\\]+");
        StringJoiner pathBuilder = new StringJoiner(File.separator);

        for (String part : parts) {
            if (part.equalsIgnoreCase("APPDATA")) {
                String appData = System.getenv("APPDATA");
                if (appData == null) {
                    throw new IllegalStateException("APPDATA переменная не определена в системе.");
                }
                pathBuilder.add(appData);
            } else {
                pathBuilder.add(part);
            }
        }

        File logDir = new File(pathBuilder.toString());

        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (!created) {
                throw new RuntimeException("Не удалось создать директорию: " + logDir.getAbsolutePath());
            }
        }

        System.setProperty("log.dir", logDir.getAbsolutePath());
    }

    public JButton getAddBtn() {
        return addBtn;
    }

    public JButton getRemBtn() {
        return remBtn;
    }
}