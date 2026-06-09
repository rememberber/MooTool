package com.luoboduner.moo.tool.ui.dialog;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.SystemInfo;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * OCR 识别结果对话框
 */
public class ImageOcrResultDialog extends JDialog {

    private final JTextArea resultArea;
    private final JButton copyButton;

    public ImageOcrResultDialog(String resultText) {
        super(App.mainFrame, "OCR 识别结果", false);
        resultArea = new JTextArea(resultText);
        copyButton = new JButton("复制到剪贴板");

        JPanel contentPane = new JPanel(new BorderLayout(0, 12));
        contentPane.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        setContentPane(contentPane);

        if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
            getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            getRootPane().putClientProperty("apple.awt.fullscreenable", true);
            getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton saveButton = new JButton("保存为文本");
        JButton closeButton = new JButton("关闭");
        buttonPanel.add(copyButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        copyButton.addActionListener(e -> {
            ClipboardUtil.setStr(resultArea.getText());
            AlertUtil.buttonInfo(copyButton, "复制到剪贴板", "已复制", 2000);
        });
        saveButton.addActionListener(e -> saveAsText());
        closeButton.addActionListener(e -> dispose());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        contentPane.registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.5, 0.6);
        pack();
    }

    private void saveAsText() {
        String text = resultArea.getText();
        if (StringUtils.isBlank(text)) {
            JOptionPane.showMessageDialog(this, "没有可保存的内容", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String defaultName = "OCR_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss") + ".txt";
        SystemFileChooser fileChooser = new SystemFileChooser(SystemUtil.CONFIG_HOME);
        fileChooser.setSelectedFile(new File(defaultName));
        if (fileChooser.showSaveDialog(this) != SystemFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        if (!StringUtils.endsWithIgnoreCase(file.getName(), ".txt")) {
            file = FileUtil.file(file.getParent(), file.getName() + ".txt");
        }
        FileUtil.writeString(text, file, StandardCharsets.UTF_8);
        JOptionPane.showMessageDialog(this, "已保存到：\n" + file.getAbsolutePath(), "保存成功",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
