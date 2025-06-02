package org.foxesworld.cge.core.file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Base file handler: manages opening/closing RandomAccessFile and provides
 * common utility methods for reading/writing bytes, strings and primitive types.
 */
public abstract class AbstractFile implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(AbstractFile.class);
    protected final RandomAccessFile raf;
    private String MAGIC;
    private int VERSION;
    private int MAX_NAME_LENGTH = 4096;
    private ByteOrder BYTE_ORDER    = ByteOrder.LITTLE_ENDIAN;
    private final File file;

    protected AbstractFile(File file, String mode) {
        this.file = file;
        this.raf  = openRandomAccess(file, mode);
    }

    protected abstract FileReader readFile();
    //protected abstract FileWriter writeFile();

    private RandomAccessFile openRandomAccess(File f, String mode) {
        try {
            return new RandomAccessFile(f, mode);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Cannot open file: " + f.getAbsolutePath(), e);
        }
    }

    public void writeVariableLengthString(String value) throws IOException {
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        int length = valueBytes.length;
        raf.writeInt(length);
        raf.write(valueBytes);
    }


    public void seek(long position) throws IOException {
        raf.seek(position);
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] data = new byte[length];
        raf.readFully(data);
        return data;
    }

    protected void writeBytes(byte[] data) throws IOException {
        raf.write(data);
    }

    public int readInt() throws IOException {
        return raf.readInt();
    }

    protected void writeInt(int value) throws IOException {
        raf.writeInt(value);
    }

    public long readLong() throws IOException {
        return raf.readLong();
    }

    protected void writeLong(long value) throws IOException {
        raf.writeLong(value);
    }

    public String readString(int maxLength) throws IOException {
        int len = raf.readInt();
        if (len < 0 || len > maxLength) {
            throw new IOException("Invalid string length: " + len);
        }
        byte[] data = new byte[len];
        raf.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    protected void writeString(String s) throws IOException {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        raf.writeInt(data.length);
        raf.write(data);
    }

    public File getFile() {
        return file;
    }

    public RandomAccessFile getRaf() {
        return raf;
    }

    @Override
    public void close() throws IOException {
        try {
            raf.close();
            logger.info("Closing {}", this.file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setMAGIC(String MAGIC) {
        this.MAGIC = MAGIC;
    }

    public void setVERSION(int VERSION) {
        this.VERSION = VERSION;
    }

    public void setMAX_NAME_LENGTH(int MAX_NAME_LENGTH) {
        this.MAX_NAME_LENGTH = MAX_NAME_LENGTH;
    }

    public void setBYTE_ORDER(ByteOrder BYTE_ORDER) {
        this.BYTE_ORDER = BYTE_ORDER;
    }

    public String getMAGIC() {
        return MAGIC;
    }

    public int getVERSION() {
        return VERSION;
    }

    public int getMAX_NAME_LENGTH() {
        return MAX_NAME_LENGTH;
    }

    public ByteOrder getBYTE_ORDER() {
        return BYTE_ORDER;
    }
}