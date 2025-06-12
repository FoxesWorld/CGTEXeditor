package org.foxesworld.cge.tools.cgtexEditor;

import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class FileExporter {

    public interface ExportHandler extends BiConsumer<TextureInfo, File> {}

    private static final Map<String, ExportFormat> formats = new LinkedHashMap<>();

    static {
        registerFormat("png", "PNG Image (*.png)", (ti, file) -> {
            BufferedImage img = ti.getPreviewImage();
            try {
                ImageIO.write(img, "png", file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        registerFormat("dds", "DirectDraw Surface (*.dds)", (ti, file) -> {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(createDDSHeader(ti));
                fos.write(ti.getData());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void registerFormat(String ext, String description, ExportHandler handler) {
        formats.put(ext, new ExportFormat(ext, description, handler));
    }

    public static void exportTexture(Component parent, TextureInfo ti) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save as...");
        chooser.setAcceptAllFileFilterUsed(false);

        Map<FileNameExtensionFilter, ExportFormat> filterMap = new LinkedHashMap<>();
        for (ExportFormat fmt : formats.values()) {
            FileNameExtensionFilter filter = new FileNameExtensionFilter(fmt.description(), fmt.extension());
            chooser.addChoosableFileFilter(filter);
            filterMap.put(filter, fmt);
        }

        // Default to first format
        chooser.setFileFilter(filterMap.keySet().iterator().next());
        chooser.setSelectedFile(new File(ti.getName()));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File selectedFile = chooser.getSelectedFile();
        ExportFormat selectedFormat = filterMap.get(chooser.getFileFilter());

        String ext = selectedFormat.extension();
        if (!selectedFile.getName().toLowerCase().endsWith("." + ext)) {
            selectedFile = new File(selectedFile.getAbsolutePath() + "." + ext);
        }

        try {
            selectedFormat.handler().accept(ti, selectedFile);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Export error: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static byte[] createDDSHeader(TextureInfo ti) {
        int height = ti.getHeight();
        int width  = ti.getWidth();
        byte fmt   = ti.getFormatCode();

        int linearSize = Math.max(1, ((width + 3) / 4)) * Math.max(1, ((height + 3) / 4)) * 16;

        ByteBuffer buf = ByteBuffer.allocate(128);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte) 'D'); // 'D'
        buf.put((byte) 'D'); // 'D'
        buf.put((byte) 'S'); // 'S'
        buf.put((byte) ' '); // ' '

        buf.putInt(124);         // dwSize
        buf.putInt(0x0002100F);  // dwFlags
        buf.putInt(height);      // dwHeight
        buf.putInt(width);       // dwWidth
        buf.putInt(linearSize);  // dwPitchOrLinearSize
        buf.putInt(0);           // dwDepth
        buf.putInt(0);           // dwMipMapCount

        for (int i = 0; i < 11; i++) {
            buf.putInt(0);       // dwReserved1[11]
        }

        // DDS_PIXELFORMAT
        buf.putInt(32);          // dwSize
        buf.putInt(0x00000004);  // dwFlags: DDPF_FOURCC

        int fourCC = switch (fmt) {
            case 1 -> 0x31545844; // DXT1
            case 3 -> 0x33545844; // DXT3
            case 5 -> 0x35545844; // DXT5
            default -> 0;
        };
        buf.putInt(fourCC);      // dwFourCC

        buf.putInt(0);           // dwRGBBitCount
        buf.putInt(0);           // dwRBitMask
        buf.putInt(0);           // dwGBitMask
        buf.putInt(0);           // dwBBitMask
        buf.putInt(0);           // dwABitMask

        buf.putInt(0x1000);      // dwCaps1: DDSCAPS_TEXTURE
        buf.putInt(0);           // dwCaps2
        buf.putInt(0);           // dwCaps3
        buf.putInt(0);           // dwCaps4

        buf.putInt(0);           // dwReserved2

        return buf.array();
    }


    private record ExportFormat(String extension, String description, ExportHandler handler) {}
}