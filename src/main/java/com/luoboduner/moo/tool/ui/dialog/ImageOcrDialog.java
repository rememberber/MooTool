package com.luoboduner.moo.tool.ui.dialog;

import com.formdev.flatlaf.util.SystemInfo;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.ComponentUtil;
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

    private final JComboBox<ImageOcrUtil.LanguageMode> languageComboBox;
    private final JCheckBox preprocessCheckBox;

    private boolean confirmed;

    public ImageOcrDialog(int imageCount) {
        super(App.mainFrame, "OCR 识别", true);
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

        optionsPanel.add(new JLabel("识别语言："), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        languageComboBox = new JComboBox<>(ImageOcrUtil.LanguageMode.values());
        languageComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ImageOcrUtil.LanguageMode mode) {
                    setText(mode.getLabel());
                }
                return this;
            }
        });
        optionsPanel.add(languageComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        preprocessCheckBox = new JCheckBox("图像预处理（灰度化并放大过小图片，提升识别率）", true);
        optionsPanel.add(preprocessCheckBox, gbc);

        gbc.gridy++;
        JTextArea tipArea = new JTextArea(
                "OCR 依赖本机 Tesseract 引擎（macOS: brew install tesseract tesseract-lang）。\n"
                        + "若未安装系统语言包，将自动下载到 ~/.MooTool/tessdata。");
        tipArea.setEditable(false);
        tipArea.setOpaque(false);
        tipArea.setLineWrap(true);
        tipArea.setWrapStyleWord(true);
        tipArea.setFont(UIManager.getFont("Label.font"));
        optionsPanel.add(tipArea, gbc);

        contentPane.add(optionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancelButton = new JButton("取消");
        JButton okButton = new JButton("开始识别");
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
