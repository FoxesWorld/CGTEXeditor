package org.foxesworld.cge.tools.cgtexEditor.preview;

import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * DDSParser provides functionality to parse a DDS file from a byte array
 * and extract its width, height, format code, and compressed data into a TextureInfo.
 */
public class DDSParser {

    private static final int DDS_HEADER_SIZE = 128;
    private static final int SIGNATURE_LENGTH = 4;
    private static final int HEIGHT_OFFSET = 12;
    private static final int WIDTH_OFFSET = 16;
    private static final int FORMAT_CODE_OFFSET = 84;
    private static final int NAME_OFFSET = 88;
    private static final int FORMAT_CODE_LENGTH = 4;
    private static final int NAME_LENGTH = 8;

    /**
     * Parses the given byte array as a DDS file and returns a TextureInfo instance.
     * The method verifies the DDS signature, reads width and height, determines the
     * DXT format (1, 3, or 5), extracts the texture name (up to 8 characters), and
     * copies the remaining compressed data bytes.
     *
     * @param fileBytes a byte array containing the full contents of a DDS file
     * @return a TextureInfo object populated with texture metadata and data
     * @throws IOException if the byte array does not represent a valid or supported DDS file
     */
    public static TextureInfo parseBytes(byte[] fileBytes) throws IOException {
        if (fileBytes == null || fileBytes.length < DDS_HEADER_SIZE) {
            throw new IOException("Invalid DDS data: insufficient length");
        }

        // Verify DDS signature "DDS "
        if (fileBytes[0] != 'D' || fileBytes[1] != 'D' || fileBytes[2] != 'S' || fileBytes[3] != ' ') {
            throw new IOException("Not a DDS file: invalid signature");
        }

        // Wrap the header bytes in a little-endian ByteBuffer for reading integers
        ByteBuffer headerBuffer = ByteBuffer.wrap(fileBytes, 0, DDS_HEADER_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);

        // Read height and width from header offsets
        int height = headerBuffer.getInt(HEIGHT_OFFSET);
        int width = headerBuffer.getInt(WIDTH_OFFSET);

        // Read the four-character format code (e.g., "DXT1", "DXT3", "DXT5")
        String formatCode = new String(
                fileBytes, FORMAT_CODE_OFFSET, FORMAT_CODE_LENGTH, StandardCharsets.UTF_8
        );

        // Determine numeric format identifier
        byte formatId;
        switch (formatCode) {
            case "DXT1" -> formatId = 1;
            case "DXT3" -> formatId = 3;
            case "DXT5" -> formatId = 5;
            default -> throw new IOException("Unsupported DDS format: " + formatCode);
        }

        // Read the texture name (up to 8 ASCII characters, trimmed of trailing nulls/spaces)
        String textureName = new String(
                fileBytes, NAME_OFFSET, NAME_LENGTH, StandardCharsets.UTF_8
        ).trim();

        // Extract compressed texture data starting immediately after the 128-byte header
        byte[] data = Arrays.copyOfRange(fileBytes, DDS_HEADER_SIZE, fileBytes.length);

        return new TextureInfo(
                new File(textureName),
                width,
                height,
                textureName,
                formatId,
                data
        );
    }
}
