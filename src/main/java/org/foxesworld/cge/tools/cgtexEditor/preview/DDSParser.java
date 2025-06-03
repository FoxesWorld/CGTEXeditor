package org.foxesworld.cge.tools.cgtexEditor.preview;

import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Parses a DDS file from a byte array and extracts width, height, format, mipmap count,
 * texture name, and compressed data into a TextureInfo.
 */
public class DDSParser {
    private static final int DDS_HEADER_SIZE = 128;
    private static final int SIGNATURE_LENGTH = 4;
    private static final int HEIGHT_OFFSET = 12;
    private static final int WIDTH_OFFSET = 16;
    private static final int MIPMAP_COUNT_OFFSET = 28;
    private static final int FORMAT_CODE_OFFSET = 84;
    private static final int FORMAT_CODE_LENGTH = 4;
    private static final int NAME_OFFSET = 88;
    private static final int NAME_LENGTH = 8;

    /**
     * Parses the given byte array as a DDS file and returns a TextureInfo instance.
     *
     * @param fileBytes a byte array containing the full contents of a DDS file
     * @return a TextureInfo object populated with texture metadata, mipmap count, and data
     * @throws IOException if the byte array does not represent a valid or supported DDS file
     */
    public static TextureInfo parseBytes(byte[] fileBytes) throws IOException {
        if (fileBytes == null || fileBytes.length < DDS_HEADER_SIZE) {
            throw new IOException("Invalid DDS data: insufficient length");
        }

        if (fileBytes[0] != 'D' || fileBytes[1] != 'D' || fileBytes[2] != 'S' || fileBytes[3] != ' ') {
            throw new IOException("Not a DDS file: invalid signature");
        }

        ByteBuffer headerBuffer = ByteBuffer.wrap(fileBytes, 0, DDS_HEADER_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);

        int height = headerBuffer.getInt(HEIGHT_OFFSET);
        int width = headerBuffer.getInt(WIDTH_OFFSET);
        int mipMapCount = headerBuffer.getInt(MIPMAP_COUNT_OFFSET);

        String formatCode = new String(
                fileBytes, FORMAT_CODE_OFFSET, FORMAT_CODE_LENGTH, StandardCharsets.UTF_8
        );
        byte formatId;
        switch (formatCode) {
            case "DXT1" -> formatId = 1;
            case "DXT3" -> formatId = 3;
            case "DXT5" -> formatId = 5;
            default -> throw new IOException("Unsupported DDS format: " + formatCode);
        }

        String textureName = new String(
                fileBytes, NAME_OFFSET, NAME_LENGTH, StandardCharsets.UTF_8
        ).trim();

        byte[] data = Arrays.copyOfRange(fileBytes, DDS_HEADER_SIZE, fileBytes.length);

        TextureInfo info = new TextureInfo(
                new File(textureName),
                width,
                height,
                textureName,
                formatId,
                data
        );
        info.setMipMapCount(mipMapCount);
        return info;
    }
}
