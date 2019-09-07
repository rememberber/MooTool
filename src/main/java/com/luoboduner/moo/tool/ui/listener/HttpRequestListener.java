package com.luoboduner.moo.tool.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.fun.HttpRequestForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
 * Http请求事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/9/7.
 */
@Slf4j
public class HttpRequestListener {

    private static TQuickNoteMapper quickNoteMapper = MybatisUtil.getSqlSession().getMapper(TQuickNoteMapper.class);

    public static String selectedName;

    public static void addListeners() {
        HttpRequestForm httpRequestForm = HttpRequestForm.getInstance();

        httpRequestForm.getSaveButton().addActionListener(e -> {
            if (StringUtils.isEmpty(selectedName)) {
                selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            }
            String name = JOptionPane.showInputDialog("名称", selectedName);
            if (StringUtils.isNotBlank(name)) {
                TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
                if (tQuickNote == null) {
                    tQuickNote = new TQuickNote();
                }
                String now = SqliteUtil.nowDateForSqlite();
                tQuickNote.setName(name);
                tQuickNote.setContent(HttpRequestForm.getInstance().getTextArea().getText());
                tQuickNote.setCreateTime(now);
                tQuickNote.setModifiedTime(now);
                if (tQuickNote.getId() == null) {
                    quickNoteMapper.insert(tQuickNote);
                    HttpRequestForm.initNoteListTable();
                    selectedName = name;
                } else {
                    quickNoteMapper.updateByPrimaryKey(tQuickNote);
                }

            }
        });

        // 点击左侧表格事件
        httpRequestForm.getNoteListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ThreadUtil.execute(() -> {
                    int selectedRow = httpRequestForm.getNoteListTable().getSelectedRow();
                    String name = httpRequestForm.getNoteListTable().getValueAt(selectedRow, 1).toString();
                    selectedName = name;
                    TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
                    httpRequestForm.getTextArea().setText(tQuickNote.getContent());
                });
                super.mousePressed(e);
            }
        });

        // 文本域按键事件
        httpRequestForm.getTextArea().addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_S) {
                    String now = SqliteUtil.nowDateForSqlite();
                    if (selectedName != null) {
                        TQuickNote tQuickNote = new TQuickNote();
                        tQuickNote.setName(selectedName);
                        tQuickNote.setContent(httpRequestForm.getTextArea().getText());
                        tQuickNote.setModifiedTime(now);

                        quickNoteMapper.updateByName(tQuickNote);
                    } else {
                        String tempName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
                        String name = JOptionPane.showInputDialog("名称", tempName);
                        TQuickNote tQuickNote = new TQuickNote();
                        tQuickNote.setName(name);
                        tQuickNote.setContent(httpRequestForm.getTextArea().getText());
                        tQuickNote.setCreateTime(now);
                        tQuickNote.setModifiedTime(now);

                        quickNoteMapper.insert(tQuickNote);
                        HttpRequestForm.initNoteListTable();
                        selectedName = name;
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        // 删除按钮事件
        httpRequestForm.getDeleteButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                int[] selectedRows = httpRequestForm.getNoteListTable().getSelectedRows();

                if (selectedRows.length == 0) {
                    JOptionPane.showMessageDialog(App.mainFrame, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(App.mainFrame, "确认删除？", "确认", JOptionPane.YES_NO_OPTION);
                    if (isDelete == JOptionPane.YES_OPTION) {
                        DefaultTableModel tableModel = (DefaultTableModel) httpRequestForm.getNoteListTable().getModel();

                        for (int i = selectedRows.length; i > 0; i--) {
                            int selectedRow = httpRequestForm.getNoteListTable().getSelectedRow();
                            Integer id = (Integer) tableModel.getValueAt(selectedRow, 0);
                            quickNoteMapper.deleteByPrimaryKey(id);

                            tableModel.removeRow(selectedRow);
                        }
                        selectedName = null;
                        HttpRequestForm.initNoteListTable();
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(e1.toString());
            }
        }));

        // 字体名称下拉框事件
        httpRequestForm.getFontNameComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String fontName = e.getItem().toString();
                int fontSize = Integer.parseInt(httpRequestForm.getFontSizeComboBox().getSelectedItem().toString());
                Font font = new Font(fontName, Font.PLAIN, fontSize);
                httpRequestForm.getTextArea().setFont(font);

                App.config.setQuickNoteFontName(fontName);
                App.config.setQuickNoteFontSize(fontSize);
                App.config.save();
            }
        });

        // 字体大小下拉框事件
        httpRequestForm.getFontSizeComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int fontSize = Integer.parseInt(e.getItem().toString());
                String fontName = httpRequestForm.getFontNameComboBox().getSelectedItem().toString();
                Font font = new Font(fontName, Font.PLAIN, fontSize);
                httpRequestForm.getTextArea().setFont(font);

                App.config.setQuickNoteFontName(fontName);
                App.config.setQuickNoteFontSize(fontSize);
                App.config.save();
            }
        });

        // 自动换行按钮事件
        httpRequestForm.getWrapButton().addActionListener(e -> {
            httpRequestForm.getTextArea().setLineWrap(!httpRequestForm.getTextArea().getLineWrap());
        });

        // 添加按钮事件
        httpRequestForm.getAddButton().addActionListener(e -> {
            httpRequestForm.getTextArea().setText("");
            selectedName = null;
        });

        // 左侧列表鼠标点击事件（显示下方删除按钮）
        httpRequestForm.getNoteListTable().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                httpRequestForm.getDeletePanel().setVisible(true);
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
        httpRequestForm.getTextArea().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                httpRequestForm.getDeletePanel().setVisible(false);
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
        httpRequestForm.getNoteListTable().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    int selectedRow = httpRequestForm.getNoteListTable().getSelectedRow();
                    int noteId = Integer.parseInt(String.valueOf(httpRequestForm.getNoteListTable().getValueAt(selectedRow, 0)));
                    String noteName = String.valueOf(httpRequestForm.getNoteListTable().getValueAt(selectedRow, 1));
                    TQuickNote tQuickNote = new TQuickNote();
                    tQuickNote.setId(noteId);
                    tQuickNote.setName(noteName);
                    try {
                        quickNoteMapper.updateByPrimaryKeySelective(tQuickNote);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，可能和已有笔记重名");
                        HttpRequestForm.initNoteListTable();
                        log.error(e.toString());
                    }
                }
            }
        });

    }
}
