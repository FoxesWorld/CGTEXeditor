package org.foxesworld.cge.tools.cgtexEditor.panels;

import org.foxesworld.cge.tools.cgtexEditor.CGTEXCreatorUI;
import org.foxesworld.cge.tools.cgtexEditor.FileExporter;
import org.foxesworld.cge.tools.cgtexEditor.utils.UIUtils;
import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;
import org.foxesworld.cge.tools.cgtexEditor.preview.DDSParser;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileListPanel extends JPanel {
    private static final String FILTER_DDS = "dds";
    private static final int COUNT_PADDING = 10;
    private final List<TextureInfo> textures = new ArrayList<>();
    private final DefaultListModel<TextureInfo> listModel = new DefaultListModel<>();
    private final JList<TextureInfo> fileList = new JList<>(listModel);
    private final JLabel countLabel = new JLabel("0");
    private final JLayeredPane layeredPane;
    private final JTextField filterField = new JTextField(20);

    private List<TextureInfo> allTextures = new ArrayList<>(); // keep the full list for filtering

    public FileListPanel(CGTEXCreatorUI ui) {
        super(new BorderLayout(5,5));
        ui.getAddBtn().addActionListener(e -> onAdd());
        ui.getRemBtn().addActionListener(e -> onRemove());

        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer(new org.foxesworld.cge.tools.cgtexEditor.preview.TextureCellRenderer());

        JPopupMenu menu = new JPopupMenu();
        JMenuItem miCopy = new JMenuItem("Copy name");
        JMenuItem miExport = new JMenuItem("Export File");
        menu.add(miCopy);
        menu.add(miExport);

        fileList.addMouseListener(new MouseAdapter() {
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int idx = fileList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        fileList.setSelectedIndex(idx);
                        menu.show(fileList, e.getX(), e.getY());
                    }
                }
            }
            @Override public void mousePressed(MouseEvent e) { showPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { showPopup(e); }
        });

        miCopy.addActionListener(e -> {
            TextureInfo ti = fileList.getSelectedValue();
            if (ti != null) {
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new StringSelection(ti.getName()), null);
            }
        });

        miExport.addActionListener(e -> {
            TextureInfo ti = fileList.getSelectedValue();
            if (ti != null) {
                FileExporter.exportTexture(this, ti);
            }
        });

        JScrollPane scroll = new JScrollPane(fileList);
        scroll.setBorder(new TitledBorder("Texture Files"));

        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 48f));
        countLabel.setForeground(new Color(0, 0, 0, 80));
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(300, 500));
        layeredPane.setLayout(null);

        scroll.setBounds(0, 0, 300, 500);
        layeredPane.add(scroll, Integer.valueOf(0));
        layeredPane.add(countLabel, Integer.valueOf(1));

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int w = layeredPane.getWidth(), h = layeredPane.getHeight();
                scroll.setBounds(0, 0, w, h);
                Dimension sz = countLabel.getPreferredSize();
                countLabel.setBounds(w - sz.width - COUNT_PADDING,
                        h - sz.height - COUNT_PADDING,
                        sz.width, sz.height);
            }
        });

        // === Filter Panel ===
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // FlatLaf placeholder:
        filterField.putClientProperty("JTextField.placeholderText", "Texture Filter");

        // Create a panel with BorderLayout to hold the filterField and icon inside it
        JPanel filterFieldPanel = new JPanel(new BorderLayout());
        filterFieldPanel.setPreferredSize(new Dimension(220, filterField.getPreferredSize().height + 2));
        filterFieldPanel.add(filterField, BorderLayout.CENTER);

        // FlatLaf search icon
        JLabel searchIconLabel = new JLabel();
        Icon icon = UIManager.getIcon("Search.icon");
        if (icon != null) {
            searchIconLabel.setIcon(icon);
        } else {
            searchIconLabel.setText("\uD83D\uDD0D");
        }
        searchIconLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        searchIconLabel.setCursor(Cursor.getDefaultCursor());
        // The icon will be inside the text field on the right
        filterFieldPanel.add(searchIconLabel, BorderLayout.EAST);

        filterPanel.add(filterFieldPanel);
        add(filterPanel, BorderLayout.NORTH);

        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterList(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterList(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterList(); }
        });

        add(layeredPane, BorderLayout.CENTER);
        refreshFileList(List.of());
    }

    public void refreshFileList(List<TextureInfo> newList) {
        listModel.clear();
        textures.clear();
        allTextures.clear();
        textures.addAll(newList);
        allTextures.addAll(newList);
        // apply filter if filterField is not empty
        filterList();
        countLabel.setText(String.valueOf(textures.size()));
    }

    private void filterList() {
        String filter = filterField.getText().trim().toLowerCase();
        listModel.clear();
        textures.clear();
        List<TextureInfo> filtered;
        if (filter.isEmpty()) {
            filtered = new ArrayList<>(allTextures);
        } else {
            filtered = allTextures.stream()
                    .filter(ti -> ti.getName() != null && ti.getName().toLowerCase().contains(filter))
                    .collect(Collectors.toList());
        }
        textures.addAll(filtered);
        filtered.forEach(listModel::addElement);
        countLabel.setText(String.valueOf(textures.size()));
    }

    public List<TextureInfo> getAllTextures() {
        return new ArrayList<>(allTextures);
    }

    public TextureInfo getSelectedTexture() {
        return fileList.getSelectedValue();
    }

    private void onAdd() {
        JFileChooser chooser = UIUtils.createFileChooser("Select DDS Files", FILTER_DDS, true);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        for (File f : chooser.getSelectedFiles()) {
            try {
                byte[] raw = Files.readAllBytes(f.toPath());
                TextureInfo ti = DDSParser.parseBytes(raw);
                ti.setName(UIUtils.stripExtension(f.getName()));
                ti.setData(raw);
                allTextures.add(ti);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot parse DDS: " + f.getName() + "\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        filterList();
    }

    private void onRemove() {
        for (TextureInfo ti : fileList.getSelectedValuesList()) {
            allTextures.removeIf(t -> t.getName().equals(ti.getName()));
        }
        filterList();
    }

    public JList<TextureInfo> getFileList() {
        return fileList;
    }
}