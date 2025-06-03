package org.foxesworld.cge.tools.cgtexEditor.info;

import org.foxesworld.cge.tools.cgtexEditor.preview.DDSDecoder;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Класс для хранения информации о одной текстуре в UI:
 *  • Исходный файл (DDS или путь в CGTEX-архиве)
 *  • Ширина и высота базового уровня (уровень 0)
 *  • Имя (строка) — используется при записи в CGTEX-архив
 *  • Код формата (например, DXT1, DXT3, DXT5 и т.п.)
 *  • Данные базового уровня (байтовый массив)
 *  • Количество уровней MipMap (mipMapCount)
 *  • Кешированное предварительное изображение (BufferedImage) для отображения в PreviewPanel
 */
public class TextureInfo {
    /** Ссылка на файл (DDS-файл на диске или просто контейнер для имени внутри CGTEX) */
    private final File file;

    /** Ширина базового уровня (level 0) в пикселях */
    private final int width;

    /** Высота базового уровня (level 0) в пикселях */
    private final int height;

    /**
     * Строковое имя текстуры.
     * При чтении из CGTEX оно соответствует именованию внутри архива.
     * При создании новой текстуры на основе DDS—файла мы можем установить его равным имени файла (без расширения).
     */
    private String name;

    /** Код формата (DXT1=0, DXT3=1, DXT5=2 и т.п.) */
    private final byte formatCode;

    /** Данные базового уровня (байтовый массив, например, DXT-блоки или несжатые пиксели) */
    private final byte[] data;

    /**
     * Кешированное итоговое изображение для предпросмотра.
     * При первом вызове getPreviewImage() будет декодироваться через DDSDecoder.decode(...).
     */
    private BufferedImage preview;

    /**
     * Количество уровней MipMap, включая базовый уровень (level 0).
     * • Если текстура пришла из CGTEX (при чтении), то mipMapCount устанавливается через setter
     *   (например, texInfo.setMipMapCount(entry.getMipMapCount())).
     * • Если мы создаём новую TextureInfo из DDS-файла в UI (до упаковки в CGTEX), по умолчанию mipMapCount = 1.
     */
    private int mipMapCount = 1;

    /**
     * Конструктор. Создаёт новую запись TextureInfo с данными базового уровня.
     *
     * @param file       исходный файл (DDS или просто контейнер для имени внутри CGTEX)
     * @param width      ширина базового уровня (level 0)
     * @param height     высота базового уровня (level 0)
     * @param name       строковое имя текстуры (будет записано в CGTEX)
     * @param formatCode код формата (DXT1, DXT3 и т.д.)
     * @param data       массив байт базового уровня (level 0)
     */
    public TextureInfo(File file, int width, int height, String name, byte formatCode, byte[] data) {
        this.file = file;
        this.width = width;
        this.height = height;
        this.name = name;
        this.formatCode = formatCode;
        this.data = data;
        // mipMapCount по умолчанию = 1, пока пользователь не установит явно
    }

    /** @return исходный файл (DDS) или просто контейнер для имени */
    public File getFile() {
        return file;
    }

    /** @return ширина базового уровня */
    public int getWidth() {
        return width;
    }

    /** @return высота базового уровня */
    public int getHeight() {
        return height;
    }

    /** @return код формата (DXT1=0, DXT3=1 и т.д.) */
    public byte getFormatCode() {
        return formatCode;
    }

    /** @return данные базового уровня (байты уровня 0) */
    public byte[] getData() {
        return data;
    }

    /**
     * @return предварительное изображение (BufferedImage); при первом вызове производится
     *         декодирование через DDSDecoder.decode(width, height, formatCode, data)
     */
    public BufferedImage getPreviewImage() {
        if (preview == null) {
            preview = DDSDecoder.decode(width, height, formatCode, data);
        }
        return preview;
    }

    /**
     * Удаляет расширение из имени файла (вспомогательный метод).
     *
     * @param fileName имя файла с расширением (например, "texture.dds")
     * @return строчка без расширения (например, "texture")
     */
    public String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    /** @param name задать новое строковое имя текстуры */
    public void setName(String name) {
        this.name = name;
    }

    /** @return текущее строковое имя текстуры */
    public String getName() {
        return name;
    }

    /**
     * @return текущее количество уровней MipMap (включая level 0). По умолчанию = 1.
     */
    public int getMipMapCount() {
        return mipMapCount;
    }

    /**
     * Задать количество уровней MipMap (например, при чтении из CGTEX: entry.getMipMapCount()).
     *
     * @param mipMapCount количество уровней (должно быть ≥ 1)
     */
    public void setMipMapCount(int mipMapCount) {
        if (mipMapCount < 1) {
            throw new IllegalArgumentException("mipMapCount must be >= 1, but was " + mipMapCount);
        }
        this.mipMapCount = mipMapCount;
    }
}
