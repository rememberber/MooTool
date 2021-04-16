package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.component.QuickNotePlainTextViewer;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.FindResultForm;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.ui.frame.FindResultFrame;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Date;

/**
 * <pre>
 * 随手记事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/8/15.
 */
@Slf4j
public class QuickNoteListener {

    private static TQuickNoteMapper quickNoteMapper = MybatisUtil.getSqlSession().getMapper(TQuickNoteMapper.class);

    public static String selectedName;

    public static void addListeners() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();

        quickNoteForm.getSaveButton().addActionListener(e -> {
            if (StringUtils.isEmpty(selectedName)) {
                selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            }
            String name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", selectedName);
            if (StringUtils.isNotBlank(name)) {
                TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
                if (tQuickNote == null) {
                    tQuickNote = new TQuickNote();
                }
                String now = SqliteUtil.nowDateForSqlite();
                tQuickNote.setName(name);
                JTextArea view = (JTextArea) quickNoteForm.getScrollPane().getViewport().getView();
                tQuickNote.setContent(view.getText());
                tQuickNote.setCreateTime(now);
                tQuickNote.setModifiedTime(now);
                if (tQuickNote.getId() == null) {
                    quickNoteMapper.insert(tQuickNote);
                    QuickNoteForm.initNoteListTable();
                } else {
                    quickNoteMapper.updateByPrimaryKey(tQuickNote);
                }
            }
        });

        // 点击左侧表格事件
        quickNoteForm.getNoteListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                quickSave(false);
                int selectedRow = quickNoteForm.getNoteListTable().getSelectedRow();
                String name = quickNoteForm.getNoteListTable().getValueAt(selectedRow, 1).toString();
                TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
                QuickNotePlainTextViewer plainTextViewer = QuickNoteForm.quickNotePlainTextViewerManager.getPlainTextViewer(name);
                plainTextViewer.setText(tQuickNote.getContent());
                plainTextViewer.setCaretPosition(0);
                quickNoteForm.getScrollPane().setViewportView(plainTextViewer);
                quickNoteForm.getScrollPane().getVerticalScrollBar().setValue(0);
                quickNoteForm.getScrollPane().getHorizontalScrollBar().setValue(0);
                plainTextViewer.updateUI();
                super.mousePressed(e);
            }
        });


        // 删除按钮事件
        quickNoteForm.getDeleteButton().addActionListener(e -> {
            deleteFiles(quickNoteForm);
        });

        // 字体名称下拉框事件
        quickNoteForm.getFontNameComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String fontName = e.getItem().toString();
                int fontSize = Integer.parseInt(quickNoteForm.getFontSizeComboBox().getSelectedItem().toString());
                Font font = new Font(fontName, Font.PLAIN, fontSize);
                JTextArea view = (JTextArea) quickNoteForm.getScrollPane().getViewport().getView();
                view.setFont(font);

                App.config.setQuickNoteFontName(fontName);
                App.config.setQuickNoteFontSize(fontSize);
                App.config.save();
            }
        });

        // 字体大小下拉框事件
        quickNoteForm.getFontSizeComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int fontSize = Integer.parseInt(e.getItem().toString());
                String fontName = quickNoteForm.getFontNameComboBox().getSelectedItem().toString();
                Font font = new Font(fontName, Font.PLAIN, fontSize);
                JTextArea view = (JTextArea) quickNoteForm.getScrollPane().getViewport().getView();
                view.setFont(font);

                App.config.setQuickNoteFontName(fontName);
                App.config.setQuickNoteFontSize(fontSize);
                App.config.save();
            }
        });

        // 自动换行按钮事件
        quickNoteForm.getWrapButton().addActionListener(e -> {
            JTextArea view = (JTextArea) quickNoteForm.getScrollPane().getViewport().getView();
            view.setLineWrap(!view.getLineWrap());
        });

        // 添加按钮事件
        quickNoteForm.getAddButton().addActionListener(e -> {
            newNote();
        });

        // 左侧列表鼠标点击事件（显示下方删除按钮）
        quickNoteForm.getNoteListTable().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                quickNoteForm.getDeletePanel().setVisible(true);
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        // 左侧列表按键事件（重命名）
        quickNoteForm.getNoteListTable().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    int selectedRow = quickNoteForm.getNoteListTable().getSelectedRow();
                    int noteId = Integer.parseInt(String.valueOf(quickNoteForm.getNoteListTable().getValueAt(selectedRow, 0)));
                    String name = String.valueOf(quickNoteForm.getNoteListTable().getValueAt(selectedRow, 1));

                    if (StringUtils.isNotBlank(name)) {
                        TQuickNote tQuickNote = new TQuickNote();
                        tQuickNote.setId(noteId);
                        tQuickNote.setName(name);
                        try {
                            quickNoteMapper.updateByPrimaryKeySelective(tQuickNote);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，可能和已有笔记重名");
                            QuickNoteForm.initNoteListTable();
                            log.error(ExceptionUtils.getStackTrace(e));
                        }
                    }
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(quickNoteForm);
                }
            }
        });

        quickNoteForm.getFindButton().addActionListener(e -> {
            quickNoteForm.getFindReplacePanel().setVisible(true);
            quickNoteForm.getFindTextField().grabFocus();
            quickNoteForm.getFindTextField().selectAll();
        });

        quickNoteForm.getFindTextField().addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    find();
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        quickNoteForm.getListItemButton().addActionListener(e -> {
            int currentDividerLocation = quickNoteForm.getSplitPane().getDividerLocation();
            if (currentDividerLocation < 5) {
                quickNoteForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
            } else {
                quickNoteForm.getSplitPane().setDividerLocation(0);
            }
        });

        quickNoteForm.getExportButton().addActionListener(e -> {
            int[] selectedRows = quickNoteForm.getNoteListTable().getSelectedRows();

            try {
                if (selectedRows.length > 0) {
                    JFileChooser fileChooser = new JFileChooser(App.config.getQuickNoteExportPath());
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(quickNoteForm.getQuickNotePanel());
                    String exportPath;
                    if (approve == JFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        App.config.setQuickNoteExportPath(exportPath);
                        App.config.save();
                    } else {
                        return;
                    }

                    for (int row : selectedRows) {
                        Integer selectedId = (Integer) quickNoteForm.getNoteListTable().getValueAt(row, 0);
                        TQuickNote tQuickNote = quickNoteMapper.selectByPrimaryKey(selectedId);
                        File exportFile = FileUtil.touch(exportPath + File.separator + tQuickNote.getName() + ".txt");
                        FileUtil.writeUtf8String(tQuickNote.getContent(), exportFile);
                    }
                    JOptionPane.showMessageDialog(quickNoteForm.getQuickNotePanel(), "导出成功！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(exportPath));
                    } catch (Exception e2) {
                        log.error(ExceptionUtils.getStackTrace(e2));
                    }
                } else {
                    JOptionPane.showMessageDialog(quickNoteForm.getQuickNotePanel(), "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(quickNoteForm.getQuickNotePanel(), "导出失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        quickNoteForm.getDoFindButton().addActionListener(e -> find());

        quickNoteForm.getFindReplaceCloseLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                quickNoteForm.getFindReplacePanel().setVisible(false);
                super.mouseClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
            }
        });

        quickNoteForm.getDoReplaceButton().addActionListener(e -> replace());
        quickNoteForm.getReplaceTextField().addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    replace();
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        quickNoteForm.getFindUseRegexCheckBox().addActionListener(e -> {
            boolean selected = quickNoteForm.getFindUseRegexCheckBox().isSelected();
            if (selected) {
                quickNoteForm.getFindWordsCheckBox().setSelected(false);
                quickNoteForm.getFindWordsCheckBox().setEnabled(false);
            } else {
                quickNoteForm.getFindWordsCheckBox().setEnabled(true);
            }
        });
    }

    private static void deleteFiles(QuickNoteForm quickNoteForm) {
        try {
            int[] selectedRows = quickNoteForm.getNoteListTable().getSelectedRows();

            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(App.mainFrame, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int isDelete = JOptionPane.showConfirmDialog(App.mainFrame, "确认删除？", "确认", JOptionPane.YES_NO_OPTION);
                if (isDelete == JOptionPane.YES_OPTION) {
                    DefaultTableModel tableModel = (DefaultTableModel) quickNoteForm.getNoteListTable().getModel();

                    for (int i = 0; i < selectedRows.length; i++) {
                        int selectedRow = selectedRows[i];
                        Integer id = (Integer) tableModel.getValueAt(selectedRow, 0);
                        quickNoteMapper.deleteByPrimaryKey(id);
                    }
                    QuickNoteForm.initNoteListTable();
                }
            }
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(App.mainFrame, "删除失败！\n\n" + e1.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            log.error(ExceptionUtils.getStackTrace(e1));
        }
    }

    /**
     * save for quick key and item change
     *
     * @param refreshModifiedTime
     */
    public static void quickSave(boolean refreshModifiedTime) {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        String now = SqliteUtil.nowDateForSqlite();
        if (selectedName != null) {
            TQuickNote tQuickNote = new TQuickNote();
            tQuickNote.setName(selectedName);
            tQuickNote.setContent(QuickNoteForm.quickNotePlainTextViewerManager.getPlainTextViewer(selectedName).getText());
            if (refreshModifiedTime) {
                tQuickNote.setModifiedTime(now);
            }

            quickNoteMapper.updateByName(tQuickNote);
        } else {
            String tempName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            String name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", tempName);
            if (StringUtils.isNotBlank(name)) {
                TQuickNote tQuickNote = new TQuickNote();
                tQuickNote.setName(name);
                JTextArea view = (JTextArea) quickNoteForm.getScrollPane().getViewport().getView();
                tQuickNote.setContent(view.getText());
                tQuickNote.setCreateTime(now);
                tQuickNote.setModifiedTime(now);

                quickNoteMapper.insert(tQuickNote);
                QuickNoteForm.initNoteListTable();
            }
        }
    }

    public static void newNote() {
        String name = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
        name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", name);
        if (StringUtils.isNotBlank(name)) {
            TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
            if (tQuickNote == null) {
                tQuickNote = new TQuickNote();
            } else {
                JOptionPane.showMessageDialog(App.mainFrame, "存在同名笔记，请重新命名！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String now = SqliteUtil.nowDateForSqlite();
            tQuickNote.setName(name);
            tQuickNote.setCreateTime(now);
            tQuickNote.setModifiedTime(now);
            quickNoteMapper.insert(tQuickNote);
            QuickNoteForm.initNoteListTable();
        }
    }

    private static void find() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        JTextArea view = (JTextArea) quickNoteForm.getScrollPane().getViewport().getView();

        String content = view.getText();
        String findKeyWord = quickNoteForm.getFindTextField().getText();
        boolean isMatchCase = quickNoteForm.getFindMatchCaseCheckBox().isSelected();
        boolean isWords = quickNoteForm.getFindWordsCheckBox().isSelected();
        boolean useRegex = quickNoteForm.getFindUseRegexCheckBox().isSelected();

        int count;
        String regex = findKeyWord;

        if (!useRegex) {
            regex = ReUtil.escape(regex);
        }
        if (isWords) {
            regex = "\\b" + regex + "\\b";
        }
        if (!isMatchCase) {
            regex = "(?i)" + regex;
        }

        count = ReUtil.findAll(regex, content, 0).size();
        content = ReUtil.replaceAll(content, regex, "<span>$0</span>");

        FindResultForm.getInstance().getFindResultCount().setText(String.valueOf(count));
        FindResultForm.getInstance().setHtmlText(content);
        FindResultFrame.showResultWindow();
    }

    private static void replace() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        JTextArea view = (JTextArea) quickNoteForm.getScrollPane().getViewport().getView();
        String target = quickNoteForm.getFindTextField().getText();
        String replacement = quickNoteForm.getReplaceTextField().getText();
        String content = view.getText();
        boolean isMatchCase = quickNoteForm.getFindMatchCaseCheckBox().isSelected();
        boolean isWords = quickNoteForm.getFindWordsCheckBox().isSelected();
        boolean useRegex = quickNoteForm.getFindUseRegexCheckBox().isSelected();

        String regex = target;

        if (!useRegex) {
            regex = ReUtil.escape(regex);
        }
        if (isWords) {
            regex = "\\b" + regex + "\\b";
        }
        if (!isMatchCase) {
            regex = "(?i)" + regex;
        }

        content = ReUtil.replaceAll(content, regex, replacement);

        view.setText(content);
        view.setCaretPosition(0);
        quickNoteForm.getScrollPane().getVerticalScrollBar().setValue(0);
        quickNoteForm.getScrollPane().getHorizontalScrollBar().setValue(0);
    }
}
