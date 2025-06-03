package org.foxesworld.cge.tools.cgtexEditor.preview;

import org.foxesworld.cge.tools.cgtexEditor.info.TextureInfo;

import javax.swing.*;
import java.awt.*;

/**
 * Рендерер ячейки списка текстур, показывает:
 *  • Имя текстуры
 *  • Ширину, высоту, формат и число уровней MipMap
 */
public class TextureCellRenderer extends JPanel implements ListCellRenderer<TextureInfo> {
    private final JLabel iconLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel infoLabel = new JLabel();

    public TextureCellRenderer() {
        setLayout(new BorderLayout(5, 5));

        // Слева: иконка (можно в будущем установить thumbnail из TextureInfo)
        add(iconLabel, BorderLayout.WEST);

        // Справа: панель с текстом (имя и информация)
        JPanel textPanel = new JPanel(new GridLayout(0, 1));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 12f));
        infoLabel.setForeground(Color.GRAY);
        textPanel.add(nameLabel);
        textPanel.add(infoLabel);

        add(textPanel, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TextureInfo> list,
                                                  TextureInfo value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        // В будущем можно установить превью-иконку:
        // iconLabel.setIcon(value.getThumbnailIcon());

        // Устанавливаем имя
        nameLabel.setText(value.getName());

        // Формируем текст с размерами, форматом и числом mip-уровней
        infoLabel.setText(String.format(
                "%dx%d, DXT%d, MipMaps: %d",
                value.getWidth(),
                value.getHeight(),
                value.getFormatCode(),
                value.getMipMapCount()
        ));

        // Подкрашиваем фон/передний план при выделении
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        setOpaque(true);
        return this;
    }
}
