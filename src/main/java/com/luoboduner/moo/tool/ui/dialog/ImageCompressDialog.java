package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.util.SystemInfo;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.ImageCompressUtil;
import com.luoboduner.moo.tool.util.SystemUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 图片压缩选项对话框
 */
public class ImageCompressDialog extends JDialog {

    private final JSlider qualitySlider;
    private final JLabel qualityValueLabel;
    private final JComboBox<String> scaleComboBox;
    private final JComboBox<String> formatComboBox;
    private final JRadioButton overwriteRadio;
    private final JRadioButton keepOriginalRadio;
    private final JLabel countLabel;

    private boolean confirmed;

    public ImageCompressDialog(int imageCount) {
        super(App.mainFrame, I18n.get("imageCompress.title"), true);
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

        countLabel = new JLabel(I18n.format("imageCompress.selectedCount", imageCount));
        contentPane.add(countLabel, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        optionsPanel.add(new JLabel(I18n.get("imageCompress.quality")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        qualitySlider = new JSlider(10, 100, 80);
        qualitySlider.setMajorTickSpacing(30);
        qualitySlider.setMinorTickSpacing(10);
        qualitySlider.setPaintTicks(true);
        optionsPanel.add(qualitySlider, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        qualityValueLabel = new JLabel("80%");
        optionsPanel.add(qualityValueLabel, gbc);
        qualitySlider.addChangeListener(e -> qualityValueLabel.setText(qualitySlider.getValue() + "%"));

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel(I18n.get("imageCompress.scale")), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        scaleComboBox = new JComboBox<>(new String[]{
                I18n.get("imageCompress.scale100"), "90%", "75%", "50%"
        });
        optionsPanel.add(scaleComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel(I18n.get("imageCompress.format")), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        formatComboBox = new JComboBox<>(new String[]{
                I18n.get("imageCompress.formatKeep"), "PNG", "JPEG"
        });
        optionsPanel.add(formatComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        optionsPanel.add(new JLabel(I18n.get("imageCompress.saveMode")), gbc);

        gbc.gridy++;
        overwriteRadio = new JRadioButton(I18n.get("imageCompress.overwrite"));
        keepOriginalRadio = new JRadioButton(I18n.get("imageCompress.keepOriginal"), true);
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
        JButton okButton = new JButton(I18n.get("imageCompress.start"));
        getRootPane().setDefaultButton(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
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

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 460, 320);
        pack();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ImageCompressUtil.CompressOptions getOptions() {
        ImageCompressUtil.CompressOptions options = new ImageCompressUtil.CompressOptions();
        options.setQuality(qualitySlider.getValue() / 100f);
        options.setScale(parseScale(scaleComboBox.getSelectedIndex()));
        options.setOutputFormat(parseFormat(formatComboBox.getSelectedIndex()));
        options.setOutputMode(overwriteRadio.isSelected()
                ? ImageCompressUtil.OutputMode.OVERWRITE
                : ImageCompressUtil.OutputMode.KEEP_ORIGINAL);
        return options;
    }

    private static float parseScale(int index) {
        return switch (index) {
            case 1 -> 0.9f;
            case 2 -> 0.75f;
            case 3 -> 0.5f;
            default -> 1.0f;
        };
    }

    private static ImageCompressUtil.OutputFormat parseFormat(int index) {
        return switch (index) {
            case 1 -> ImageCompressUtil.OutputFormat.PNG;
            case 2 -> ImageCompressUtil.OutputFormat.JPEG;
            default -> ImageCompressUtil.OutputFormat.AUTO;
        };
    }
}
