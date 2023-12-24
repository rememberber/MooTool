package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONUtil;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import com.google.common.collect.Lists;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.component.FindReplaceBar;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteRSyntaxTextViewer;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteRSyntaxTextViewerManager;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.QuickNoteIndicatorTools;
import com.luoboduner.moo.tool.util.SqliteUtil;
import de.hunsicker.jalopy.Jalopy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    // 创建一个单线程的ExecutorService
    public static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void addListeners() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        QuickNoteRSyntaxTextViewerManager quickNoteRSyntaxTextViewerManager = QuickNoteForm.quickNoteRSyntaxTextViewerManager;

        // 保存按钮
        quickNoteForm.getSaveButton().addActionListener(e -> {
            if (StringUtils.isEmpty(selectedName)) {
                selectedName = getDefaultFileName();
            }

            // show input dialog
            String name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", selectedName);
            if (StringUtils.isNotBlank(name)) {
                TQuickNote tQuickNote = quickNoteMapper.selectByName(name);

                if (tQuickNote == null) {
                    tQuickNote = new TQuickNote();
                }
                String now = SqliteUtil.nowDateForSqlite();
                tQuickNote.setName(name);
                tQuickNote.setContent(quickNoteRSyntaxTextViewerManager.getCurrentText());
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
                int focusedRowIndex = quickNoteForm.getNoteListTable().rowAtPoint(e.getPoint());
                if (focusedRowIndex == -1) {
                    return;
                }
                QuickNoteRSyntaxTextViewer.ignoreQuickSave = true;
                try {
                    viewByRowNum(focusedRowIndex);
                } catch (Exception e1) {
                    log.error(e1.toString());
                } finally {
                    QuickNoteRSyntaxTextViewer.ignoreQuickSave = false;
                }

                super.mousePressed(e);
            }
        });


        // 删除按钮事件
        quickNoteForm.getDeleteButton().addActionListener(e -> {
            deleteFiles(quickNoteForm);
        });

        // 语法高亮下拉框事件
        quickNoteForm.getSyntaxComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String syntaxName = e.getItem().toString();

                if (StringUtils.isNotEmpty(syntaxName)) {
                    if (selectedName != null && !QuickNoteRSyntaxTextViewer.ignoreQuickSave) {
                        TQuickNote tQuickNote = new TQuickNote();
                        tQuickNote.setName(selectedName);
                        tQuickNote.setSyntax(syntaxName);
                        String now = SqliteUtil.nowDateForSqlite();
                        tQuickNote.setModifiedTime(now);

                        quickNoteMapper.updateByName(tQuickNote);

                        quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(selectedName);
                        RTextScrollPane syntaxTextViewer = quickNoteRSyntaxTextViewerManager.getRTextScrollPane(selectedName);
                        quickNoteForm.getContentSplitPane().setLeftComponent(syntaxTextViewer);
                        syntaxTextViewer.updateUI();
                    }

                }
            }
        });

        // 字体名称下拉框事件
        quickNoteForm.getFontNameComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String fontName = e.getItem().toString();

                if (selectedName != null && !QuickNoteRSyntaxTextViewer.ignoreQuickSave) {

                    TQuickNote tQuickNote = new TQuickNote();
                    tQuickNote.setName(selectedName);
                    tQuickNote.setFontName(fontName);
                    String now = SqliteUtil.nowDateForSqlite();
                    tQuickNote.setModifiedTime(now);

                    quickNoteMapper.updateByName(tQuickNote);
                    App.config.setQuickNoteFontName(fontName);
                    App.config.save();

                    quickNoteRSyntaxTextViewerManager.updateFont(selectedName);
                }
            }
        });

        // 字体大小下拉框事件
        quickNoteForm.getFontSizeComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int fontSize = Integer.parseInt(e.getItem().toString());

                if (selectedName != null && !QuickNoteRSyntaxTextViewer.ignoreQuickSave) {
                    TQuickNote tQuickNote = new TQuickNote();
                    tQuickNote.setName(selectedName);
                    tQuickNote.setFontSize(String.valueOf(fontSize));
                    String now = SqliteUtil.nowDateForSqlite();
                    tQuickNote.setModifiedTime(now);

                    quickNoteMapper.updateByName(tQuickNote);

                    App.config.setQuickNoteFontSize(fontSize);
                    App.config.save();

                    quickNoteRSyntaxTextViewerManager.updateFont(selectedName);
                }
            }
        });

        // 自动换行按钮事件
        quickNoteForm.getWrapButton().addActionListener(e -> {
            RSyntaxTextArea view = quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea();
            view.setLineWrap(!view.getLineWrap());
        });

        // 添加按钮事件
        quickNoteForm.getAddButton().addActionListener(e -> newNote());


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
                            TQuickNote tQuickNoteBefore = quickNoteMapper.selectByPrimaryKey(noteId);

                            quickNoteMapper.updateByPrimaryKeySelective(tQuickNote);

                            selectedName = name;

                            RTextScrollPane syntaxTextViewer = quickNoteRSyntaxTextViewerManager.getRTextScrollPane(name);
                            quickNoteForm.getContentSplitPane().setLeftComponent(syntaxTextViewer);
                            quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(tQuickNoteBefore.getName());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，可能和已有笔记重名");
                            QuickNoteForm.initNoteListTable();
                            log.error(ExceptionUtils.getStackTrace(e));
                        }
                    }
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(quickNoteForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                    int selectedRow = quickNoteForm.getNoteListTable().getSelectedRow();
                    QuickNoteRSyntaxTextViewer.ignoreQuickSave = true;
                    try {
                        viewByRowNum(selectedRow);
                    } catch (Exception e1) {
                        log.error(e1.toString());
                    } finally {
                        QuickNoteRSyntaxTextViewer.ignoreQuickSave = false;
                    }
                }
            }
        });

        // 查找按钮
        quickNoteForm.getFindButton().addActionListener(e -> {
            showFindPanel();

        });

        // 快捷替换按钮
        quickNoteForm.getQuickReplaceButton().addActionListener(e -> {
            int totalWidth = quickNoteForm.getContentSplitPane().getWidth();
            int currentDividerLocation = quickNoteForm.getContentSplitPane().getDividerLocation();

            if (totalWidth - currentDividerLocation < 10) {
                quickNoteForm.getQuickReplaceScrollPane().setVisible(true);
                quickNoteForm.getContentSplitPane().setDividerLocation((int) (totalWidth * 0.72));
            } else {
                quickNoteForm.getContentSplitPane().setDividerLocation(totalWidth);
                quickNoteForm.getQuickReplaceScrollPane().setVisible(false);
            }
        });

        // 列表按钮
        quickNoteForm.getListItemButton().addActionListener(e -> {
            int currentDividerLocation = quickNoteForm.getSplitPane().getDividerLocation();
            if (currentDividerLocation < 5) {
                quickNoteForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
            } else {
                quickNoteForm.getSplitPane().setDividerLocation(0);
            }
        });

        // 导出按钮
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

        // 格式化按钮
        quickNoteForm.getFormatButton().addActionListener(e -> format());

        // 执行快捷替换
        quickNoteForm.getStartQuickReplaceButton().addActionListener(e -> quickReplace());

        // 关闭快捷查找面板按钮
        quickNoteForm.getQuickReplaceCloseLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                quickNoteForm.getContentSplitPane().setDividerLocation(quickNoteForm.getContentSplitPane().getWidth());
                quickNoteForm.getQuickReplaceScrollPane().setVisible(false);
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

        quickNoteForm.getColorButton().addActionListener(e -> quickNoteForm.getColorSettingPanel().setVisible(quickNoteForm.getColorButton().isSelected()));

        // 颜色按钮事件
        String[] colorKeys = QuickNoteForm.COLOR_KEYS;
        JToggleButton[] colorButtons = QuickNoteForm.COLOR_BUTTONS;
        for (int i = 0; i < colorButtons.length; i++) {
            colorButtons[i].addActionListener(e -> {
                String colorKey = colorKeys[0];
                for (int i1 = 0; i1 < colorButtons.length; i1++) {
                    if (colorButtons[i1].isSelected()) {
                        colorKey = colorKeys[i1];
                        break;
                    }
                }
//                Color color = UIManager.getColor(colorKey);
                if (StringUtils.isNotEmpty(colorKey)) {
                    if (selectedName != null && !QuickNoteRSyntaxTextViewer.ignoreQuickSave) {
                        TQuickNote tQuickNote = new TQuickNote();
                        tQuickNote.setName(selectedName);
                        tQuickNote.setColor(colorKey);
                        String now = SqliteUtil.nowDateForSqlite();
                        tQuickNote.setModifiedTime(now);

                        quickNoteMapper.updateByName(tQuickNote);

                        QuickNoteForm.initNoteListTable();
                    }
                }
            });
        }

        // 搜索框变更事件
        quickNoteForm.getSearchTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                QuickNoteForm.initNoteListTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                QuickNoteForm.initNoteListTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
//                QuickNoteForm.initNoteListTable();
            }
        });
    }

    public static void showFindPanel() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        QuickNoteRSyntaxTextViewerManager quickNoteRSyntaxTextViewerManager = QuickNoteForm.quickNoteRSyntaxTextViewerManager;
        quickNoteForm.getFindReplacePanel().removeAll();
        quickNoteForm.getFindReplacePanel().setDoubleBuffered(true);
        RSyntaxTextArea view = quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea();
        FindReplaceBar findReplaceBar = new FindReplaceBar(view);
        quickNoteForm.getFindReplacePanel().add(findReplaceBar.getFindOptionPanel());
        quickNoteForm.getFindReplacePanel().setVisible(true);
        quickNoteForm.getFindReplacePanel().updateUI();
        findReplaceBar.getFindField().setText(view.getSelectedText());
        findReplaceBar.getFindField().grabFocus();
        findReplaceBar.getFindField().selectAll();
    }

    /**
     * view By Row Num
     *
     * @param rowNum
     */
    private static void viewByRowNum(int rowNum) {

        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        QuickNoteRSyntaxTextViewerManager quickNoteRSyntaxTextViewerManager = QuickNoteForm.quickNoteRSyntaxTextViewerManager;

        quickNoteForm.getFindReplacePanel().removeAll();
        quickNoteForm.getFindReplacePanel().setVisible(false);

        String name = quickNoteForm.getNoteListTable().getValueAt(rowNum, 1).toString();
        selectedName = name;

        quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(name);
        RTextScrollPane syntaxTextViewer = quickNoteRSyntaxTextViewerManager.getRTextScrollPane(name);

        quickNoteForm.getContentSplitPane().setLeftComponent(syntaxTextViewer);

        TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
        String color = tQuickNote.getColor();
        if (StringUtils.isEmpty(color)) {
            color = QuickNoteForm.COLOR_KEYS[0];
        }

        quickNoteForm.getColorButton().setIcon(new QuickNoteForm.AccentColorIcon(color));
        quickNoteForm.getSyntaxComboBox().setSelectedItem(tQuickNote.getSyntax());
        quickNoteForm.getFontNameComboBox().setSelectedItem(tQuickNote.getFontName());
        quickNoteForm.getFontSizeComboBox().setSelectedItem(String.valueOf(tQuickNote.getFontSize()));

        int colorIndex = ArrayUtil.indexOf(QuickNoteForm.COLOR_KEYS, color);
        if (colorIndex >= 0 && colorIndex < QuickNoteForm.COLOR_BUTTONS.length) {
            QuickNoteForm.COLOR_BUTTONS[colorIndex].setSelected(true);
        }

        syntaxTextViewer.putClientProperty("JComponent.outline", UIManager.getColor(color));

//        syntaxTextViewer.updateUI();
    }

    /**
     * Default File Name
     *
     * @return
     */
    private static String getDefaultFileName() {
        return "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
    }

    /**
     * 快捷替换
     */
    private static void quickReplace() {
        try {
            QuickNoteRSyntaxTextViewer.ignoreQuickSave = true;
            QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
            RSyntaxTextArea view = QuickNoteForm.quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea();

            String content = view.getText();

            String[] splits = content.split("\n");

            List<String> target = Lists.newArrayList();
            for (String split : splits) {

                if (quickNoteForm.getTrimBlankCheckBox().isSelected()) {
                    split = split.replace(" ", "");
                }

                if (quickNoteForm.getTrimBlankRowCheckBox().isSelected() && StringUtils.isBlank(split)) {
                    continue;
                }

                if (quickNoteForm.getClearTabTCheckBox().isSelected()) {
                    split = split.replace("\t", "");
                }

                // ------------

                if (quickNoteForm.getScientificToNormalCheckBox().isSelected()) {
                    String[] strs = split.split(" ");
                    List<String> tmp = Lists.newArrayList();
                    for (String str : strs) {
                        if (NumberUtil.isNumber(str)) {
                            BigDecimal bigDecimal = NumberUtil.toBigDecimal(str.replace("e", "E"));
                            str = bigDecimal.toString();
                        }
                        tmp.add(str);
                    }
                    split = StringUtils.join(tmp, " ");
                }

                if (quickNoteForm.getNormalToScientificCheckBox().isSelected()) {
                    String[] strs = split.split(" ");
                    List<String> tmp = Lists.newArrayList();
                    for (String str : strs) {
                        if (NumberUtil.isNumber(str)) {
                            BigDecimal bigDecimal = NumberUtil.toBigDecimal(str);
                            DecimalFormat decimalFormat = new DecimalFormat("0." + StringUtils.repeat("#", str.split("\\.")[0].length() - 1) + "E0");
                            str = decimalFormat.format(bigDecimal);
                        }
                        tmp.add(str);
                    }
                    split = StringUtils.join(tmp, " ");
                }

                if (quickNoteForm.getToThousandthCheckBox().isSelected()) {
                    String[] strs = split.split(" ");
                    List<String> tmp = Lists.newArrayList();
                    for (String str : strs) {
                        if (NumberUtil.isNumber(str)) {
                            str = toThousandth(str);
                        }
                        tmp.add(str);
                    }
                    split = StringUtils.join(tmp, " ");
                }

                if (quickNoteForm.getToNormalNumCheckBox().isSelected()) {
                    String[] strs = split.split(" ");
                    List<String> tmp = Lists.newArrayList();
                    for (String str : strs) {
                        // 如果str只包含数字和小数点和逗号，就去掉逗号
                        if (str.matches("^[0-9,\\.]+$")) {
                            str = str.replace(",", "");
                        }
                        tmp.add(str);
                    }
                    split = StringUtils.join(tmp, " ");
                }

                if (quickNoteForm.getUnderlineToHumpCheckBox().isSelected()) {
                    split = underlineToHump(split);
                }

                if (quickNoteForm.getHumpToUnderlineCheckBox().isSelected()) {
                    split = humpToUnderline(split);
                }

                if (quickNoteForm.getUperToLowerCheckBox().isSelected()) {
                    split = split.toLowerCase();
                }

                if (quickNoteForm.getLowerToUperCheckBox().isSelected()) {
                    split = split.toUpperCase();
                }

                // ------------
                if (quickNoteForm.getCommaToEnterCheckBox().isSelected()) {
                    split = split.replace(",", "\n");
                }
                if (quickNoteForm.getTabToEnterCheckBox().isSelected()) {
                    split = split.replace("\t", "\n");
                }

                target.add(split);
            }

            if (quickNoteForm.getClearEnterCheckBox().isSelected()) {
                view.setText(StringUtils.join(target, ""));
            } else if (quickNoteForm.getEnterToCommaCheckBox().isSelected()) {
                view.setText(StringUtils.join(target, ","));
            } else if (quickNoteForm.getEnterToCommaSingleQuotesCheckBox().isSelected()) {
                view.setText("'" + StringUtils.join(target, "','") + "'");
            } else if (quickNoteForm.getEnterToCommaDoubleQuotesCheckBox().isSelected()) {
                view.setText("\"" + StringUtils.join(target, "\",\"") + "\"");
            } else {
                view.setText(StringUtils.join(target, "\n"));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(App.mainFrame, "转换失败！\n\n" + e.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            log.error(ExceptionUtils.getStackTrace(e));
        } finally {
            QuickNoteRSyntaxTextViewer.ignoreQuickSave = false;
        }

    }

    /**
     * 删除文件
     *
     * @param quickNoteForm
     */
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
                        String name = (String) tableModel.getValueAt(selectedRow, 1);
                        quickNoteMapper.deleteByPrimaryKey(id);
                        QuickNoteForm.quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(name);
                    }
                    selectedName = null;
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
    public static void quickSave(boolean refreshModifiedTime, boolean writeLog) {

        executorService.submit(() -> {
            String now = SqliteUtil.nowDateForSqlite();
            if (selectedName != null) {
                TQuickNote tQuickNote = new TQuickNote();
                tQuickNote.setName(selectedName);

                String text = QuickNoteForm.quickNoteRSyntaxTextViewerManager.getTextByName(selectedName);
                if (writeLog) {
                    log.info("save note: " + selectedName + ", content: " + text);
                }
                tQuickNote.setContent(text);
                if (refreshModifiedTime) {
                    tQuickNote.setModifiedTime(now);
                }

                quickNoteMapper.updateByName(tQuickNote);
            }

            QuickNoteIndicatorTools.showTips("已保存：" + selectedName, QuickNoteIndicatorTools.TipsLevel.SUCCESS);
        });

    }

    /**
     * create new Note
     */
    public static void newNote() {
        String name = getDefaultFileName();
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
            tQuickNote.setSyntax(SyntaxConstants.SYNTAX_STYLE_NONE);
            tQuickNote.setFontName(App.config.getQuickNoteFontName());
            tQuickNote.setFontSize(String.valueOf(App.config.getFontSize()));
            quickNoteMapper.insert(tQuickNote);
            QuickNoteForm.initNoteListTable();
        }
    }

    /**
     * 将字符串数字转成千分位显示。
     */
    public static String toThousandth(String value) {
        DecimalFormat decimalFormat;
        if (value.indexOf(".") > 0) {
            int afterPointLength = value.length() - value.indexOf(".") - 1;

            StringBuilder formatBuilder = new StringBuilder("###,##0.");
            for (int i = 0; i < afterPointLength; i++) {
                formatBuilder.append("0");
            }
            decimalFormat = new DecimalFormat(formatBuilder.toString());
        } else {
            decimalFormat = new DecimalFormat("###,##0");
        }
        double number;
        try {
            number = Double.parseDouble(value);
        } catch (Exception e) {
            number = 0.0;
        }
        return decimalFormat.format(number);
    }


    /**
     * 下划线转驼峰
     *
     * @param split
     * @return
     */
    private static String underlineToHump(String split) {
        StringBuilder result = new StringBuilder();
        String[] strings = split.split("_");
        for (String str : strings) {
            if (result.length() == 0) {
                result.append(str.toLowerCase());
            } else {
                result.append(str.substring(0, 1).toUpperCase());
                result.append(str.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    /**
     * 驼峰转下划线
     * 来源于网络，地址找不到了，如有侵权，请联系作者删除。
     *
     * @param split
     * @return
     */
    private static String humpToUnderline(String split) {
        StringBuilder underLineBuilder = new StringBuilder();
        // 连续大写字母单词开关
        boolean switchTag = true;
        for (int i = 0; i < split.length(); i++) {
            // 转为ASCII
            int asciiNum = split.charAt(i);
            if (asciiNum > 64 && asciiNum < 91) {
                // 首字母不加下划线
                boolean temp1 = i != 0;
                // 下一位为小写字母时
                boolean temp2 = i < split.length() - 1 && split.charAt(i + 1) > 95 && split.charAt(i + 1) < 123;
                // 添加下划线
                if (temp1 && (switchTag || temp2))
                    underLineBuilder.append("_");
                // 大写字母转为小写
                asciiNum += 32;
                switchTag = false;
            } else {
                switchTag = true;
            }
            underLineBuilder.append((char) (asciiNum));
        }
        return underLineBuilder.toString();
    }

    /**
     * 文本格式化
     */
    public static void format() {
        try {
            QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
            String text = QuickNoteForm.quickNoteRSyntaxTextViewerManager.getTextByName(selectedName);

            String format;
            String selectedSyntax = (String) quickNoteForm.getSyntaxComboBox().getSelectedItem();
            if (StringUtils.isBlank(text) || StringUtils.isEmpty(selectedSyntax)) {
                return;
            }

            switch (selectedSyntax) {
                case SyntaxConstants.SYNTAX_STYLE_SQL:
                    switch (App.config.getSqlDialect()) {
                        case "MariaDB":
                            format = SqlFormatter.of(Dialect.MariaDb).format(text, "    ");
                            break;
                        case "MySQL":
                            format = SqlFormatter.of(Dialect.MySql).format(text, "    ");
                            break;
                        case "PostgreSQL":
                            format = SqlFormatter.of(Dialect.PostgreSql).format(text, "    ");
                            break;
                        case "IBM DB2":
                            format = SqlFormatter.of(Dialect.Db2).format(text, "    ");
                            break;
                        case "Oracle PL/SQL":
                            format = SqlFormatter.of(Dialect.PlSql).format(text, "    ");
                            break;
                        case "Couchbase N1QL":
                            format = SqlFormatter.of(Dialect.N1ql).format(text, "    ");
                            break;
                        case "Amazon Redshift":
                            format = SqlFormatter.of(Dialect.Redshift).format(text, "    ");
                            break;
                        case "Spark":
                            format = SqlFormatter.of(Dialect.SparkSql).format(text, "    ");
                            break;
                        case "SQL Server Transact-SQL":
                            format = SqlFormatter.of(Dialect.TSql).format(text, "    ");
                            break;
                        default:
                            format = SqlFormatter.of(Dialect.StandardSql).format(text, "    ");
                    }
                    break;
                case SyntaxConstants.SYNTAX_STYLE_JSON:
                case SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS:
                    try {
                        format = JSONUtil.toJsonPrettyStr(text);
                    } catch (Exception e1) {
                        log.error(ExceptionUtils.getStackTrace(e1));
                        format = JSONUtil.formatJsonStr(text);
                    }
                    break;

                case SyntaxConstants.SYNTAX_STYLE_JAVA:
//                    format = new Formatter().formatSource(text);
                    Jalopy jalopy = new Jalopy();

                    StringWriter stringWriter = new StringWriter();
                    File tempFile = FileUtil.touch(App.tempDir + File.separator + "temp.java");
                    FileUtil.writeUtf8String(text, tempFile);
                    jalopy.setInput(tempFile);
                    jalopy.setOutput(stringWriter);
                    boolean result = jalopy.format();
                    if (!result) {
                        throw new Exception("格式化失败！");
                    }

                    format = stringWriter.toString();
                    break;

                default:
                    JOptionPane.showMessageDialog(App.mainFrame, "尚不支持对该语言格式化！\n", "不支持该语言",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
            }

            QuickNoteForm.quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea().setText(format);
            QuickNoteForm.quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea().setCaretPosition(0);

            QuickNoteIndicatorTools.showTips("已格式化：" + selectedName, QuickNoteIndicatorTools.TipsLevel.SUCCESS);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(App.mainFrame, "格式化失败！\n\n" + e.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            log.error(ExceptionUtils.getStackTrace(e));
        }

    }
}
