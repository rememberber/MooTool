package com.luoboduner.moo.tool.ui.listener.func;

import com.luoboduner.moo.tool.ui.form.func.TextDiffForm;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * <pre>
 * TextDiffListener - 文本对比功能事件监听器
 * </pre>
 *
 * @author CassianFlorin
 * @email flowercard591@gmail.com
 * @date 2025/9/27 10:51
 */
@Slf4j
public class TextDiffListener {

    /**
     * 添加事件监听器
     */
    public static void addListeners() {
        TextDiffForm textDiffForm = TextDiffForm.getInstance();

        // 对比按钮事件
        textDiffForm.getCompareButton().addActionListener(e -> {
            textDiffForm.performDiff();
        });

        // 清空按钮事件
        textDiffForm.getClearButton().addActionListener(e -> {
            textDiffForm.clearAll();
        });

        // 交换按钮事件
        textDiffForm.getSwapButton().addActionListener(e -> {
            textDiffForm.swapTexts();
        });

        // 复制差异按钮事件
        textDiffForm.getCopyDiffButton().addActionListener(e -> {
            textDiffForm.copyDiffResult();
        });

        // 显示模式切换事件
        textDiffForm.getDisplayModeComboBox().addActionListener(e -> {
            textDiffForm.updateDisplayMode();
            // 如果当前有内容，重新执行对比
            if (!textDiffForm.getLeftTextArea().getText().isEmpty() || 
                !textDiffForm.getRightTextArea().getText().isEmpty()) {
                textDiffForm.performDiff();
            }
        });

        // 实时对比复选框事件
        textDiffForm.getRealTimeCheckBox().addActionListener(e -> {
            if (textDiffForm.getRealTimeCheckBox().isSelected()) {
                // 开启实时对比时立即执行一次对比
                textDiffForm.performDiff();
            }
        });

        // 忽略空白差异复选框事件：切换时立即重算
        textDiffForm.getIgnoreWhitespaceCheckBox().addActionListener(e -> {
            textDiffForm.performDiff();
        });

        // 文本变化监听器 - 实时对比
        DocumentListener realTimeDocumentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (textDiffForm.getRealTimeCheckBox().isSelected()) {
                    // 延迟执行，避免频繁对比
                    javax.swing.SwingUtilities.invokeLater(textDiffForm::performDiff);
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (textDiffForm.getRealTimeCheckBox().isSelected()) {
                    javax.swing.SwingUtilities.invokeLater(textDiffForm::performDiff);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (textDiffForm.getRealTimeCheckBox().isSelected()) {
                    javax.swing.SwingUtilities.invokeLater(textDiffForm::performDiff);
                }
            }
        };

        // 为左右文本区域添加文档监听器
        textDiffForm.getLeftTextArea().getDocument().addDocumentListener(realTimeDocumentListener);
        textDiffForm.getRightTextArea().getDocument().addDocumentListener(realTimeDocumentListener);

        // 内容保存监听器
        DocumentListener saveDocumentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                saveContent();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                saveContent();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                saveContent();
            }

            private void saveContent() {
                // 延迟保存，避免频繁IO操作
                SwingUtilities.invokeLater(() -> {
                    try {
                        textDiffForm.save();
                    } catch (Exception ex) {
                        log.error("保存文本对比内容失败", ex);
                    }
                });
            }
        };

        // 为左右文本区域添加保存监听器
        textDiffForm.getLeftTextArea().getDocument().addDocumentListener(saveDocumentListener);
        textDiffForm.getRightTextArea().getDocument().addDocumentListener(saveDocumentListener);
    }
}
