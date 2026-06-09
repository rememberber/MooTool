package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.util.FontUtils;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TJsonBeautyMapper;
import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.ui.component.FindReplaceBar;
import com.luoboduner.moo.tool.ui.dialog.JsonResultDialog;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

    /** 忽略 JOptionPane 关闭后回传到列表的 Enter 键，避免重命名弹框重复弹出 */
    private static boolean suppressListEnterRename;

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
                    JsonBeautyForm.initList();
                    selectedNameJson = name;
                } else {
                    jsonBeautyMapper.updateByPrimaryKey(tJsonBeauty);
                }

            }
        });

        // 点击左侧列表事件
        jsonBeautyForm.getNoteList().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);

                int index = jsonBeautyForm.getNoteList().locationToIndex(e.getPoint());
                if (index == -1) {
                    return;
                }

                ignoreQuickSave = true;
                try {
                    viewByIndex(index);
                } catch (Exception e2) {
                    log.error(e2.getMessage());
                } finally {
                    ignoreQuickSave = false;
                }
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
                } else if (((evt.isControlDown() || evt.isMetaDown()) && evt.isShiftDown() && evt.getKeyCode() == KeyEvent.VK_F) ||
                        evt.isMetaDown() && evt.isAltDown() && evt.getKeyCode() == KeyEvent.VK_L) {
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
                Font font = FontUtils.getCompositeFont(fontName, Font.PLAIN, fontSize);
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
                Font font = FontUtils.getCompositeFont(fontName, Font.PLAIN, fontSize);
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
        jsonBeautyForm.getNoteList().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (suppressListEnterRename) {
                        suppressListEnterRename = false;
                        return;
                    }
                    renameSelectedNote(jsonBeautyForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(jsonBeautyForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                    int selectedIndex = jsonBeautyForm.getNoteList().getSelectedIndex();
                    if (selectedIndex < 0) {
                        return;
                    }
                    ignoreQuickSave = true;
                    try {
                        viewByIndex(selectedIndex);
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
            int[] selectedIndices = jsonBeautyForm.getNoteList().getSelectedIndices();

            try {
                if (selectedIndices.length > 0) {
                    SystemFileChooser fileChooser = new SystemFileChooser(App.config.getJsonBeautyExportPath());
                    fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(jsonBeautyForm.getJsonBeautyPanel());
                    String exportPath;
                    if (approve == SystemFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        App.config.setJsonBeautyExportPath(exportPath);
                        App.config.save();
                    } else {
                        return;
                    }

                    DefaultListModel<TJsonBeauty> listModel = (DefaultListModel<TJsonBeauty>) jsonBeautyForm.getNoteList().getModel();
                    for (int index : selectedIndices) {
                        TJsonBeauty tJsonBeauty = jsonBeautyMapper.selectByPrimaryKey(listModel.getElementAt(index).getId());
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

        // 搜索框变更事件
        jsonBeautyForm.getSearchTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                JsonBeautyForm.initList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                JsonBeautyForm.initList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
//                JsonBeautyForm.initList();
            }
        });

        jsonBeautyForm.getCompressButton().addActionListener(e -> {
            String jsonText = jsonBeautyForm.getTextArea().getText();
            jsonBeautyForm.getTextArea().setText(JSONUtil.toJsonStr(JSONUtil.isTypeJSONArray(jsonText) ? JSONUtil.parseArray(jsonText) : JSONUtil.parseObj(jsonText), 0));
            jsonBeautyForm.getTextArea().setCaretPosition(0);
        });

        jsonBeautyForm.getCustomFormatButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                JSONConfig jsonConfig = new JSONConfig();
                jsonConfig.setIgnoreCase(jsonBeautyForm.getIgnoreCaseCheckBox().isSelected());
                jsonConfig.setCheckDuplicate(jsonBeautyForm.getCheckDuplicateCheckBox().isSelected());
                if (jsonBeautyForm.getKeySortCheckBox().isSelected()) {
                    jsonConfig.setKeyComparator((o1, o2) -> {
                        if (jsonBeautyForm.getIgnoreCaseCheckBox().isSelected()) {
                            return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
                        } else {
                            return o1.toString().compareTo(o2.toString());
                        }
                    });
                }
                jsonBeautyForm.getTextArea().setText(JSONUtil.toJsonPrettyStr(JSONUtil.parse(jsonText, jsonConfig)));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "格式化失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getJsonToXmlButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                JsonResultDialog jsonResultDialog = new JsonResultDialog("XML", "JSON转XML", "Display");
                String xmlStr = JSONUtil.toXmlStr(JSONUtil.isTypeJSONArray(jsonText) ? JSONUtil.parseArray(jsonText) : JSONUtil.parseObj(jsonText));
                xmlStr = "<root>" + xmlStr + "</root>";
                jsonResultDialog.setToTextArea(XmlReformatUtil.format(xmlStr));
                jsonResultDialog.setVisible(true);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "转换失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getXmlToJsonButton().addActionListener(e -> {
            try {
                JsonResultDialog jsonResultDialog = new JsonResultDialog("XML", "请输入XML文本：", "Input");
                jsonResultDialog.setVisible(true);
                String inputValue = JsonResultDialog.textInputValue;
                if (StringUtils.isBlank(inputValue)) {
                    return;
                }
                jsonBeautyForm.getTextArea().setText(JSONUtil.toJsonPrettyStr(JSONUtil.xmlToJson(inputValue)));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "转换失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getEscapeJsonButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                jsonBeautyForm.getTextArea().setText(JSONUtil.escape(jsonText));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "转义失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getEscapeStringButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                jsonBeautyForm.getTextArea().setText(StringEscapeUtils.escapeJava(jsonText));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "转义失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getUnescapeButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                jsonBeautyForm.getTextArea().setText(StringEscapeUtils.unescapeJson(jsonText));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "反转义失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getGetByJsonPathButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                String jsonPath = jsonBeautyForm.getJsonPathTextField().getText();
                if (StringUtils.isNotBlank(jsonPath)) {
                    JsonResultDialog jsonResultDialog = new JsonResultDialog("JSON", "根据JSON Path，取值如下：", "Display");
                    jsonResultDialog.setToTextArea(JSONUtil.toJsonPrettyStr(JSONUtil.getByPath(JSONUtil.parse(jsonText), jsonPath).toString()));
                    jsonResultDialog.setVisible(true);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "获取失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        jsonBeautyForm.getGetJsonPathButton().addActionListener(e -> {
            try {
                // alert:施工中，敬请期待
                JOptionPane.showMessageDialog(App.mainFrame, "施工中，敬请期待！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "获取失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        // 快捷替换按钮
        jsonBeautyForm.getMoreButton().addActionListener(e -> {
            int totalWidth = jsonBeautyForm.getContentSplitPane().getWidth();
            int currentDividerLocation = jsonBeautyForm.getContentSplitPane().getDividerLocation();

            if (totalWidth - currentDividerLocation < 10) {
                jsonBeautyForm.getMoreScrollPane().setVisible(true);
                jsonBeautyForm.getContentSplitPane().setDividerLocation((int) (totalWidth * 0.72));
            } else {
                jsonBeautyForm.getContentSplitPane().setDividerLocation(totalWidth);
                jsonBeautyForm.getMoreScrollPane().setVisible(false);
            }
        });

        jsonBeautyForm.getMoreCloseButton().addActionListener(e -> {
            jsonBeautyForm.getContentSplitPane().setDividerLocation(jsonBeautyForm.getContentSplitPane().getWidth());
            jsonBeautyForm.getMoreScrollPane().setVisible(false);
        });

        jsonBeautyForm.getBeanToJsonButton().addActionListener(e -> {
            try {
                JsonResultDialog jsonResultDialog = new JsonResultDialog("Java", "请输入JavaBean类代码：", "Input");
                jsonResultDialog.setVisible(true);
                String inputValue = JsonResultDialog.textInputValue;
                if (StringUtils.isBlank(inputValue)) {
                    return;
                }
                jsonBeautyForm.getTextArea().setText(MockDataGenerator.classCodeToJson(inputValue));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "转换失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        jsonBeautyForm.getJavaBeanToJSONButton().addActionListener(e -> {
            try {
                JsonResultDialog jsonResultDialog = new JsonResultDialog("Java", "请输入JavaBean类代码：", "Input");
                jsonResultDialog.setVisible(true);
                String inputValue = JsonResultDialog.textInputValue;
                if (StringUtils.isBlank(inputValue)) {
                    return;
                }
                jsonBeautyForm.getTextArea().setText(MockDataGenerator.classCodeToJson(inputValue));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "转换失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        jsonBeautyForm.getJsonToJavaBeanButton().addActionListener(e -> {
            try {
                JsonResultDialog jsonResultDialog = new JsonResultDialog("Java", "JSON转换JavaBean结果：", "Display");
                jsonResultDialog.setToTextArea(MockDataGenerator.jsonToClassCode(jsonBeautyForm.getTextArea().getText()));
                jsonResultDialog.setVisible(true);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "转换失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        jsonBeautyForm.getKeyValueSwapButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();

                JsonResultDialog jsonResultDialog = new JsonResultDialog("JSON", "Key-Value 互换结果:", "Display");
                jsonResultDialog.setToTextArea(JSONUtil.toJsonPrettyStr(JsonKeyValueSwapper.swapKeysAndValues(jsonText)));
                jsonResultDialog.setVisible(true);

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "转换失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        // 左侧列表增加右键菜单
        JPopupMenu noteListPopupMenu = new JPopupMenu();
        JMenuItem renameMenuItem = new JMenuItem("重命名");
        JMenuItem deleteMenuItem = new JMenuItem("删除");
        JMenuItem exportMenuItem = new JMenuItem("导出");
        noteListPopupMenu.add(renameMenuItem);
        noteListPopupMenu.add(deleteMenuItem);
        noteListPopupMenu.add(exportMenuItem);
        jsonBeautyForm.getNoteList().setComponentPopupMenu(noteListPopupMenu);

        renameMenuItem.addActionListener(e -> renameSelectedNote(jsonBeautyForm));

        deleteMenuItem.addActionListener(e -> {
            deleteFiles(jsonBeautyForm);
        });

        exportMenuItem.addActionListener(e -> {
            int[] selectedIndices = jsonBeautyForm.getNoteList().getSelectedIndices();

            try {
                if (selectedIndices.length > 0) {
                    SystemFileChooser fileChooser = new SystemFileChooser(App.config.getJsonBeautyExportPath());
                    fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(jsonBeautyForm.getJsonBeautyPanel());
                    String exportPath;
                    if (approve == SystemFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        App.config.setJsonBeautyExportPath(exportPath);
                        App.config.save();
                    } else {
                        return;
                    }

                    DefaultListModel<TJsonBeauty> listModel = (DefaultListModel<TJsonBeauty>) jsonBeautyForm.getNoteList().getModel();
                    for (int index : selectedIndices) {
                        TJsonBeauty tJsonBeauty = jsonBeautyMapper.selectByPrimaryKey(listModel.getElementAt(index).getId());
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

    private static void viewByIndex(int index) {
        if (index < 0) {
            return;
        }
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();

        DefaultListModel<TJsonBeauty> listModel = (DefaultListModel<TJsonBeauty>) jsonBeautyForm.getNoteList().getModel();
        String name = listModel.getElementAt(index).getName();
        selectedNameJson = name;
        setContentByName(name);
    }

    private static void renameSelectedNote(JsonBeautyForm jsonBeautyForm) {
        int selectedIndex = jsonBeautyForm.getNoteList().getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }
        DefaultListModel<TJsonBeauty> model = (DefaultListModel<TJsonBeauty>) jsonBeautyForm.getNoteList().getModel();
        TJsonBeauty item = model.getElementAt(selectedIndex);
        String beforeName = item.getName();
        if (StringUtils.isBlank(beforeName)) {
            return;
        }
        suppressListEnterRename = true;
        String afterName = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", beforeName);
        if (StringUtils.isBlank(afterName) || afterName.equals(beforeName)) {
            return;
        }
        try {
            TJsonBeauty tJsonBeauty = new TJsonBeauty();
            tJsonBeauty.setId(item.getId());
            tJsonBeauty.setName(afterName);
            tJsonBeauty.setModifiedTime(SqliteUtil.nowDateForSqlite());
            jsonBeautyMapper.updateByPrimaryKeySelective(tJsonBeauty);
            selectedNameJson = afterName;
            item.setName(afterName);
            model.set(selectedIndex, item);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，和已有文件重名");
            JsonBeautyForm.initList();
            log.error(e.toString());
        }
    }

    private static void setContentByName(String name) {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();
        TJsonBeauty tJsonBeauty = jsonBeautyMapper.selectByName(name);
        jsonBeautyForm.getTextArea().setText(tJsonBeauty.getContent());
        jsonBeautyForm.getTextArea().setCaretPosition(0);
        jsonBeautyForm.getScrollPane().getVerticalScrollBar().setValue(0);
        jsonBeautyForm.getScrollPane().getHorizontalScrollBar().setValue(0);
        JsonBeautyForm.initTextAreaFont();
//        jsonBeautyForm.getTextArea().updateUI();
    }

    private static void deleteFiles(JsonBeautyForm jsonBeautyForm) {
        try {
            int[] selectedIndices = jsonBeautyForm.getNoteList().getSelectedIndices();

            if (selectedIndices.length == 0) {
                JOptionPane.showMessageDialog(App.mainFrame, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int isDelete = JOptionPane.showConfirmDialog(App.mainFrame, "确认删除？", "确认", JOptionPane.YES_NO_OPTION);
                if (isDelete == JOptionPane.YES_OPTION) {
                    DefaultListModel<TJsonBeauty> listModel = (DefaultListModel<TJsonBeauty>) jsonBeautyForm.getNoteList().getModel();

                    for (int selectedIndex : selectedIndices) {
                        Integer id = listModel.getElementAt(selectedIndex).getId();
                        jsonBeautyMapper.deleteByPrimaryKey(id);
                    }
                    selectedNameJson = null;
                    JsonBeautyForm.initList();
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
                JsonBeautyForm.initList();
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
            JsonBeautyForm.initList();
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
