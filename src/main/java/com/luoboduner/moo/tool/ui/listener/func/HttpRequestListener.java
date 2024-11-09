package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TMsgHttpMapper;
import com.luoboduner.moo.tool.domain.TMsgHttp;
import com.luoboduner.moo.tool.service.HttpMsgMaker;
import com.luoboduner.moo.tool.service.HttpMsgSender;
import com.luoboduner.moo.tool.service.HttpSendResult;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.HttpRequestForm;
import com.luoboduner.moo.tool.ui.form.func.HttpResultForm;
import com.luoboduner.moo.tool.ui.frame.HttpResultFrame;
import com.luoboduner.moo.tool.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

    private static final Log logger = LogFactory.get();

    private static TMsgHttpMapper msgHttpMapper = MybatisUtil.getSqlSession().getMapper(TMsgHttpMapper.class);

    public static String selectedName;

    public static void addListeners() {
        HttpRequestForm httpRequestForm = HttpRequestForm.getInstance();

        httpRequestForm.getSaveButton().addActionListener(e -> {
            if (StringUtils.isBlank(selectedName)) {
                selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            }
            String name = JOptionPane.showInputDialog(MainWindow.getInstance().getMainPanel(), "名称", selectedName);
            if (StringUtils.isNotBlank(name)) {
                HttpRequestForm.save(name);
            }
        });

        // 点击左侧表格事件
        httpRequestForm.getNoteListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int focusedRowIndex = httpRequestForm.getNoteListTable().rowAtPoint(e.getPoint());
                if (focusedRowIndex == -1) {
                    return;
                }
                String name = httpRequestForm.getNoteListTable().getValueAt(focusedRowIndex, 1).toString();
                selectedName = name;
                HttpRequestForm.initMsg(name);
                super.mousePressed(e);
            }
        });

        // 删除按钮事件
        httpRequestForm.getDeleteButton().addActionListener(e -> ThreadUtil.execute(() -> {
            deleteFiles(httpRequestForm);
        }));

        // 添加按钮事件
        httpRequestForm.getAddButton().addActionListener(e -> {
            HttpRequestForm.clearAllField();
            selectedName = null;
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
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(httpRequestForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                    int selectedRow = httpRequestForm.getNoteListTable().getSelectedRow();
                    String name = httpRequestForm.getNoteListTable().getValueAt(selectedRow, 1).toString();
                    selectedName = name;
                    HttpRequestForm.initMsg(name);
                }
            }
        });

        httpRequestForm.getParamAddButton().addActionListener(e -> {
            String[] data = new String[2];
            data[0] = httpRequestForm.getParamNameTextField().getText();
            data[1] = httpRequestForm.getParamValueTextField().getText();

            if (httpRequestForm.getParamTable().getModel().getRowCount() == 0) {
                HttpRequestForm.initParamTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) httpRequestForm.getParamTable().getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(App.mainFrame, "Name和Value不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(App.mainFrame, "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });

        httpRequestForm.getHeaderAddButton().addActionListener(e -> {
            String[] data = new String[2];
            data[0] = httpRequestForm.getHeaderNameTextField().getText();
            data[1] = httpRequestForm.getHeaderValueTextField5().getText();

            if (httpRequestForm.getHeaderTable().getModel().getRowCount() == 0) {
                HttpRequestForm.initHeaderTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) httpRequestForm.getHeaderTable().getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(App.mainFrame, "Name和Value不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(App.mainFrame, "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });

        httpRequestForm.getCookieAddButton().addActionListener(e -> {
            String[] data = new String[5];
            data[0] = httpRequestForm.getCookieNameTextField().getText();
            data[1] = httpRequestForm.getCookieValueTextField().getText();
            data[2] = httpRequestForm.getCookieDomainTextField().getText();
            data[3] = httpRequestForm.getCookiePathTextField().getText();
            data[4] = httpRequestForm.getCookieExpiryTextField().getText();

            if (httpRequestForm.getCookieTable().getModel().getRowCount() == 0) {
                HttpRequestForm.initCookieTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) httpRequestForm.getCookieTable().getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1]) || StringUtils.isEmpty(data[4])) {
                JOptionPane.showMessageDialog(App.mainFrame, "Name、Value、Expiry不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(App.mainFrame, "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });

        // 消息类型切换事件
        httpRequestForm.getMethodComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                HttpRequestForm.switchMethod(e.getItem().toString());
            }
        });

        httpRequestForm.getSendToWindowButton().addActionListener(e -> {
            try {
                HttpMsgMaker.prepare();
                HttpMsgSender httpMsgSender = new HttpMsgSender();
                HttpSendResult httpSendResult = httpMsgSender.send();

                // clear all
                HttpResultForm.getInstance().getResponseBodyTextArea().setText("");
                HttpResultForm.getInstance().getHeadersTextArea().setText("");
                HttpResultForm.getInstance().getCookiesTextArea().setText("");

                if (httpSendResult.isSuccess()) {
                    HttpResultForm.getInstance().getResponseBodyTextArea().setText(httpSendResult.getBody());
                    HttpResultForm.getInstance().getResponseBodyTextArea().setCaretPosition(0);
                    HttpResultForm.getInstance().getHeadersTextArea().setText(httpSendResult.getHeaders());
                    HttpResultForm.getInstance().getHeadersTextArea().setCaretPosition(0);
                    HttpResultForm.getInstance().getCookiesTextArea().setText(httpSendResult.getCookies());
                    HttpResultForm.getInstance().getCookiesTextArea().setCaretPosition(0);
                    HttpResultFrame.showResultWindow();

                    if (StringUtils.isBlank(selectedName)) {
                        selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
                    }
                    HttpRequestForm.save(selectedName);
                } else {
                    JOptionPane.showMessageDialog(App.mainFrame, "发送请求失败！\n\n" + httpSendResult.getInfo(), "失败",
                            JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "发送请求失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        httpRequestForm.getSendButton().addActionListener(e -> {
            try {
                HttpMsgMaker.prepare();
                HttpMsgSender httpMsgSender = new HttpMsgSender();
                HttpSendResult httpSendResult = httpMsgSender.send();

                // clear all
                httpRequestForm.getResponseBodyTextArea().setText("");
                httpRequestForm.getHeadersTextArea().setText("");
                httpRequestForm.getCookiesTextArea().setText("");

                if (httpSendResult.isSuccess()) {
                    httpRequestForm.getResponseBodyTextArea().setText(httpSendResult.getBody());
                    httpRequestForm.getResponseBodyTextArea().setCaretPosition(0);
                    httpRequestForm.getHeadersTextArea().setText(httpSendResult.getHeaders());
                    httpRequestForm.getHeadersTextArea().setCaretPosition(0);
                    httpRequestForm.getCookiesTextArea().setText(httpSendResult.getCookies());
                    httpRequestForm.getCookiesTextArea().setCaretPosition(0);

                    if (StringUtils.isBlank(selectedName)) {
                        selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
                    }
                    HttpRequestForm.save(selectedName);
                } else {
                    JOptionPane.showMessageDialog(App.mainFrame, "发送请求失败！\n\n" + httpSendResult.getInfo(), "失败",
                            JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "发送请求失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // 搜索框变更事件
        httpRequestForm.getSearchTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                HttpRequestForm.initListTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                HttpRequestForm.initListTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
//                HttpRequestForm.initListTable();
            }
        });

        // 历史记录按钮事件
        httpRequestForm.getHistoryButton().addActionListener(e -> {
            // toggle history panel visibility
            int totalWidth = httpRequestForm.getHistorySplitPane().getWidth();
            int currentDividerLocation = httpRequestForm.getHistorySplitPane().getDividerLocation();

            if (totalWidth - currentDividerLocation < 10) {
                httpRequestForm.getHistoryPanel().setVisible(true);
                httpRequestForm.getHistorySplitPane().setDividerLocation((int) (totalWidth * 0.6));
            } else {
                httpRequestForm.getHistorySplitPane().setDividerLocation(totalWidth);
                httpRequestForm.getHistoryPanel().setVisible(false);
            }
        });

        // 历史记录关闭按钮事件
        httpRequestForm.getCloseHistoryLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                httpRequestForm.getHistorySplitPane().setDividerLocation(httpRequestForm.getHistorySplitPane().getWidth());
                httpRequestForm.getHistoryPanel().setVisible(false);
                super.mouseClicked(e);
            }
        });

    }

    private static void deleteFiles(HttpRequestForm httpRequestForm) {
        try {
            int[] selectedRows = httpRequestForm.getNoteListTable().getSelectedRows();

            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(App.mainFrame, "请至少选择一个！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int isDelete = JOptionPane.showConfirmDialog(App.mainFrame, "确认删除？", "确认", JOptionPane.YES_NO_OPTION);
                if (isDelete == JOptionPane.YES_OPTION) {
                    DefaultTableModel tableModel = (DefaultTableModel) httpRequestForm.getNoteListTable().getModel();

                    for (int i = 0; i < selectedRows.length; i++) {
                        int selectedRow = selectedRows[i];
                        Integer id = (Integer) tableModel.getValueAt(selectedRow, 0);
                        msgHttpMapper.deleteByPrimaryKey(id);
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
    }
}
