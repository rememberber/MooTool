package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.util.SystemInfo;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.ImageWatermarkUtil;
import com.luoboduner.moo.tool.util.MsgUtil;
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
        super(App.mainFrame, I18n.get("imageWatermark.title"), true);
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

        contentPane.add(new JLabel(I18n.format("imageCompress.selectedCount", imageCount)), BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        optionsPanel.add(new JLabel(I18n.get("imageWatermark.text")), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        textField = new JTextField("MooTool", 20);
        optionsPanel.add(textField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel(I18n.get("imageWatermark.opacity")), gbc);

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
        optionsPanel.add(new JLabel(I18n.get("imageWatermark.position")), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        positionComboBox = new JComboBox<>(new String[]{
                I18n.get("imageWatermark.pos.bottomRight"),
                I18n.get("imageWatermark.pos.bottomLeft"),
                I18n.get("imageWatermark.pos.topRight"),
                I18n.get("imageWatermark.pos.topLeft"),
                I18n.get("imageWatermark.pos.center"),
                I18n.get("imageWatermark.pos.tile")
        });
        optionsPanel.add(positionComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel(I18n.get("imageWatermark.fontSize")), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        fontSizeComboBox = new JComboBox<>(new String[]{
                I18n.get("imageWatermark.font.auto"),
                I18n.get("imageWatermark.font.small"),
                I18n.get("imageWatermark.font.medium"),
                I18n.get("imageWatermark.font.large")
        });
        optionsPanel.add(fontSizeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel(I18n.get("imageWatermark.color")), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        colorComboBox = new JComboBox<>(new String[]{
                I18n.get("imageWatermark.color.white"),
                I18n.get("imageWatermark.color.black"),
                I18n.get("imageWatermark.color.gray"),
                I18n.get("imageWatermark.color.red")
        });
        optionsPanel.add(colorComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        diagonalCheckBox = new JCheckBox(I18n.get("imageWatermark.diagonal"));
        optionsPanel.add(diagonalCheckBox, gbc);

        gbc.gridy++;
        optionsPanel.add(new JLabel(I18n.get("imageCompress.saveMode")), gbc);

        gbc.gridy++;
        overwriteRadio = new JRadioButton(I18n.get("imageCompress.overwrite"));
        keepOriginalRadio = new JRadioButton(I18n.get("imageWatermark.keepOriginal"), true);
        ButtonGroup saveModeGroup = new ButtonGroup();
        saveModeGroup.add(overwriteRadio);
        saveModeGroup.add(keepOriginalRadio);

        JPanel saveModePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        saveModePanel.add(keepOriginalRadio);
        saveModePanel.add(overwriteRadio);
        optionsPanel.add(saveModePanel, gbc);

        contentPane.add(optionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancelButton = new JButton(I18n.get("common.cancel"));
        JButton okButton = new JButton(I18n.get("imageWatermark.start"));
        getRootPane().setDefaultButton(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            if (StringUtils.isBlank(textField.getText())) {
                MsgUtil.warn(this, "msg.watermarkTextRequired");
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
        options.setPosition(parsePosition(positionComboBox.getSelectedIndex()));
        options.setFontSizeMode(parseFontSize(fontSizeComboBox.getSelectedIndex()));
        options.setColor(parseColor(colorComboBox.getSelectedIndex()));
        options.setDiagonal(diagonalCheckBox.isSelected());
        options.setOutputMode(overwriteRadio.isSelected()
                ? ImageWatermarkUtil.OutputMode.OVERWRITE
                : ImageWatermarkUtil.OutputMode.KEEP_ORIGINAL);
        return options;
    }

    private static ImageWatermarkUtil.Position parsePosition(int index) {
        return switch (index) {
            case 1 -> ImageWatermarkUtil.Position.BOTTOM_LEFT;
            case 2 -> ImageWatermarkUtil.Position.TOP_RIGHT;
            case 3 -> ImageWatermarkUtil.Position.TOP_LEFT;
            case 4 -> ImageWatermarkUtil.Position.CENTER;
            case 5 -> ImageWatermarkUtil.Position.TILE;
            default -> ImageWatermarkUtil.Position.BOTTOM_RIGHT;
        };
    }

    private static ImageWatermarkUtil.FontSizeMode parseFontSize(int index) {
        return switch (index) {
            case 1 -> ImageWatermarkUtil.FontSizeMode.SMALL;
            case 2 -> ImageWatermarkUtil.FontSizeMode.MEDIUM;
            case 3 -> ImageWatermarkUtil.FontSizeMode.LARGE;
            default -> ImageWatermarkUtil.FontSizeMode.AUTO;
        };
    }

    private static Color parseColor(int index) {
        return switch (index) {
            case 1 -> Color.BLACK;
            case 2 -> Color.GRAY;
            case 3 -> Color.RED;
            default -> Color.WHITE;
        };
    }
}
