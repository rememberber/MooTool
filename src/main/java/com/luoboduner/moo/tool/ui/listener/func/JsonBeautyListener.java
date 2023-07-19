package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TJsonBeautyMapper;
import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.ui.component.FindReplaceBar;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

    public static boolean ignoreQuickSave;

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
                int selectedRow = jsonBeautyForm.getNoteListTable().getSelectedRow();
                ignoreQuickSave = true;
                try {
                    viewByRowNum(selectedRow);
                } catch (Exception e2) {
                    log.error(e2.getMessage());
                } finally {
                    ignoreQuickSave = false;
                }

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
                if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_S) {
                    quickSave(true);
                } else if ((evt.isControlDown() || evt.isMetaDown()) && evt.isShiftDown() && evt.getKeyCode() == KeyEvent.VK_F) {
                    String jsonText = jsonBeautyForm.getTextArea().getText();
                    jsonBeautyForm.getTextArea().setText(formatJson(jsonText));
                    jsonBeautyForm.getTextArea().setCaretPosition(0);
                } else if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_F) {
                    showFindPanel();
                } else if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_R) {
                    showFindPanel();
                } else if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_N) {
                    newJson();
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        jsonBeautyForm.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (ignoreQuickSave) {
                    return;
                }
                quickSave(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (ignoreQuickSave) {
                    return;
                }
                quickSave(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (ignoreQuickSave) {
                    return;
                }
                quickSave(true);
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
                            JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，和已有文件重名");
                            JsonBeautyForm.initListTable();
                            log.error(e.toString());
                        }
                    }
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(jsonBeautyForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                    int selectedRow = jsonBeautyForm.getNoteListTable().getSelectedRow();
                    ignoreQuickSave = true;
                    try {
                        viewByRowNum(selectedRow);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    } finally {
                        ignoreQuickSave = false;
                    }
                }
            }
        });

        jsonBeautyForm.getFindButton().addActionListener(e -> {
            showFindPanel();
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

    }

    private static void viewByRowNum(int selectedRow) {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();

        String name = jsonBeautyForm.getNoteListTable().getValueAt(selectedRow, 1).toString();
        selectedNameJson = name;
        setContentByName(name);
    }

    private static void setContentByName(String name) {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();
        TJsonBeauty tJsonBeauty = jsonBeautyMapper.selectByName(name);
        jsonBeautyForm.getTextArea().setText(tJsonBeauty.getContent());
        jsonBeautyForm.getTextArea().setCaretPosition(0);
        jsonBeautyForm.getScrollPane().getVerticalScrollBar().setValue(0);
        jsonBeautyForm.getScrollPane().getHorizontalScrollBar().setValue(0);
//        jsonBeautyForm.getTextArea().updateUI();
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

                    for (int i = 0; i < selectedRows.length; i++) {
                        int selectedRow = selectedRows[i];
                        Integer id = (Integer) tableModel.getValueAt(selectedRow, 0);
                        jsonBeautyMapper.deleteByPrimaryKey(id);
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
        String name = getDefaultFileName();
        name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", name);
        if (StringUtils.isNotBlank(name)) {
            TJsonBeauty tJsonBeauty = jsonBeautyMapper.selectByName(name);
            if (tJsonBeauty == null) {
                tJsonBeauty = new TJsonBeauty();
            } else {
                JOptionPane.showMessageDialog(App.mainFrame, "存在同名文件，请重新命名！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String now = SqliteUtil.nowDateForSqlite();
            tJsonBeauty.setName(name);
            tJsonBeauty.setCreateTime(now);
            tJsonBeauty.setModifiedTime(now);
            jsonBeautyMapper.insert(tJsonBeauty);
            JsonBeautyForm.initListTable();
        }
    }

    static String formatJson(String jsonText) {
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

    public static void showFindPanel() {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();
        jsonBeautyForm.getFindReplacePanel().removeAll();
        jsonBeautyForm.getFindReplacePanel().setDoubleBuffered(true);
        FindReplaceBar findReplaceBar = new FindReplaceBar(jsonBeautyForm.getTextArea());
        jsonBeautyForm.getFindReplacePanel().add(findReplaceBar.getFindOptionPanel());
        jsonBeautyForm.getFindReplacePanel().setVisible(true);
        jsonBeautyForm.getFindReplacePanel().updateUI();
        findReplaceBar.getFindField().setText(jsonBeautyForm.getTextArea().getSelectedText());
        findReplaceBar.getFindField().grabFocus();
        findReplaceBar.getFindField().selectAll();
    }

    /**
     * Default File Name
     *
     * @return
     */
    private static String getDefaultFileName() {
        return "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
    }
}
