package org.foxesworld.cge.tools.cgtexEditor.panels;

import org.foxesworld.cge.tools.cgtexEditor.CGTEXCreatorUI;
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

    public FileListPanel(CGTEXCreatorUI cgtexCreatorUI) {
        super(new BorderLayout(5, 5));

        cgtexCreatorUI.getAddBtn().addActionListener(e -> onAdd());
        cgtexCreatorUI.getRemBtn().addActionListener(e -> onRemove());

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer(new org.foxesworld.cge.tools.cgtexEditor.preview.TextureCellRenderer());

        // добавляем контекстное меню "Copy name"
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyName = new JMenuItem("Copy name");
        popup.add(copyName);

        fileList.addMouseListener(new MouseAdapter() {
            private void showIfPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int idx = fileList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        fileList.setSelectedIndex(idx);
                        popup.show(fileList, e.getX(), e.getY());
                    }
                }
            }
            @Override public void mousePressed(MouseEvent e) { showIfPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { showIfPopup(e); }
        });

        copyName.addActionListener(e -> {
            TextureInfo ti = fileList.getSelectedValue();
            if (ti != null) {
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new StringSelection(ti.getName()), null);
            }
        });

        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(new TitledBorder("Texture Files"));

        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 48f));
        countLabel.setForeground(new Color(0, 0, 0, 80));
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(300, 500));
        layeredPane.setLayout(null);

        listScroll.setBounds(0, 0,
                layeredPane.getPreferredSize().width,
                layeredPane.getPreferredSize().height);
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

        add(buttonsPanel, BorderLayout.NORTH);
        add(layeredPane, BorderLayout.CENTER);
        refreshFileList(new ArrayList<>());
    }

    public void refreshFileList(List<TextureInfo> newList) {
        listModel.clear();
        textures.clear();
        textures.addAll(newList);
        for (TextureInfo info : newList) {
            listModel.addElement(info);
        }
        updateCountLabel();
    }

    public List<TextureInfo> getAllTextures() {
        return new ArrayList<>(textures);
    }

    public TextureInfo getSelectedTexture() {
        return fileList.getSelectedValue();
    }

    public JList<TextureInfo> getFileList() {
        return fileList;
    }

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
                ti.setData(fileBytes);
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

    private void onRemove() {
        List<TextureInfo> selected = fileList.getSelectedValuesList();
        for (TextureInfo info : selected) {
            textures.removeIf(ti -> ti.getName().equals(info.getName()));
            listModel.removeElement(info);
        }
        updateCountLabel();
    }

    private void updateCountLabel() {
        countLabel.setText(String.valueOf(textures.size()));
        repositionCountLabel();
    }

    private void repositionCountLabel() {
        int w = layeredPane.getWidth();
        int h = layeredPane.getHeight();
        Dimension sz = countLabel.getPreferredSize();
        int x = w - sz.width - COUNT_PADDING;
        int y = h - sz.height - COUNT_PADDING;
        countLabel.setBounds(x, y, sz.width, sz.height);
    }
}
