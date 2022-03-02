package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONUtil;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import com.google.common.collect.Lists;
import com.google.googlejavaformat.java.Formatter;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.component.FindReplaceBar;
import com.luoboduner.moo.tool.ui.component.QuickNoteSyntaxTextViewer;
import com.luoboduner.moo.tool.ui.component.QuickNoteSyntaxTextViewerManager;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.QuickNoteIndicatorTools;
import com.luoboduner.moo.tool.util.SqliteUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

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
        QuickNoteSyntaxTextViewerManager quickNoteSyntaxTextViewerManager = QuickNoteForm.quickNoteSyntaxTextViewerManager;

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
                tQuickNote.setContent(quickNoteSyntaxTextViewerManager.getCurrentText());
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
                int selectedRow = quickNoteForm.getNoteListTable().getSelectedRow();
                QuickNoteSyntaxTextViewer.ignoreQuickSave = true;
                try {
                    viewByRowNum(selectedRow);
                } catch (Exception e1) {
                    log.error(e1.toString());
                } finally {
                    QuickNoteSyntaxTextViewer.ignoreQuickSave = false;
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
                    if (selectedName != null && !QuickNoteSyntaxTextViewer.ignoreQuickSave) {
                        TQuickNote tQuickNote = new TQuickNote();
                        tQuickNote.setName(selectedName);
                        tQuickNote.setSyntax(syntaxName);
                        String now = SqliteUtil.nowDateForSqlite();
                        tQuickNote.setModifiedTime(now);

                        quickNoteMapper.updateByName(tQuickNote);

                        quickNoteSyntaxTextViewerManager.removeRTextScrollPane(selectedName);
                        RTextScrollPane syntaxTextViewer = quickNoteSyntaxTextViewerManager.getRTextScrollPane(selectedName);
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

                if (selectedName != null && !QuickNoteSyntaxTextViewer.ignoreQuickSave) {

                    TQuickNote tQuickNote = new TQuickNote();
                    tQuickNote.setName(selectedName);
                    tQuickNote.setFontName(fontName);
                    String now = SqliteUtil.nowDateForSqlite();
                    tQuickNote.setModifiedTime(now);

                    quickNoteMapper.updateByName(tQuickNote);
                    App.config.setQuickNoteFontName(fontName);
                    App.config.save();

                    quickNoteSyntaxTextViewerManager.removeRTextScrollPane(selectedName);
                    RTextScrollPane syntaxTextViewer = quickNoteSyntaxTextViewerManager.getRTextScrollPane(selectedName);
                    quickNoteForm.getContentSplitPane().setLeftComponent(syntaxTextViewer);
                    syntaxTextViewer.updateUI();
                }
            }
        });

        // 字体大小下拉框事件
        quickNoteForm.getFontSizeComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int fontSize = Integer.parseInt(e.getItem().toString());

                if (selectedName != null && !QuickNoteSyntaxTextViewer.ignoreQuickSave) {
                    TQuickNote tQuickNote = new TQuickNote();
                    tQuickNote.setName(selectedName);
                    tQuickNote.setFontSize(String.valueOf(fontSize));
                    String now = SqliteUtil.nowDateForSqlite();
                    tQuickNote.setModifiedTime(now);

                    quickNoteMapper.updateByName(tQuickNote);

                    App.config.setQuickNoteFontSize(fontSize);
                    App.config.save();

                    quickNoteSyntaxTextViewerManager.removeRTextScrollPane(selectedName);
                    RTextScrollPane syntaxTextViewer = quickNoteSyntaxTextViewerManager.getRTextScrollPane(selectedName);
                    quickNoteForm.getContentSplitPane().setLeftComponent(syntaxTextViewer);
                    syntaxTextViewer.updateUI();
                }
            }
        });

        // 自动换行按钮事件
        quickNoteForm.getWrapButton().addActionListener(e -> {
            RSyntaxTextArea view = quickNoteSyntaxTextViewerManager.getCurrentRSyntaxTextArea();
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

                            RTextScrollPane syntaxTextViewer = quickNoteSyntaxTextViewerManager.getRTextScrollPane(name);
                            quickNoteForm.getContentSplitPane().setLeftComponent(syntaxTextViewer);
                            quickNoteSyntaxTextViewerManager.removeRTextScrollPane(tQuickNoteBefore.getName());
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
                    QuickNoteSyntaxTextViewer.ignoreQuickSave = true;
                    try {
                        viewByRowNum(selectedRow);
                    } catch (Exception e1) {
                        log.error(e1.toString());
                    } finally {
                        QuickNoteSyntaxTextViewer.ignoreQuickSave = false;
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
                quickNoteForm.getContentSplitPane().setDividerLocation((int) (totalWidth * 0.62));
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
                    if (selectedName != null && !QuickNoteSyntaxTextViewer.ignoreQuickSave) {
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
    }

    public static void showFindPanel() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        QuickNoteSyntaxTextViewerManager quickNoteSyntaxTextViewerManager = QuickNoteForm.quickNoteSyntaxTextViewerManager;
        quickNoteForm.getFindReplacePanel().removeAll();
        quickNoteForm.getFindReplacePanel().setDoubleBuffered(true);
        RSyntaxTextArea view = quickNoteSyntaxTextViewerManager.getCurrentRSyntaxTextArea();
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
        QuickNoteSyntaxTextViewerManager quickNoteSyntaxTextViewerManager = QuickNoteForm.quickNoteSyntaxTextViewerManager;

        quickNoteForm.getFindReplacePanel().removeAll();
        quickNoteForm.getFindReplacePanel().setVisible(false);

        String name = quickNoteForm.getNoteListTable().getValueAt(rowNum, 1).toString();
        selectedName = name;

        quickNoteSyntaxTextViewerManager.removeRTextScrollPane(name);
        RTextScrollPane syntaxTextViewer = quickNoteSyntaxTextViewerManager.getRTextScrollPane(name);

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

        syntaxTextViewer.updateUI();
    }

    /**
     * Default File Name
     *
     * @return
     */
    @NotNull
    private static String getDefaultFileName() {
        return "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
    }

    /**
     * 快捷替换
     */
    private static void quickReplace() {
        try {
            QuickNoteSyntaxTextViewer.ignoreQuickSave = true;
            QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
            RSyntaxTextArea view = QuickNoteForm.quickNoteSyntaxTextViewerManager.getCurrentRSyntaxTextArea();

            String content = view.getText();

            String[] splits = content.split("\n");

            List<String> target = Lists.newArrayList();
            for (String split : splits) {

                if (quickNoteForm.getTrimBlankCheckBox().isSelected()) {
                    split = split.replace(" ", "");
                }

                if (quickNoteForm.getClearTabTCheckBox().isSelected()) {
                    split = split.replace("\t", "");
                }

                // ------------

                if (quickNoteForm.getScientificToNormalCheckBox().isSelected()) {
                    BigDecimal bigDecimal = NumberUtil.toBigDecimal(split);
                    split = bigDecimal.toString();
                }

                if (quickNoteForm.getToThousandthCheckBox().isSelected()) {
                    split = toThousandth(split);
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
                view.setText(StringUtils.join(target, "','"));
            } else if (quickNoteForm.getEnterToCommaDoubleQuotesCheckBox().isSelected()) {
                view.setText(StringUtils.join(target, "\",\""));
            } else {
                view.setText(StringUtils.join(target, "\n"));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(App.mainFrame, "转换失败！\n\n" + e.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            log.error(ExceptionUtils.getStackTrace(e));
        } finally {
            QuickNoteSyntaxTextViewer.ignoreQuickSave = false;
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
                        QuickNoteForm.quickNoteSyntaxTextViewerManager.removeRTextScrollPane(name);
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
    public static void quickSave(boolean refreshModifiedTime) {
        String now = SqliteUtil.nowDateForSqlite();
        if (selectedName != null) {
            TQuickNote tQuickNote = new TQuickNote();
            tQuickNote.setName(selectedName);

            String text = QuickNoteForm.quickNoteSyntaxTextViewerManager.getTextByName(selectedName);
            tQuickNote.setContent(text);
            if (refreshModifiedTime) {
                tQuickNote.setModifiedTime(now);
            }

            quickNoteMapper.updateByName(tQuickNote);
        }

        QuickNoteIndicatorTools.showTips("已保存：" + selectedName, QuickNoteIndicatorTools.TipsLevel.SUCCESS);
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
     * 文本格式化
     */
    public static void format() {
        try {
            QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
            String text = QuickNoteForm.quickNoteSyntaxTextViewerManager.getTextByName(selectedName);

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
                    format = new Formatter().formatSource(text);
                    break;

                default:
                    JOptionPane.showMessageDialog(App.mainFrame, "尚不支持对该语言格式化！\n", "不支持该语言",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
            }

            QuickNoteForm.quickNoteSyntaxTextViewerManager.getCurrentRSyntaxTextArea().setText(format);
            QuickNoteForm.quickNoteSyntaxTextViewerManager.getCurrentRSyntaxTextArea().setCaretPosition(0);

            QuickNoteIndicatorTools.showTips("已格式化：" + selectedName, QuickNoteIndicatorTools.TipsLevel.SUCCESS);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(App.mainFrame, "格式化失败！\n\n" + e.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            log.error(ExceptionUtils.getStackTrace(e));
        }

    }
}
