package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.util.SystemInfo;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.ImageWatermarkUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 图片水印选项对话框
 */
public class ImageWatermarkDialog extends JDialog {

    private final JTextField textField;
    private final JSlider opacitySlider;
    private final JLabel opacityValueLabel;
    private final JComboBox<String> positionComboBox;
    private final JComboBox<String> fontSizeComboBox;
    private final JComboBox<String> colorComboBox;
    private final JCheckBox diagonalCheckBox;
    private final JRadioButton overwriteRadio;
    private final JRadioButton keepOriginalRadio;

    private boolean confirmed;

    public ImageWatermarkDialog(int imageCount) {
        super(App.mainFrame, "添加水印", true);
        confirmed = false;

        JPanel contentPane = new JPanel(new BorderLayout(0, 12));
        contentPane.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        setContentPane(contentPane);

        if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
            getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            getRootPane().putClientProperty("apple.awt.fullscreenable", true);
            getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        contentPane.add(new JLabel("已选择 " + imageCount + " 张图片"), BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        optionsPanel.add(new JLabel("水印文字："), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        textField = new JTextField("MooTool", 20);
        optionsPanel.add(textField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel("不透明度："), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        opacitySlider = new JSlider(10, 100, 50);
        opacitySlider.setMajorTickSpacing(30);
        opacitySlider.setMinorTickSpacing(10);
        opacitySlider.setPaintTicks(true);
        optionsPanel.add(opacitySlider, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        opacityValueLabel = new JLabel("50%");
        optionsPanel.add(opacityValueLabel, gbc);
        opacitySlider.addChangeListener(e -> opacityValueLabel.setText(opacitySlider.getValue() + "%"));

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel("水印位置："), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        positionComboBox = new JComboBox<>(new String[]{
                "右下角", "左下角", "右上角", "左上角", "居中", "平铺"
        });
        optionsPanel.add(positionComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel("字体大小："), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        fontSizeComboBox = new JComboBox<>(new String[]{"自动", "小", "中", "大"});
        optionsPanel.add(fontSizeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel("文字颜色："), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        colorComboBox = new JComboBox<>(new String[]{"白色", "黑色", "灰色", "红色"});
        optionsPanel.add(colorComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        diagonalCheckBox = new JCheckBox("倾斜显示（-45°）");
        optionsPanel.add(diagonalCheckBox, gbc);

        gbc.gridy++;
        optionsPanel.add(new JLabel("保存方式："), gbc);

        gbc.gridy++;
        overwriteRadio = new JRadioButton("覆盖原图");
        keepOriginalRadio = new JRadioButton("保留原图（另存为 *_watermarked.*）", true);
        ButtonGroup saveModeGroup = new ButtonGroup();
        saveModeGroup.add(overwriteRadio);
        saveModeGroup.add(keepOriginalRadio);

        JPanel saveModePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        saveModePanel.add(keepOriginalRadio);
        saveModePanel.add(overwriteRadio);
        optionsPanel.add(saveModePanel, gbc);

        contentPane.add(optionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancelButton = new JButton("取消");
        JButton okButton = new JButton("添加水印");
        getRootPane().setDefaultButton(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            if (StringUtils.isBlank(textField.getText())) {
                JOptionPane.showMessageDialog(this, "请输入水印文字", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            confirmed = true;
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        contentPane.registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 460, 400);
        pack();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ImageWatermarkUtil.WatermarkOptions getOptions() {
        ImageWatermarkUtil.WatermarkOptions options = new ImageWatermarkUtil.WatermarkOptions();
        options.setText(textField.getText().trim());
        options.setOpacity(opacitySlider.getValue() / 100f);
        options.setPosition(parsePosition((String) positionComboBox.getSelectedItem()));
        options.setFontSizeMode(parseFontSize((String) fontSizeComboBox.getSelectedItem()));
        options.setColor(parseColor((String) colorComboBox.getSelectedItem()));
        options.setDiagonal(diagonalCheckBox.isSelected());
        options.setOutputMode(overwriteRadio.isSelected()
                ? ImageWatermarkUtil.OutputMode.OVERWRITE
                : ImageWatermarkUtil.OutputMode.KEEP_ORIGINAL);
        return options;
    }

    private static ImageWatermarkUtil.Position parsePosition(String positionText) {
        if (positionText == null) {
            return ImageWatermarkUtil.Position.BOTTOM_RIGHT;
        }
        return switch (positionText) {
            case "左下角" -> ImageWatermarkUtil.Position.BOTTOM_LEFT;
            case "右上角" -> ImageWatermarkUtil.Position.TOP_RIGHT;
            case "左上角" -> ImageWatermarkUtil.Position.TOP_LEFT;
            case "居中" -> ImageWatermarkUtil.Position.CENTER;
            case "平铺" -> ImageWatermarkUtil.Position.TILE;
            default -> ImageWatermarkUtil.Position.BOTTOM_RIGHT;
        };
    }

    private static ImageWatermarkUtil.FontSizeMode parseFontSize(String fontSizeText) {
        if (fontSizeText == null) {
            return ImageWatermarkUtil.FontSizeMode.AUTO;
        }
        return switch (fontSizeText) {
            case "小" -> ImageWatermarkUtil.FontSizeMode.SMALL;
            case "中" -> ImageWatermarkUtil.FontSizeMode.MEDIUM;
            case "大" -> ImageWatermarkUtil.FontSizeMode.LARGE;
            default -> ImageWatermarkUtil.FontSizeMode.AUTO;
        };
    }

    private static Color parseColor(String colorText) {
        if ("黑色".equals(colorText)) {
            return Color.BLACK;
        }
        if ("灰色".equals(colorText)) {
            return Color.GRAY;
        }
        if ("红色".equals(colorText)) {
            return Color.RED;
        }
        return Color.WHITE;
    }
}
