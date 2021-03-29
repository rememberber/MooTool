package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.json.JSONUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TJsonBeautyMapper;
import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.FindResultForm;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.ui.frame.FindResultFrame;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import com.luoboduner.moo.tool.util.TextAreaUtil;
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
 * Json格式化事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/9/6.
 */
@Slf4j
public class JsonBeautyListener {

    private static TJsonBeautyMapper jsonBeautyMapper = MybatisUtil.getSqlSession().getMapper(TJsonBeautyMapper.class);

    public static String selectedNameJson;

    public static void addListeners() {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();

        // 格式化按钮事件
        jsonBeautyForm.getBeautifyButton().addActionListener(e -> {
            String jsonText = jsonBeautyForm.getTextArea().getText();
            jsonBeautyForm.getTextArea().setText(formatJson(jsonText));
            jsonBeautyForm.getTextArea().setCaretPosition(0);
        });

        // 保存按钮事件
        jsonBeautyForm.getSaveButton().addActionListener(e -> {
            if (StringUtils.isEmpty(selectedNameJson)) {
                selectedNameJson = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            }
            String name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", selectedNameJson);
            if (StringUtils.isNotBlank(name)) {
                TJsonBeauty tJsonBeauty = jsonBeautyMapper.selectByName(name);
                if (tJsonBeauty == null) {
                    tJsonBeauty = new TJsonBeauty();
                }
                String now = SqliteUtil.nowDateForSqlite();
                tJsonBeauty.setName(name);
                tJsonBeauty.setContent(JsonBeautyForm.getInstance().getTextArea().getText());
                tJsonBeauty.setCreateTime(now);
                tJsonBeauty.setModifiedTime(now);
                if (tJsonBeauty.getId() == null) {
                    jsonBeautyMapper.insert(tJsonBeauty);
                    JsonBeautyForm.initListTable();
                    selectedNameJson = name;
                } else {
                    jsonBeautyMapper.updateByPrimaryKey(tJsonBeauty);
                }

            }
        });

        // 点击左侧表格事件
        jsonBeautyForm.getNoteListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                quickSave(false);
                int selectedRow = jsonBeautyForm.getNoteListTable().getSelectedRow();
                String name = jsonBeautyForm.getNoteListTable().getValueAt(selectedRow, 1).toString();
                selectedNameJson = name;
                TJsonBeauty tJsonBeauty = jsonBeautyMapper.selectByName(name);
                jsonBeautyForm.getTextArea().setText(tJsonBeauty.getContent());
                jsonBeautyForm.getTextArea().setCaretPosition(0);
                jsonBeautyForm.getScrollPane().getVerticalScrollBar().setValue(0);
                jsonBeautyForm.getScrollPane().getHorizontalScrollBar().setValue(0);
                jsonBeautyForm.getTextArea().updateUI();
                super.mousePressed(e);
            }
        });

        // 文本域按键事件
        jsonBeautyForm.getTextArea().addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_S) {
                    quickSave(true);
                } else if (evt.isControlDown() && evt.isShiftDown() && evt.getKeyCode() == KeyEvent.VK_F) {
                    String jsonText = jsonBeautyForm.getTextArea().getText();
                    jsonBeautyForm.getTextArea().setText(formatJson(jsonText));
                    jsonBeautyForm.getTextArea().setCaretPosition(0);
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_F) {
                    jsonBeautyForm.getFindReplacePanel().setVisible(true);
                    jsonBeautyForm.getFindTextField().grabFocus();
                    jsonBeautyForm.getFindTextField().selectAll();
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_R) {
                    jsonBeautyForm.getFindReplacePanel().setVisible(true);
                    jsonBeautyForm.getReplaceTextField().grabFocus();
                    jsonBeautyForm.getReplaceTextField().selectAll();
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_N) {
                    newJson();
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_D) {
                    TextAreaUtil.deleteSelectedLine(jsonBeautyForm.getTextArea());
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        // 删除按钮事件
        jsonBeautyForm.getDeleteButton().addActionListener(e -> {
            deleteFiles(jsonBeautyForm);
        });

        // 字体名称下拉框事件
        jsonBeautyForm.getFontNameComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String fontName = e.getItem().toString();
                int fontSize = Integer.parseInt(jsonBeautyForm.getFontSizeComboBox().getSelectedItem().toString());
                Font font = new Font(fontName, Font.PLAIN, fontSize);
                jsonBeautyForm.getTextArea().setFont(font);

                App.config.setJsonBeautyFontName(fontName);
                App.config.setJsonBeautyFontSize(fontSize);
                App.config.save();
            }
        });

        // 字体大小下拉框事件
        jsonBeautyForm.getFontSizeComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int fontSize = Integer.parseInt(e.getItem().toString());
                String fontName = jsonBeautyForm.getFontNameComboBox().getSelectedItem().toString();
                Font font = new Font(fontName, Font.PLAIN, fontSize);
                jsonBeautyForm.getTextArea().setFont(font);

                App.config.setJsonBeautyFontName(fontName);
                App.config.setJsonBeautyFontSize(fontSize);
                App.config.save();
            }
        });

        // 自动换行按钮事件
        jsonBeautyForm.getWrapButton().addActionListener(e -> {
            jsonBeautyForm.getTextArea().setLineWrap(!jsonBeautyForm.getTextArea().getLineWrap());
        });

        // 添加按钮事件
        jsonBeautyForm.getAddButton().addActionListener(e -> {
            newJson();
        });

        // 左侧列表鼠标点击事件（显示下方删除按钮）
        jsonBeautyForm.getNoteListTable().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                jsonBeautyForm.getDeletePanel().setVisible(true);
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

        // 文本域鼠标点击事件，隐藏删除按钮
        jsonBeautyForm.getTextArea().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                jsonBeautyForm.getDeletePanel().setVisible(false);
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
        jsonBeautyForm.getNoteListTable().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    int selectedRow = jsonBeautyForm.getNoteListTable().getSelectedRow();
                    int noteId = Integer.parseInt(String.valueOf(jsonBeautyForm.getNoteListTable().getValueAt(selectedRow, 0)));
                    String name = String.valueOf(jsonBeautyForm.getNoteListTable().getValueAt(selectedRow, 1));
                    if (StringUtils.isNotBlank(name)) {
                        TJsonBeauty tJsonBeauty = new TJsonBeauty();
                        tJsonBeauty.setId(noteId);
                        tJsonBeauty.setName(name);
                        try {
                            jsonBeautyMapper.updateByPrimaryKeySelective(tJsonBeauty);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，可能和已有笔记重名");
                            JsonBeautyForm.initListTable();
                            log.error(e.toString());
                        }
                    }
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(jsonBeautyForm);
                }
            }
        });

        jsonBeautyForm.getFindButton().addActionListener(e -> {
            jsonBeautyForm.getFindReplacePanel().setVisible(true);
            jsonBeautyForm.getFindTextField().grabFocus();
            jsonBeautyForm.getFindTextField().selectAll();
        });

        jsonBeautyForm.getFindTextField().addKeyListener(new KeyListener() {
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

        jsonBeautyForm.getListItemButton().addActionListener(e -> {
            int currentDividerLocation = jsonBeautyForm.getSplitPane().getDividerLocation();
            if (currentDividerLocation < 5) {
                jsonBeautyForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
            } else {
                jsonBeautyForm.getSplitPane().setDividerLocation(0);
            }
        });

        jsonBeautyForm.getExportButton().addActionListener(e -> {
            int[] selectedRows = jsonBeautyForm.getNoteListTable().getSelectedRows();

            try {
                if (selectedRows.length > 0) {
                    JFileChooser fileChooser = new JFileChooser(App.config.getJsonBeautyExportPath());
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(jsonBeautyForm.getJsonBeautyPanel());
                    String exportPath;
                    if (approve == JFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        App.config.setJsonBeautyExportPath(exportPath);
                        App.config.save();
                    } else {
                        return;
                    }

                    for (int row : selectedRows) {
                        Integer selectedId = (Integer) jsonBeautyForm.getNoteListTable().getValueAt(row, 0);
                        TJsonBeauty tJsonBeauty = jsonBeautyMapper.selectByPrimaryKey(selectedId);
                        File exportFile = FileUtil.touch(exportPath + File.separator + tJsonBeauty.getName() + ".json");
                        FileUtil.writeUtf8String(tJsonBeauty.getContent(), exportFile);
                    }
                    JOptionPane.showMessageDialog(jsonBeautyForm.getJsonBeautyPanel(), "导出成功！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(exportPath));
                    } catch (Exception e2) {
                        log.error(ExceptionUtils.getStackTrace(e2));
                    }
                } else {
                    JOptionPane.showMessageDialog(jsonBeautyForm.getJsonBeautyPanel(), "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(jsonBeautyForm.getJsonBeautyPanel(), "导出失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getDoFindButton().addActionListener(e -> find());

        jsonBeautyForm.getFindReplaceCloseLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                jsonBeautyForm.getFindReplacePanel().setVisible(false);
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

        jsonBeautyForm.getDoReplaceButton().addActionListener(e -> replace());
        jsonBeautyForm.getReplaceTextField().addKeyListener(new KeyListener() {
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

        jsonBeautyForm.getFindUseRegexCheckBox().addActionListener(e -> {
            boolean selected = jsonBeautyForm.getFindUseRegexCheckBox().isSelected();
            if (selected) {
                jsonBeautyForm.getFindWordsCheckBox().setSelected(false);
                jsonBeautyForm.getFindWordsCheckBox().setEnabled(false);
            } else {
                jsonBeautyForm.getFindWordsCheckBox().setEnabled(true);
            }
        });

    }

    private static void deleteFiles(JsonBeautyForm jsonBeautyForm) {
        try {
            int[] selectedRows = jsonBeautyForm.getNoteListTable().getSelectedRows();

            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(App.mainFrame, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int isDelete = JOptionPane.showConfirmDialog(App.mainFrame, "确认删除？", "确认", JOptionPane.YES_NO_OPTION);
                if (isDelete == JOptionPane.YES_OPTION) {
                    DefaultTableModel tableModel = (DefaultTableModel) jsonBeautyForm.getNoteListTable().getModel();

                    for (int i = selectedRows.length; i > 0; i--) {
                        int selectedRow = jsonBeautyForm.getNoteListTable().getSelectedRow();
                        Integer id = (Integer) tableModel.getValueAt(selectedRow, 0);
                        jsonBeautyMapper.deleteByPrimaryKey(id);

                        tableModel.removeRow(selectedRow);
                        jsonBeautyForm.getNoteListTable().updateUI();
                    }
                    selectedNameJson = null;
                    JsonBeautyForm.initListTable();
                }
            }
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(App.mainFrame, "删除失败！\n\n" + e1.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            log.error(e1.toString());
        }
    }

    /**
     * save for quick key and item change
     *
     * @param refreshModifiedTime
     */
    private static void quickSave(boolean refreshModifiedTime) {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();
        String now = SqliteUtil.nowDateForSqlite();
        if (selectedNameJson != null) {
            TJsonBeauty tJsonBeauty = new TJsonBeauty();
            tJsonBeauty.setName(selectedNameJson);
            tJsonBeauty.setContent(jsonBeautyForm.getTextArea().getText());
            if (refreshModifiedTime) {
                tJsonBeauty.setModifiedTime(now);
            }
            jsonBeautyMapper.updateByName(tJsonBeauty);
        } else {
            String tempName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            String name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", tempName);
            if (StringUtils.isNotBlank(name)) {
                TJsonBeauty tJsonBeauty = new TJsonBeauty();
                tJsonBeauty.setName(name);
                tJsonBeauty.setContent(jsonBeautyForm.getTextArea().getText());
                tJsonBeauty.setCreateTime(now);
                tJsonBeauty.setModifiedTime(now);

                jsonBeautyMapper.insert(tJsonBeauty);
                JsonBeautyForm.initListTable();
                selectedNameJson = name;
            }
        }
    }

    private static void newJson() {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();
        jsonBeautyForm.getTextArea().setText("");
        selectedNameJson = null;
    }

    private static String formatJson(String jsonText) {
        try {
            jsonText = JSONUtil.toJsonPrettyStr(jsonText);
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(App.mainFrame, "格式化失败！\n\n" + e1.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            log.error(ExceptionUtils.getStackTrace(e1));
            try {
                jsonText = JSONUtil.formatJsonStr(jsonText);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(App.mainFrame, "格式化失败！\n\n" + e.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e));
            }
        }
        return jsonText;
    }

    private static void find() {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();

        String content = jsonBeautyForm.getTextArea().getText();
        String findKeyWord = jsonBeautyForm.getFindTextField().getText();
        boolean isMatchCase = jsonBeautyForm.getFindMatchCaseCheckBox().isSelected();
        boolean isWords = jsonBeautyForm.getFindWordsCheckBox().isSelected();
        boolean useRegex = jsonBeautyForm.getFindUseRegexCheckBox().isSelected();

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
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();
        String target = jsonBeautyForm.getFindTextField().getText();
        String replacement = jsonBeautyForm.getReplaceTextField().getText();
        String content = jsonBeautyForm.getTextArea().getText();
        boolean isMatchCase = jsonBeautyForm.getFindMatchCaseCheckBox().isSelected();
        boolean isWords = jsonBeautyForm.getFindWordsCheckBox().isSelected();
        boolean useRegex = jsonBeautyForm.getFindUseRegexCheckBox().isSelected();

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

        jsonBeautyForm.getTextArea().setText(content);
        jsonBeautyForm.getTextArea().setCaretPosition(0);
        jsonBeautyForm.getScrollPane().getVerticalScrollBar().setValue(0);
        jsonBeautyForm.getScrollPane().getHorizontalScrollBar().setValue(0);
    }
}
