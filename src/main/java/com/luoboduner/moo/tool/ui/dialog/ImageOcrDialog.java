package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.util.SystemInfo;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.ImageOcrUtil;
import com.luoboduner.moo.tool.util.SystemUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * OCR 识别选项对话框
 */
public class ImageOcrDialog extends JDialog {

    private final JLabel countLabel;
    private final JComboBox<ImageOcrUtil.LanguageMode> languageComboBox;
    private final JCheckBox preprocessCheckBox;
    private final JTextArea tipArea;
    private final JButton cancelButton;
    private final JButton okButton;

    private boolean confirmed;

    public ImageOcrDialog(int imageCount) {
        super(App.mainFrame, I18n.get("imageOcr.title"), true);
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

        countLabel = new JLabel(I18n.format("imageOcr.selectedCount", imageCount));
        contentPane.add(countLabel, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        optionsPanel.add(new JLabel(I18n.get("imageOcr.language")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        languageComboBox = new JComboBox<>(ImageOcrUtil.LanguageMode.values());
        languageComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ImageOcrUtil.LanguageMode mode) {
                    setText(languageLabel(mode));
                }
                return this;
            }
        });
        optionsPanel.add(languageComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        preprocessCheckBox = new JCheckBox(I18n.get("imageOcr.preprocess"), true);
        optionsPanel.add(preprocessCheckBox, gbc);

        gbc.gridy++;
        tipArea = new JTextArea(I18n.get("imageOcr.tip"));
        tipArea.setEditable(false);
        tipArea.setOpaque(false);
        tipArea.setLineWrap(true);
        tipArea.setWrapStyleWord(true);
        tipArea.setFont(UIManager.getFont("Label.font"));
        optionsPanel.add(tipArea, gbc);

        contentPane.add(optionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cancelButton = new JButton(I18n.get("common.cancel"));
        okButton = new JButton(I18n.get("imageOcr.start"));
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

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 460, 260);
        pack();
    }

    private static String languageLabel(ImageOcrUtil.LanguageMode mode) {
        return switch (mode) {
            case CHINESE_ENGLISH -> I18n.get("imageOcr.lang.chineseEnglish");
            case CHINESE -> I18n.get("imageOcr.lang.chinese");
            case ENGLISH -> I18n.get("imageOcr.lang.english");
        };
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ImageOcrUtil.OcrOptions getOptions() {
        ImageOcrUtil.OcrOptions options = new ImageOcrUtil.OcrOptions();
        options.setLanguageMode((ImageOcrUtil.LanguageMode) languageComboBox.getSelectedItem());
        options.setPreprocess(preprocessCheckBox.isSelected());
        return options;
    }
}
