package com.luoboduner.moo.tool.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TJsonBeautyMapper;
import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.ui.form.fun.JsonBeautyForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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

    public static String selectedName;

    public static void addListeners() {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();

        // 格式化按钮事件
        jsonBeautyForm.getBeautifyButton().addActionListener(e -> {
            String jsonText = jsonBeautyForm.getTextArea().getText();
            jsonBeautyForm.getTextArea().setText(formatJson(jsonText));
        });

        jsonBeautyForm.getTextArea().addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_F) {
                    String jsonText = jsonBeautyForm.getTextArea().getText();
                    jsonBeautyForm.getTextArea().setText(formatJson(jsonText));
                } else if (evt.isControlDown() && evt.isShiftDown() && evt.getKeyCode() == KeyEvent.VK_F) {
                    String jsonText = jsonBeautyForm.getTextArea().getText();
                    jsonBeautyForm.getTextArea().setText(formatJson(jsonText));
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        // 保存按钮事件
        jsonBeautyForm.getSaveButton().addActionListener(e -> {
            if (StringUtils.isEmpty(selectedName)) {
                selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            }
            String name = JOptionPane.showInputDialog("名称", selectedName);
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
                    selectedName = name;
                } else {
                    jsonBeautyMapper.updateByPrimaryKey(tJsonBeauty);
                }

            }
        });

        // 点击左侧表格事件
        jsonBeautyForm.getNoteListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ThreadUtil.execute(() -> {
                    int selectedRow = jsonBeautyForm.getNoteListTable().getSelectedRow();
                    String name = jsonBeautyForm.getNoteListTable().getValueAt(selectedRow, 1).toString();
                    selectedName = name;
                    TJsonBeauty tJsonBeauty = jsonBeautyMapper.selectByName(name);
                    jsonBeautyForm.getTextArea().setText(tJsonBeauty.getContent());
                });
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
                    String now = SqliteUtil.nowDateForSqlite();
                    if (selectedName != null) {
                        TJsonBeauty tJsonBeauty = new TJsonBeauty();
                        tJsonBeauty.setName(selectedName);
                        tJsonBeauty.setContent(jsonBeautyForm.getTextArea().getText());
                        tJsonBeauty.setModifiedTime(now);

                        jsonBeautyMapper.updateByName(tJsonBeauty);
                    } else {
                        String tempName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
                        String name = JOptionPane.showInputDialog("名称", tempName);
                        TJsonBeauty tJsonBeauty = new TJsonBeauty();
                        tJsonBeauty.setName(name);
                        tJsonBeauty.setContent(jsonBeautyForm.getTextArea().getText());
                        tJsonBeauty.setCreateTime(now);
                        tJsonBeauty.setModifiedTime(now);

                        jsonBeautyMapper.insert(tJsonBeauty);
                        JsonBeautyForm.initListTable();
                        selectedName = name;
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        // 删除按钮事件
        jsonBeautyForm.getDeleteButton().addActionListener(e -> ThreadUtil.execute(() -> {
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
                        }
                        selectedName = null;
                        JsonBeautyForm.initListTable();
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(e1.toString());
            }
        }));

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
            jsonBeautyForm.getTextArea().setText("");
            selectedName = null;
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
                    String noteName = String.valueOf(jsonBeautyForm.getNoteListTable().getValueAt(selectedRow, 1));
                    TJsonBeauty tJsonBeauty = new TJsonBeauty();
                    tJsonBeauty.setId(noteId);
                    tJsonBeauty.setName(noteName);
                    try {
                        jsonBeautyMapper.updateByPrimaryKeySelective(tJsonBeauty);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，可能和已有笔记重名");
                        JsonBeautyForm.initListTable();
                        log.error(e.toString());
                    }
                }
            }
        });

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
}
