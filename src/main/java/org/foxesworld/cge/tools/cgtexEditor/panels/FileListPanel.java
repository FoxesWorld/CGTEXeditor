package org.foxesworld.cge.tools.cgtexEditor.panels;

import org.foxesworld.cge.tools.cgtexEditor.CGTEXCreatorUI;
import org.foxesworld.cge.tools.cgtexEditor.utils.UIUtils;
import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;
import org.foxesworld.cge.tools.cgtexEditor.preview.DDSParser;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileListPanel extends JPanel {
    private static final String FILTER_DDS     = "dds";
    private static final int    PREF_WIDTH     = 300;
    private static final int    PREF_HEIGHT    = 500;
    private static final int    COUNT_PADDING  = 10;

    private final List<TextureInfo> textures  = new ArrayList<>();
    private final DefaultListModel<TextureInfo> listModel = new DefaultListModel<>();
    private final JList<TextureInfo> fileList = new JList<>(listModel);
    private final JLabel countLabel          = new JLabel("0");
    private final JLayeredPane layeredPane;

    public FileListPanel(CGTEXCreatorUI ui) {
        super(new BorderLayout(5,5));

        ui.getAddBtn().addActionListener(e -> onAdd());
        ui.getRemBtn().addActionListener(e -> onRemove());

        setupFileList();
        layeredPane = setupLayeredPane();

        add(new JPanel(new FlowLayout(FlowLayout.LEFT)), BorderLayout.NORTH);
        add(layeredPane, BorderLayout.CENTER);
        refreshFileList(List.of());
    }

    private void setupFileList() {
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer(new org.foxesworld.cge.tools.cgtexEditor.preview.TextureCellRenderer());
        fileList.setComponentPopupMenu(createPopupMenu());
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miCopy   = new JMenuItem("Copy name");
        JMenuItem miExport = new JMenuItem("Export");

        miCopy.addActionListener(e -> copyName());
        miExport.addActionListener(e -> exportSelected());

        menu.add(miCopy);
        menu.add(miExport);
        return menu;
    }

    private JLayeredPane setupLayeredPane() {
        JScrollPane scroll = new JScrollPane(fileList);
        scroll.setBorder(new TitledBorder("Texture Files"));
        scroll.setBounds(0,0,PREF_WIDTH,PREF_HEIGHT);

        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD,48f));
        countLabel.setForeground(new Color(0,0,0,80));
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JLayeredPane pane = new JLayeredPane();
        pane.setPreferredSize(new Dimension(PREF_WIDTH,PREF_HEIGHT));
        pane.setLayout(null);
        pane.add(scroll, Integer.valueOf(0));
        pane.add(countLabel, Integer.valueOf(1));

        pane.addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension size = pane.getSize();
                scroll.setBounds(0,0,size.width,size.height);
                repositionCount(size.width, size.height);
            }
        });

        return pane;
    }

    private void repositionCount(int w, int h) {
        Dimension sz = countLabel.getPreferredSize();
        countLabel.setBounds(w - sz.width - COUNT_PADDING,
                h - sz.height- COUNT_PADDING,
                sz.width, sz.height);
    }

    public void refreshFileList(List<TextureInfo> newList) {
        listModel.clear();
        textures.clear();
        textures.addAll(newList);
        newList.forEach(listModel::addElement);
        updateCount();
    }

    private void updateCount() {
        countLabel.setText(String.valueOf(textures.size()));
        repositionCount(layeredPane.getWidth(), layeredPane.getHeight());
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
                JOptionPane.showMessageDialog(this,
                        "Cannot parse DDS: " + f.getName() + "\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        updateCount();
    }

    private void onRemove() {
        for (TextureInfo ti : fileList.getSelectedValuesList()) {
            textures.removeIf(t -> t.getName().equals(ti.getName()));
            listModel.removeElement(ti);
        }
        updateCount();
    }

    private void copyName() {
        TextureInfo ti = getSelectedTexture();
        if (ti != null) {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(ti.getName()), null);
        }
    }

    private void exportSelected() {
        TextureInfo ti = getSelectedTexture();
        if (ti == null) return;

        String[] formats = {"PNG", "DDS"};
        String fmt = (String) JOptionPane.showInputDialog(
                this,
                "Choose format:",
                "Export format",
                JOptionPane.PLAIN_MESSAGE,
                null,
                formats,
                formats[0]
        );
        if (fmt == null) return;

        String ext = fmt.toLowerCase();
        JFileChooser chooser = UIUtils.createFileChooser("Save as " + fmt, ext, false);
        chooser.setSelectedFile(new File(ti.getName() + "." + ext));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            if ("DDS".equals(fmt)) {
                writeDDS(ti, chooser.getSelectedFile());
            } else {
                ImageIO.write(ti.getPreviewImage(), ext, chooser.getSelectedFile());
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Export error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void writeDDS(TextureInfo ti, File out) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(createDDSHeader(ti));
            fos.write(ti.getData());
        }
    }

    private static byte[] createDDSHeader(TextureInfo ti) {
        int w = ti.getWidth(), h = ti.getHeight();
        byte fmt = ti.getFormatCode();
        int linear = Math.max(1, ((w+3)/4)) * Math.max(1, ((h+3)/4)) * 16;
        ByteBuffer b = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
        // magic, size, flags, dims, linearSize, zero mipcount & reserved
        b.putInt(0x20534444).putInt(124).putInt(0x0002100F)
                .putInt(h).putInt(w).putInt(linear).putInt(0).putInt(0);
        for(int i=0;i<11;i++) b.putInt(0);
        // pixel format
        b.putInt(32).putInt(0x4)
                .putInt(switch(fmt) {
                    case 1 -> 0x31545844; case 3 -> 0x33545844;
                    case 5 -> 0x35545844; default -> 0;
                })
                .putInt(0).putInt(0).putInt(0).putInt(0);
        // caps
        b.putInt(0x1000).putInt(0).putInt(0).putInt(0).putInt(0);
        return b.array();
    }

    public JList<TextureInfo> getFileList() {
        return fileList;
    }
}
