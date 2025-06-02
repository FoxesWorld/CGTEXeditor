package org.foxesworld.cge.tools.cgtexEditor.panels;

import org.foxesworld.cge.tools.cgtexEditor.CGTEXCreatorUI;
import org.foxesworld.cge.tools.cgtexEditor.utils.UIUtils;
import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;
import org.foxesworld.cge.tools.cgtexEditor.preview.DDSParser;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * FileListPanel is a JPanel containing a list of TextureInfo items, Add/Remove buttons,
 * and an overlaid count label showing the total number of textures.
 */
public class FileListPanel extends JPanel {

    private static final String FILTER_DDS = "dds";
    private static final int COUNT_PADDING = 10;

    private final List<TextureInfo> textures = new ArrayList<>();
    private final DefaultListModel<TextureInfo> listModel = new DefaultListModel<>();
    private final JList<TextureInfo> fileList = new JList<>(listModel);
    private final JLabel countLabel = new JLabel("0");
    private final JLayeredPane layeredPane;

    /**
     * Constructs a FileListPanel with Add DDS and Remove buttons, a JList for textures,
     * and a layered pane that overlays a large semi-transparent count label.
     */
    public FileListPanel(CGTEXCreatorUI cgtexCreatorUI) {
        super(new BorderLayout(5, 5));


        cgtexCreatorUI.getAddBtn().addActionListener(e -> onAdd());
        cgtexCreatorUI.getRemBtn().addActionListener(e -> onRemove());

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        //buttonsPanel.add(addBtn);
        //buttonsPanel.add(remBtn);

        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer(new org.foxesworld.cge.tools.cgtexEditor.preview.TextureCellRenderer());

        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(new TitledBorder("Texture Files"));

        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 48f));
        countLabel.setForeground(new Color(0, 0, 0, 80));
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(300, 500));
        layeredPane.setLayout(null);

        listScroll.setBounds(0, 0, layeredPane.getPreferredSize().width, layeredPane.getPreferredSize().height);
        layeredPane.add(listScroll, Integer.valueOf(0));
        layeredPane.add(countLabel, Integer.valueOf(1));

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                listScroll.setBounds(0, 0, w, h);
                repositionCountLabel();
            }
        });

        this.add(buttonsPanel, BorderLayout.NORTH);
        this.add(layeredPane, BorderLayout.CENTER);
        refreshFileList(new ArrayList<>());
    }

    /**
     * Replaces the current list model with the provided list of TextureInfo objects
     * and updates the count label accordingly.
     *
     * @param newList the new list of TextureInfo objects to display
     */
    public void refreshFileList(List<TextureInfo> newList) {
        listModel.clear();
        for (TextureInfo info : newList) {
            listModel.addElement(info);
        }
        textures.clear();
        textures.addAll(newList);
        updateCountLabel();
    }

    /**
     * Returns a copy of the current list of TextureInfo objects.
     *
     * @return a List containing all textures in this panel
     */
    public List<TextureInfo> getAllTextures() {
        return new ArrayList<>(textures);
    }

    /**
     * Returns the currently selected TextureInfo from the JList.
     *
     * @return the selected TextureInfo, or null if none is selected
     */
    public TextureInfo getSelectedTexture() {
        return fileList.getSelectedValue();
    }

    /**
     * Returns the underlying JList component that displays TextureInfo items.
     *
     * @return the JList of TextureInfo
     */
    public JList<TextureInfo> getFileList() {
        return fileList;
    }

    /**
     * Opens a file chooser for selecting one or more DDS files, parses each with DDSParser,
     * adds them to the internal list and list model, and updates the count label.
     */
    private void onAdd() {
        JFileChooser chooser = UIUtils.createFileChooser("Select DDS Files", FILTER_DDS, true);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        for (File file : chooser.getSelectedFiles()) {
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                TextureInfo ti = DDSParser.parseBytes(fileBytes);
                ti.setName(UIUtils.stripExtension(file.getName()));
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
     * Removes the selected TextureInfo items from the internal list and list model,
     * then updates the count label.
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
     * Updates the countLabel text to reflect the number of textures and repositions it.
     */
    private void updateCountLabel() {
        countLabel.setText(String.valueOf(textures.size()));
        repositionCountLabel();
    }

    /**
     * Repositions the countLabel in the bottom-right corner of the layered pane.
     */
    private void repositionCountLabel() {
        if (layeredPane == null) {
            return;
        }
        int w = layeredPane.getWidth();
        int h = layeredPane.getHeight();
        Dimension sz = countLabel.getPreferredSize();
        int x = w - sz.width - COUNT_PADDING;
        int y = h - sz.height - COUNT_PADDING;
        countLabel.setBounds(x, y, sz.width, sz.height);
    }
}
