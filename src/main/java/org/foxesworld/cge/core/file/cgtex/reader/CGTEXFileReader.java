package org.foxesworld.cge.core.file.cgtex.reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.foxesworld.cge.core.file.FileReader;
import org.foxesworld.cge.core.file.cgtex.CGTEXFile;
import org.foxesworld.cge.core.file.cgtex.CGTEXMetadata;
import org.foxesworld.cge.core.file.cgtex.TextureEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a CGTEX file containing DXT textures, including MipMap levels.
 */
public class CGTEXFileReader extends FileReader {
    private static final Logger logger = LogManager.getLogger(CGTEXFileReader.class);

    private final CGTEXMetadata metadata;
    private final List<TextureEntry> textures = new ArrayList<>();

    public CGTEXFileReader(CGTEXFile cgtexFile) throws IOException {
        super(cgtexFile);
        logger.debug("Opening file: {}", cgtexFile.getFile().getAbsolutePath());

        try {
            byte[] allBytes = Files.readAllBytes(cgtexFile.getFile().toPath());
            logger.debug("Full file size = {} bytes", allBytes.length);
        } catch (IOException e) {
            logger.warn("Failed to read full file bytes: {}", e.getMessage());
        }

        this.metadata = readHeader();
        logger.debug("Header parsed: {}", metadata);

        raf.seek(metadata.getDataOffset());

        for (int i = 0; i < metadata.getTextureCount(); i++) {
            int width = raf.readUnsignedShort();
            int height = raf.readUnsignedShort();
            int mipMapCount = raf.readInt();
            if (mipMapCount <= 0) {
                throw new IOException("Invalid mipMapCount (" + mipMapCount + ") for texture index " + i);
            }
            logger.debug("Texture[{}]: width={} height={} mipMapCount={}", i, width, height, mipMapCount);

            int nameLength = raf.readInt();
            if (nameLength < 0) {
                throw new IOException("Invalid nameLength (" + nameLength + ") for texture index " + i);
            }
            byte[] nameBytes = new byte[nameLength];
            raf.readFully(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);
            if (name.isEmpty()) {
                name = "UnnamedTexture_" + i;
            }

            byte format = raf.readByte();

            long align = raf.getFilePointer() % 4;
            if (align != 0) {
                raf.skipBytes((int) (4 - align));
            }

            List<byte[]> mipMapLevels = new ArrayList<>(mipMapCount);
            for (int level = 0; level < mipMapCount; level++) {
                int dataLength = raf.readInt();
                if (dataLength < 0) {
                    throw new IOException("Invalid dataLength (" + dataLength +
                            ") at texture #" + i + ", mip level " + level);
                }
                byte[] levelData = new byte[dataLength];
                raf.readFully(levelData);
                mipMapLevels.add(levelData);
            }

            TextureEntry entry = new TextureEntry(width, height, name, format, mipMapCount, mipMapLevels);
            entry.setMipMapCount(mipMapCount);
            textures.add(entry);
            logger.info("Texture[{}] parsed: {} ({} mip levels)", i, entry.getName(), mipMapCount);
        }

        logger.debug("Finished reading CGTEX file");
    }

    private CGTEXMetadata readHeader() throws IOException {
        raf.seek(0);
        byte[] magicBytes = new byte[4];
        raf.readFully(magicBytes);
        String magic = new String(magicBytes, StandardCharsets.US_ASCII);
        if (!getThisFile().getMAGIC().equals(magic)) {
            throw new IOException("Invalid CGTEX file magic: " + magic);
        }

        int version = raf.readInt();
        int textureCount = raf.readInt();
        if (textureCount < 0) {
            throw new IOException("Invalid textureCount: " + textureCount);
        }

        long dataOffset = raf.readLong();
        if (dataOffset < 0 || dataOffset > raf.length()) {
            throw new IOException("Invalid dataOffset: " + dataOffset);
        }

        long fileSize = raf.length();
        return new CGTEXMetadata(magic, version, textureCount, dataOffset, fileSize);
    }

    public CGTEXMetadata getMetadata() {
        return metadata;
    }

    public List<TextureEntry> getTextures() {
        return List.copyOf(textures);
    }
}
