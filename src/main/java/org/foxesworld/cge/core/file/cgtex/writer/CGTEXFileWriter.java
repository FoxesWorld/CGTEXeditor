package org.foxesworld.cge.core.file.cgtex.writer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.foxesworld.cge.core.file.FileWriter;
import org.foxesworld.cge.core.file.cgtex.CGTEXFile;
import org.foxesworld.cge.core.file.cgtex.TextureEntry;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes CGTEX files containing compressed textures with support for MipMap levels.
 */
public class CGTEXFileWriter extends FileWriter {
    private static final Logger logger = LogManager.getLogger(CGTEXFileWriter.class);

    private final CGTEXFile cgtexFile;
    private final RandomAccessFile raf;
    private final List<TextureEntry> textures;

    /**
     * Constructs a new writer for the given CGTEXFile.
     *
     * @param cgtexFile the CGTEXFile instance providing file, RAF, MAGIC, and VERSION
     */
    public CGTEXFileWriter(CGTEXFile cgtexFile) {
        this.cgtexFile = cgtexFile;
        this.raf = cgtexFile.getRaf();
        this.textures = new ArrayList<>();
    }

    /**
     * Adds a texture entry to the list of textures to be written.
     *
     * @param textureEntry the texture entry containing width, height, name, format, mipMapCount, and mipMapLevels
     */
    public void addTexture(TextureEntry textureEntry) {
        this.textures.add(textureEntry);
    }

    /**
     * Writes the CGTEX file according to the specification, including MipMapCount and all level data.
     *
     * @throws IOException           if an I/O error occurs during writing
     * @throws IllegalStateException if no textures have been added
     */
    public void writeFile() throws IOException {
        if (textures.isEmpty()) {
            throw new IllegalStateException("No textures to write");
        }

        raf.setLength(0);
        logger.info("Writing CGTEX to '{}'", cgtexFile.getFile().getAbsolutePath());

        raf.seek(0);
        raf.writeBytes(cgtexFile.getMAGIC());
        raf.writeInt(cgtexFile.getVERSION());
        raf.writeInt(textures.size());

        long dataOffsetPos = raf.getFilePointer();
        raf.writeLong(0L);

        long dataOffset = raf.getFilePointer();
        logger.debug("DataOffset (actual) = {}", dataOffset);

        for (int i = 0; i < textures.size(); i++) {
            TextureEntry tex = textures.get(i);
            int mipMapCount = tex.getMipMapCount();
            logger.info("Writing texture [{}] '{}' with {} MipMap levels", i, tex.getName(), mipMapCount);

            raf.writeShort((short) tex.getWidth());
            raf.writeShort((short) tex.getHeight());

            if (mipMapCount <= 0) {
                throw new IOException("Invalid mipMapCount (" + mipMapCount + ") for texture index " + i);
            }
            raf.writeInt(mipMapCount);

            byte[] nameBytes = tex.getName().getBytes(StandardCharsets.UTF_8);
            raf.writeInt(nameBytes.length);
            raf.write(nameBytes);

            raf.writeByte(tex.getFormat());

            long curOffset = raf.getFilePointer();
            long padding = (4 - (curOffset % 4)) % 4;
            if (padding > 0) {
                for (int p = 0; p < padding; p++) {
                    raf.writeByte(0);
                }
                logger.debug("Added {} padding bytes (offset now {})", padding, raf.getFilePointer());
            }

            List<byte[]> levels = tex.getMipMapLevels();
            if (levels.size() != mipMapCount) {
                throw new IOException("Mismatch between mipMapCount and actual levels list size for texture index " + i);
            }
            for (int level = 0; level < mipMapCount; level++) {
                byte[] levelData = levels.get(level);
                if (levelData == null) {
                    throw new IOException("Null data for mipmap level " + level + " of texture index " + i);
                }
                raf.writeInt(levelData.length);
                raf.write(levelData);
                logger.debug("Level {}: wrote DataLength={} bytes", level, levelData.length);
            }

            logger.info("Texture[{}] completely written (offset_end = {})", i, raf.getFilePointer());
        }

        raf.seek(dataOffsetPos);
        raf.writeLong(dataOffset);
        logger.debug("Patched DataOffset = {} at position {}", dataOffset, dataOffsetPos);

        logger.info("CGTEX written successfully: dataOffset={}, textureCount={}", dataOffset, textures.size());
    }
}
