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

public class FileListPanel extends JPanel {
    private static final String FILTER_DDS = "dds";
    private static final int COUNT_PADDING = 10;
    private final List<TextureInfo> textures = new ArrayList<>();
    private final DefaultListModel<TextureInfo> listModel = new DefaultListModel<>();
    private final JList<TextureInfo> fileList = new JList<>(listModel);
    private final JLabel countLabel = new JLabel("0");
    private final JLayeredPane layeredPane;

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

        add(new JPanel(new FlowLayout(FlowLayout.LEFT)), BorderLayout.NORTH);
        add(layeredPane, BorderLayout.CENTER);
        refreshFileList(List.of());
    }

    public void refreshFileList(List<TextureInfo> newList) {
        listModel.clear();
        textures.clear();
        textures.addAll(newList);
        newList.forEach(listModel::addElement);
        countLabel.setText(String.valueOf(textures.size()));
    }

    public List<TextureInfo> getAllTextures() {
        return new ArrayList<>(textures);
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
                textures.add(ti);
                listModel.addElement(ti);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot parse DDS: " + f.getName() + "\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        countLabel.setText(String.valueOf(textures.size()));
    }

    private void onRemove() {
        for (TextureInfo ti : fileList.getSelectedValuesList()) {
            textures.removeIf(t -> t.getName().equals(ti.getName()));
            listModel.removeElement(ti);
        }
        countLabel.setText(String.valueOf(textures.size()));
    }

    public JList<TextureInfo> getFileList() {
        return fileList;
    }
}
