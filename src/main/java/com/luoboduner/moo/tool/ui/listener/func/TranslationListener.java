package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.luoboduner.moo.tool.dao.TTranslationWordMapper;
import com.luoboduner.moo.tool.domain.TTranslationWord;
import com.luoboduner.moo.tool.ui.form.TranslationLayoutForm;
import com.luoboduner.moo.tool.ui.form.func.TranslationForm;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.JTableUtil;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import com.luoboduner.moo.tool.util.TranslationWordBookUtil;
import com.luoboduner.moo.tool.util.translator.Translator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

@Slf4j
public class TranslationListener {

    private static final String[] WORD_BOOK_COLUMNS = {"id", "原文", "译文"};

    private static TTranslationWordMapper wordMapper() {
        return MybatisUtil.getSqlSession().getMapper(TTranslationWordMapper.class);
    }

    private static Integer selectedWordId;

    public static void addListeners() {
        TranslationForm translationForm = TranslationForm.getInstance();
        TranslationLayoutForm layoutForm = translationForm.getTranslationLayoutForm();

        layoutForm.getSaveToWordBookButton().addActionListener(e -> saveFromTranslationTab());

        translationForm.getWordBookAddButton().addActionListener(e -> addNewWord());
        translationForm.getButton4().addActionListener(e -> deleteSelectedWord());
        translationForm.getWordSaveButton().addActionListener(e -> saveCurrentWordDetail());
        translationForm.getWordRetranslateButton().addActionListener(e -> retranslateCurrentWord());
        translationForm.getWordCopySourceButton().addActionListener(e -> copyText(
                translationForm.getWordSourceTextArea().getText(), translationForm.getWordCopySourceButton()));
        translationForm.getWordCopyTargetButton().addActionListener(e -> copyText(
                translationForm.getWordTargetTextArea().getText(), translationForm.getWordCopyTargetButton()));

        translationForm.getWordBookSearchField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshWordBookList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshWordBookList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        translationForm.getListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = translationForm.getListTable().rowAtPoint(e.getPoint());
                if (row >= 0) {
                    translationForm.getListTable().setRowSelectionInterval(row, row);
                    viewWordByRow(row);
                }
            }
        });

        translationForm.getListTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = translationForm.getListTable().getSelectedRow();
                if (row >= 0) {
                    viewWordByRow(row);
                }
            }
        });
    }

    public static DefaultTableModel createWordBookTableModel() {
        DefaultTableModel model = new DefaultTableModel(WORD_BOOK_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        loadWordsIntoModel(model, "");
        return model;
    }

    public static void refreshWordBookList() {
        TranslationForm translationForm = TranslationForm.getInstance();
        DefaultTableModel model = (DefaultTableModel) translationForm.getListTable().getModel();
        String keyword = translationForm.getWordBookSearchField().getText();
        loadWordsIntoModel(model, keyword);
        JTableUtil.hideColumn(translationForm.getListTable(), 0);

        if (selectedWordId != null) {
            selectWordRowById(selectedWordId);
        } else if (translationForm.getListTable().getRowCount() > 0) {
            translationForm.getListTable().setRowSelectionInterval(0, 0);
            viewWordByRow(0);
        } else {
            clearWordDetail();
        }
    }

    private static void loadWordsIntoModel(DefaultTableModel model, String keyword) {
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }

        List<TTranslationWord> words;
        if (StringUtils.isBlank(keyword)) {
            words = wordMapper().selectAll();
        } else {
            words = wordMapper().selectByFilter("%" + keyword.trim() + "%");
        }

        for (TTranslationWord word : words) {
            model.addRow(new Object[]{
                    word.getId(),
                    TranslationWordBookUtil.previewText(word.getSourceText(), 40),
                    TranslationWordBookUtil.previewText(word.getTargetText(), 40)
            });
        }
    }

    public static void saveFromTranslationTab() {
        TranslationForm translationForm = TranslationForm.getInstance();
        TranslationLayoutForm layoutForm = translationForm.getTranslationLayoutForm();

        String sourceText = layoutForm.getTextArea1().getText();
        String targetText = layoutForm.getTextArea2().getText();
        if (StringUtils.isBlank(sourceText)) {
            JOptionPane.showMessageDialog(translationForm.getTranslationPanel(), "请先输入要收藏的原文", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (StringUtils.isBlank(targetText) || targetText.startsWith("翻译中")
                || targetText.startsWith("访问") || targetText.startsWith("Bing翻译")
                || targetText.startsWith("解析翻译")) {
            JOptionPane.showMessageDialog(translationForm.getTranslationPanel(), "请等待翻译完成后再收藏", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String sourceLang = String.valueOf(layoutForm.getComboBox1().getSelectedItem());
        String targetLang = String.valueOf(layoutForm.getComboBox2().getSelectedItem());
        TTranslationWord saved = TranslationWordBookUtil.saveOrUpdate(sourceText, targetText, sourceLang, targetLang, null);
        if (saved != null) {
            selectedWordId = saved.getId();
            refreshWordBookList();
            AlertUtil.buttonInfo(layoutForm.getSaveToWordBookButton(), "", "已收藏", 1500);
        }
    }

    private static void addNewWord() {
        TranslationForm translationForm = TranslationForm.getInstance();
        selectedWordId = null;
        translationForm.getListTable().clearSelection();
        translationForm.getWordSourceTextArea().setText("");
        translationForm.getWordTargetTextArea().setText("");
        translationForm.getWordRemarkTextField().setText("");
        translationForm.getWordLangLabel().setText("新建单词");
        translationForm.getWordSourceTextArea().requestFocusInWindow();
    }

    private static void deleteSelectedWord() {
        TranslationForm translationForm = TranslationForm.getInstance();
        int row = translationForm.getListTable().getSelectedRow();
        if (row < 0) {
            return;
        }

        Integer id = (Integer) translationForm.getListTable().getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(translationForm.getTranslationPanel(),
                "确定删除选中的单词吗？", "确认", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        wordMapper().deleteByPrimaryKey(id);
        selectedWordId = null;
        refreshWordBookList();
    }

    private static void saveCurrentWordDetail() {
        TranslationForm translationForm = TranslationForm.getInstance();
        String sourceText = translationForm.getWordSourceTextArea().getText();
        if (StringUtils.isBlank(sourceText)) {
            JOptionPane.showMessageDialog(translationForm.getTranslationPanel(), "原文不能为空", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String now = SqliteUtil.nowDateForSqlite();
        TTranslationWord saved;
        if (selectedWordId != null) {
            TTranslationWord existing = wordMapper().selectByPrimaryKey(selectedWordId);
            existing.setSourceText(sourceText.trim());
            existing.setTargetText(translationForm.getWordTargetTextArea().getText());
            existing.setRemark(translationForm.getWordRemarkTextField().getText());
            existing.setModifiedTime(now);
            wordMapper().updateByPrimaryKey(existing);
            saved = existing;
        } else {
            saved = TranslationWordBookUtil.saveOrUpdate(
                    sourceText,
                    translationForm.getWordTargetTextArea().getText(),
                    Translator.AUTO_DETECT,
                    "中文（简体）",
                    translationForm.getWordRemarkTextField().getText());
        }

        if (saved != null) {
            selectedWordId = saved.getId();
            refreshWordBookList();
            AlertUtil.buttonInfo(translationForm.getWordSaveButton(), "保存", "已保存", 1500);
        }
    }

    private static void retranslateCurrentWord() {
        TranslationForm translationForm = TranslationForm.getInstance();
        String sourceText = translationForm.getWordSourceTextArea().getText();
        if (StringUtils.isBlank(sourceText)) {
            return;
        }

        TTranslationWord word = new TTranslationWord();
        word.setSourceText(sourceText);
        if (selectedWordId != null) {
            TTranslationWord existing = wordMapper().selectByPrimaryKey(selectedWordId);
            word.setSourceLang(existing.getSourceLang());
            word.setTargetLang(existing.getTargetLang());
        } else {
            word.setSourceLang(Translator.AUTO_DETECT);
            word.setTargetLang("中文（简体）");
        }

        translationForm.getWordTargetTextArea().setText("翻译中...");
        ThreadUtil.execute(() -> {
            String result = TranslationWordBookUtil.retranslate(word);
            SwingUtilities.invokeLater(() -> translationForm.getWordTargetTextArea().setText(result));
        });
    }

    private static void viewWordByRow(int row) {
        TranslationForm translationForm = TranslationForm.getInstance();
        Integer id = (Integer) translationForm.getListTable().getValueAt(row, 0);
        TTranslationWord word = wordMapper().selectByPrimaryKey(id);
        if (word == null) {
            return;
        }

        selectedWordId = word.getId();
        translationForm.getWordSourceTextArea().setText(word.getSourceText());
        translationForm.getWordTargetTextArea().setText(StringUtils.defaultString(word.getTargetText()));
        translationForm.getWordRemarkTextField().setText(StringUtils.defaultString(word.getRemark()));

        String langInfo = String.format("%s → %s",
                StringUtils.defaultIfBlank(word.getSourceLang(), Translator.AUTO_DETECT),
                StringUtils.defaultIfBlank(word.getTargetLang(), "中文（简体）"));
        translationForm.getWordLangLabel().setText(langInfo);
    }

    private static void selectWordRowById(Integer id) {
        TranslationForm translationForm = TranslationForm.getInstance();
        DefaultTableModel model = (DefaultTableModel) translationForm.getListTable().getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (id.equals(model.getValueAt(i, 0))) {
                translationForm.getListTable().setRowSelectionInterval(i, i);
                viewWordByRow(i);
                return;
            }
        }
    }

    private static void clearWordDetail() {
        TranslationForm translationForm = TranslationForm.getInstance();
        translationForm.getWordSourceTextArea().setText("");
        translationForm.getWordTargetTextArea().setText("");
        translationForm.getWordRemarkTextField().setText("");
        translationForm.getWordLangLabel().setText(" ");
    }

    private static void copyText(String text, JButton button) {
        if (StringUtils.isNotBlank(text)) {
            ClipboardUtil.setStr(text);
            AlertUtil.buttonInfo(button, "", "已复制", 1500);
        }
    }
}
