package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.thread.ThreadUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TMsgHttpMapper;
import com.luoboduner.moo.tool.domain.TMsgHttp;
import com.luoboduner.moo.tool.ui.form.func.HttpRequestForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
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

    private static TMsgHttpMapper msgHttpMapper = MybatisUtil.getSqlSession().getMapper(TMsgHttpMapper.class);

    public static String selectedName;

    public static void addListeners() {
        HttpRequestForm httpRequestForm = HttpRequestForm.getInstance();

        httpRequestForm.getSaveButton().addActionListener(e -> {
            if (StringUtils.isBlank(selectedName)) {
                selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            }
            String name = JOptionPane.showInputDialog("名称", selectedName);
            if (StringUtils.isNotBlank(name)) {
                HttpRequestForm.save(name);
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
                    HttpRequestForm.initMsg(name);
                });
                super.mousePressed(e);
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
                            msgHttpMapper.deleteByPrimaryKey(id);

                            tableModel.removeRow(selectedRow);
                        }
                        selectedName = null;
                        HttpRequestForm.initListTable();
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error(e1.toString());
            }
        }));

        // 添加按钮事件
        httpRequestForm.getAddButton().addActionListener(e -> {
            HttpRequestForm.clearAllField();
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
                    TMsgHttp tMsgHttp = new TMsgHttp();
                    tMsgHttp.setId(noteId);
                    tMsgHttp.setMsgName(noteName);
                    try {
                        msgHttpMapper.updateByPrimaryKeySelective(tMsgHttp);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(App.mainFrame, "重命名失败，可能和已有笔记重名");
                        HttpRequestForm.initListTable();
                        log.error(e.toString());
                    }
                }
            }
        });

    }
}
