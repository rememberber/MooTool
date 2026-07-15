package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.luoboduner.moo.tool.dao.TTranslationHistoryMapper;
import com.luoboduner.moo.tool.dao.TTranslationWordMapper;
import com.luoboduner.moo.tool.domain.TTranslationHistory;
import com.luoboduner.moo.tool.domain.TTranslationWord;
import com.luoboduner.moo.tool.ui.form.TranslationLayoutForm;
import com.luoboduner.moo.tool.ui.form.func.TranslationForm;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.JTableUtil;
import com.luoboduner.moo.tool.util.MsgUtil;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import com.luoboduner.moo.tool.util.TranslationHistoryUtil;
import com.luoboduner.moo.tool.util.TranslationWordBookUtil;
import com.luoboduner.moo.tool.util.translator.TranslatorLangUtil;
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

    private static boolean i18nRegistered;

    private static String[] wordBookColumns() {
        return new String[]{"id", I18n.get("table.col.source"), I18n.get("table.col.target")};
    }

    private static String[] historyColumns() {
        return new String[]{"id", I18n.get("history.col.time"), I18n.get("table.col.source"),
                I18n.get("table.col.lang"), I18n.get("table.col.translator")};
    }

    private static TTranslationWordMapper wordMapper() {
        return MybatisUtil.getSqlSession().getMapper(TTranslationWordMapper.class);
    }

    private static TTranslationHistoryMapper historyMapper() {
        return MybatisUtil.getSqlSession().getMapper(TTranslationHistoryMapper.class);
    }

    private static Integer selectedWordId;
    private static Integer selectedHistoryId;

    public static void addListeners() {
        TranslationForm translationForm = TranslationForm.getInstance();
        TranslationLayoutForm layoutForm = translationForm.getTranslationLayoutForm();
        applyI18nStatic();
        if (!i18nRegistered) {
            I18nUiUtil.register(TranslationListener::applyI18nStatic);
            i18nRegistered = true;
        }

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

        translationForm.getHistoryDeleteButton().addActionListener(e -> deleteSelectedHistory());
        translationForm.getHistoryClearButton().addActionListener(e -> clearAllHistory());
        translationForm.getHistoryApplyButton().addActionListener(e -> applyHistoryToTranslation());
        translationForm.getHistoryCopySourceButton().addActionListener(e -> copyText(
                translationForm.getHistorySourceTextArea().getText(), translationForm.getHistoryCopySourceButton()));
        translationForm.getHistoryCopyTargetButton().addActionListener(e -> copyText(
                translationForm.getHistoryTargetTextArea().getText(), translationForm.getHistoryCopyTargetButton()));

        translationForm.getHistorySearchField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshHistoryList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshHistoryList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        translationForm.getHistoryTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = translationForm.getHistoryTable().rowAtPoint(e.getPoint());
                if (row >= 0) {
                    translationForm.getHistoryTable().setRowSelectionInterval(row, row);
                    viewHistoryByRow(row);
                    if (e.getClickCount() >= 2) {
                        applyHistoryToTranslation();
                    }
                }
            }
        });

        translationForm.getHistoryTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = translationForm.getHistoryTable().getSelectedRow();
                if (row >= 0) {
                    viewHistoryByRow(row);
                }
            }
        });

        JTabbedPane translationTabbedPane = translationForm.getTranslationTabbedPane();
        if (translationTabbedPane != null) {
            translationTabbedPane.addChangeListener(e -> {
                if (translationTabbedPane.getSelectedIndex() == 2) {
                    refreshHistoryList();
                }
            });
        }
    }

    public static DefaultTableModel createWordBookTableModel() {
        DefaultTableModel model = new DefaultTableModel(wordBookColumns(), 0) {
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
            MsgUtil.info(translationForm.getTranslationPanel(), "msg.enterSourceToSave");
            return;
        }
        if (StringUtils.isBlank(targetText) || !TranslationHistoryUtil.isSuccessfulTranslation(targetText)) {
            MsgUtil.info(translationForm.getTranslationPanel(), "msg.waitTranslation");
            return;
        }

        String sourceLang = String.valueOf(layoutForm.getComboBox1().getSelectedItem());
        String targetLang = String.valueOf(layoutForm.getComboBox2().getSelectedItem());
        TTranslationWord saved = TranslationWordBookUtil.saveOrUpdate(sourceText, targetText, sourceLang, targetLang, null);
        if (saved != null) {
            selectedWordId = saved.getId();
            refreshWordBookList();
            AlertUtil.buttonInfo(layoutForm.getSaveToWordBookButton(), "", I18n.get("msg.savedToWordBook"), 1500);
        }
    }

    private static void addNewWord() {
        TranslationForm translationForm = TranslationForm.getInstance();
        selectedWordId = null;
        translationForm.getListTable().clearSelection();
        translationForm.getWordSourceTextArea().setText("");
        translationForm.getWordTargetTextArea().setText("");
        translationForm.getWordRemarkTextField().setText("");
        translationForm.getWordLangLabel().setText(I18n.get("translation.status.newWord"));
        translationForm.getWordSourceTextArea().requestFocusInWindow();
    }

    private static void deleteSelectedWord() {
        TranslationForm translationForm = TranslationForm.getInstance();
        int row = translationForm.getListTable().getSelectedRow();
        if (row < 0) {
            return;
        }

        Integer id = (Integer) translationForm.getListTable().getValueAt(row, 0);
        if (MsgUtil.confirm(translationForm.getTranslationPanel(), "msg.confirmDeleteWord") != JOptionPane.YES_OPTION) {
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
            MsgUtil.info(translationForm.getTranslationPanel(), "msg.sourceRequired");
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
                    TranslatorLangUtil.getAutoDetectLabel(),
                    TranslatorLangUtil.getDisplayName(TranslatorLangUtil.DEFAULT_TARGET_CODE),
                    translationForm.getWordRemarkTextField().getText());
        }

        if (saved != null) {
            selectedWordId = saved.getId();
            refreshWordBookList();
            AlertUtil.buttonInfo(translationForm.getWordSaveButton(), I18n.get("common.save"), I18n.get("msg.saved"), 1500);
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
            word.setSourceLang(TranslatorLangUtil.getAutoDetectLabel());
            word.setTargetLang(TranslatorLangUtil.getDisplayName(TranslatorLangUtil.DEFAULT_TARGET_CODE));
        }

        translationForm.getWordTargetTextArea().setText(I18n.get("translation.translating"));
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
                TranslatorLangUtil.toDisplayName(word.getSourceLang()),
                TranslatorLangUtil.toDisplayName(
                        StringUtils.defaultIfBlank(word.getTargetLang(), TranslatorLangUtil.DEFAULT_TARGET_CODE)));
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
            AlertUtil.buttonInfo(button, "", I18n.get("common.copied"), 1500);
        }
    }

    public static DefaultTableModel createHistoryTableModel() {
        DefaultTableModel model = new DefaultTableModel(historyColumns(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        loadHistoryIntoModel(model, "");
        return model;
    }

    public static void refreshHistoryListIfVisible() {
        TranslationForm translationForm = TranslationForm.getInstance();
        if (translationForm.getTranslationTabbedPane() != null
                && translationForm.getTranslationTabbedPane().getSelectedIndex() == 2) {
            refreshHistoryList();
        }
    }

    public static void refreshHistoryList() {
        TranslationForm translationForm = TranslationForm.getInstance();
        DefaultTableModel model = (DefaultTableModel) translationForm.getHistoryTable().getModel();
        String keyword = translationForm.getHistorySearchField().getText();
        loadHistoryIntoModel(model, keyword);
        JTableUtil.hideColumn(translationForm.getHistoryTable(), 0);

        if (selectedHistoryId != null) {
            selectHistoryRowById(selectedHistoryId);
        } else if (translationForm.getHistoryTable().getRowCount() > 0) {
            translationForm.getHistoryTable().setRowSelectionInterval(0, 0);
            viewHistoryByRow(0);
        } else {
            clearHistoryDetail();
        }
    }

    private static void loadHistoryIntoModel(DefaultTableModel model, String keyword) {
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }

        List<TTranslationHistory> histories;
        if (StringUtils.isBlank(keyword)) {
            histories = historyMapper().selectAll();
        } else {
            histories = historyMapper().selectByFilter("%" + keyword.trim() + "%");
        }

        for (TTranslationHistory history : histories) {
            String langPair = String.format("%s → %s",
                    TranslatorLangUtil.toDisplayName(history.getSourceLang()),
                    TranslatorLangUtil.toDisplayName(
                            StringUtils.defaultIfBlank(history.getTargetLang(), TranslatorLangUtil.DEFAULT_TARGET_CODE)));
            model.addRow(new Object[]{
                    history.getId(),
                    StringUtils.defaultString(history.getCreateTime()),
                    TranslationHistoryUtil.previewText(history.getSourceText(), 36),
                    langPair,
                    TranslationHistoryUtil.formatTranslatorType(history.getTranslatorType())
            });
        }
    }

    private static void viewHistoryByRow(int row) {
        TranslationForm translationForm = TranslationForm.getInstance();
        Integer id = (Integer) translationForm.getHistoryTable().getValueAt(row, 0);
        TTranslationHistory history = historyMapper().selectByPrimaryKey(id);
        if (history == null) {
            return;
        }

        selectedHistoryId = history.getId();
        translationForm.getHistorySourceTextArea().setText(history.getSourceText());
        translationForm.getHistoryTargetTextArea().setText(StringUtils.defaultString(history.getTargetText()));

        String meta = String.format("%s → %s  |  %s  |  %s",
                TranslatorLangUtil.toDisplayName(history.getSourceLang()),
                TranslatorLangUtil.toDisplayName(
                        StringUtils.defaultIfBlank(history.getTargetLang(), TranslatorLangUtil.DEFAULT_TARGET_CODE)),
                TranslationHistoryUtil.formatTranslatorType(history.getTranslatorType()),
                StringUtils.defaultIfBlank(history.getCreateTime(), "-"));
        translationForm.getHistoryMetaLabel().setText(meta);
    }

    private static void selectHistoryRowById(Integer id) {
        TranslationForm translationForm = TranslationForm.getInstance();
        DefaultTableModel model = (DefaultTableModel) translationForm.getHistoryTable().getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (id.equals(model.getValueAt(i, 0))) {
                translationForm.getHistoryTable().setRowSelectionInterval(i, i);
                viewHistoryByRow(i);
                return;
            }
        }
    }

    private static void clearHistoryDetail() {
        TranslationForm translationForm = TranslationForm.getInstance();
        translationForm.getHistorySourceTextArea().setText("");
        translationForm.getHistoryTargetTextArea().setText("");
        translationForm.getHistoryMetaLabel().setText(" ");
    }

    private static void deleteSelectedHistory() {
        TranslationForm translationForm = TranslationForm.getInstance();
        int row = translationForm.getHistoryTable().getSelectedRow();
        if (row < 0) {
            return;
        }

        Integer id = (Integer) translationForm.getHistoryTable().getValueAt(row, 0);
        if (MsgUtil.confirm(translationForm.getTranslationPanel(), "msg.confirmDeleteHistory") != JOptionPane.YES_OPTION) {
            return;
        }

        historyMapper().deleteByPrimaryKey(id);
        selectedHistoryId = null;
        refreshHistoryList();
    }

    private static void clearAllHistory() {
        TranslationForm translationForm = TranslationForm.getInstance();
        if (translationForm.getHistoryTable().getRowCount() == 0) {
            return;
        }

        if (MsgUtil.confirm(translationForm.getTranslationPanel(), "msg.confirmClearHistoryIrreversible") != JOptionPane.YES_OPTION) {
            return;
        }

        historyMapper().deleteAll();
        selectedHistoryId = null;
        refreshHistoryList();
    }

    private static void applyHistoryToTranslation() {
        TranslationForm translationForm = TranslationForm.getInstance();
        int row = translationForm.getHistoryTable().getSelectedRow();
        if (row < 0) {
            return;
        }

        Integer id = (Integer) translationForm.getHistoryTable().getValueAt(row, 0);
        TTranslationHistory history = historyMapper().selectByPrimaryKey(id);
        if (history == null) {
            return;
        }

        TranslationLayoutForm layoutForm = translationForm.getTranslationLayoutForm();
        layoutForm.applyFromHistory(
                history.getSourceText(),
                history.getTargetText(),
                history.getSourceLang(),
                history.getTargetLang());
        JTabbedPane translationTabbedPane = translationForm.getTranslationTabbedPane();
        if (translationTabbedPane != null) {
            translationTabbedPane.setSelectedIndex(0);
        }
        AlertUtil.buttonInfo(translationForm.getHistoryApplyButton(), I18n.get("translation.applyToTranslation"),
                I18n.get("msg.applied"), 1500);
    }

    public static void applyI18nStatic() {
        TranslationForm form = TranslationForm.getInstance();
        if (form.getListTable() != null) {
            ((DefaultTableModel) form.getListTable().getModel()).setColumnIdentifiers(wordBookColumns());
        }
        if (form.getHistoryTable() != null) {
            ((DefaultTableModel) form.getHistoryTable().getModel()).setColumnIdentifiers(historyColumns());
        }
    }
}
