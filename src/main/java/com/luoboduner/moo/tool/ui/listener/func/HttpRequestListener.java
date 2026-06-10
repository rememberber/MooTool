package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.json.JSONUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.THttpRequestHistoryMapper;
import com.luoboduner.moo.tool.dao.TMsgHttpMapper;
import com.luoboduner.moo.tool.domain.TMsgHttp;
import com.luoboduner.moo.tool.service.HttpMsgMaker;
import com.luoboduner.moo.tool.service.HttpMsgSender;
import com.luoboduner.moo.tool.service.HttpSendResult;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.HttpRequestForm;
import com.luoboduner.moo.tool.ui.form.func.HttpResultForm;
import com.luoboduner.moo.tool.ui.frame.HttpResultFrame;
import com.luoboduner.moo.tool.ui.dialog.JsonResultDialog;
import com.luoboduner.moo.tool.util.CurlParserUtil;
import com.luoboduner.moo.tool.util.AutoIndentDocumentFilter;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.MsgUtil;
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
    private static THttpRequestHistoryMapper httpRequestHistoryMapper = MybatisUtil.getSqlSession().getMapper(THttpRequestHistoryMapper.class);

    public static String selectedName;

    /** 忽略 JOptionPane 关闭后回传到列表的 Enter 键，避免重命名弹框重复弹出 */
    private static boolean suppressListEnterRename;

    public static void addListeners() {
        HttpRequestForm httpRequestForm = HttpRequestForm.getInstance();

        httpRequestForm.getSaveButton().addActionListener(e -> {
            if (StringUtils.isBlank(selectedName)) {
                selectedName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            }
            String name = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), selectedName);
            if (StringUtils.isNotBlank(name)) {
                HttpRequestForm.save(name);
            }
        });

        // 点击左侧列表事件
        httpRequestForm.getNoteList().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = httpRequestForm.getNoteList().locationToIndex(e.getPoint());
                if (index == -1) {
                    return;
                }
                DefaultListModel<TMsgHttp> listModel = (DefaultListModel<TMsgHttp>) httpRequestForm.getNoteList().getModel();
                String name = listModel.getElementAt(index).getMsgName();
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

        // 导入 cURL 按钮事件（URL 文本框尾随按钮）
        httpRequestForm.getImportCurlButton().addActionListener(e -> {
            try {
                JsonResultDialog dialog = new JsonResultDialog(null, "请输入 cURL 命令：", "Input");
                dialog.setVisible(true);
                String curl = JsonResultDialog.textInputValue;
                if (StringUtils.isBlank(curl)) {
                    return;
                }
                CurlParserUtil.CurlResult result = CurlParserUtil.parse(curl);

                int headerCount = result.getHeaders() == null ? 0 : result.getHeaders().size();
                int cookieCount = result.getCookies() == null ? 0 : result.getCookies().size();
                int bodyLen = result.getBody() == null ? 0 : result.getBody().length();
                String methodShow = result.getMethod() == null ? "" : result.getMethod();
                String urlShow = result.getUrl() == null ? "" : result.getUrl();

                String msg = I18n.format("msg.confirmImportBody", methodShow, urlShow, headerCount, cookieCount, bodyLen);
                int confirm = JOptionPane.showConfirmDialog(App.mainFrame, msg, I18n.get("msg.confirmImport"), JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }

                HttpRequestForm.clearAllField();
                HttpRequestForm.applyImportedRequest(result);
                HttpRequestForm.splitQueryToParamTable(result.getUrl());
            } catch (Exception ex) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.importCurlFailed", ex.getMessage());
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // Body 自动缩进（回车保持缩进/格式化）
        try {
            javax.swing.text.AbstractDocument doc = (javax.swing.text.AbstractDocument) httpRequestForm.getBodyTextArea().getDocument();
            doc.setDocumentFilter(new AutoIndentDocumentFilter(() -> (String) httpRequestForm.getBodyTypeComboBox().getSelectedItem()));
        } catch (Exception ignore) {
        }

        // 左侧列表按键事件（重命名）
        httpRequestForm.getNoteList().addKeyListener(new KeyListener() {
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
                    renameSelectedMsg(httpRequestForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(httpRequestForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                    int selectedIndex = httpRequestForm.getNoteList().getSelectedIndex();
                    if (selectedIndex < 0) {
                        return;
                    }
                    DefaultListModel<TMsgHttp> listModel = (DefaultListModel<TMsgHttp>) httpRequestForm.getNoteList().getModel();
                    String name = listModel.getElementAt(selectedIndex).getMsgName();
                    selectedName = name;
                    HttpRequestForm.initMsg(name);
                    HttpRequestForm.initHistoryTable();
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
                MsgUtil.info(App.mainFrame, "msg.nameValueRequired");
            } else if (keySet.contains(data[0])) {
                MsgUtil.info(App.mainFrame, "msg.nameDuplicate");
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
                MsgUtil.info(App.mainFrame, "msg.nameValueRequired");
            } else if (keySet.contains(data[0])) {
                MsgUtil.info(App.mainFrame, "msg.nameDuplicate");
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
                MsgUtil.info(App.mainFrame, "msg.nameValueExpiryRequired");
            } else if (keySet.contains(data[0])) {
                MsgUtil.info(App.mainFrame, "msg.nameDuplicate");
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
                    MsgUtil.errorWithDetail(App.mainFrame, "msg.sendFailed", httpSendResult.getInfo());
                }

            } catch (Exception ex) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.sendFailed", ex.getMessage());
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
                    MsgUtil.errorWithDetail(App.mainFrame, "msg.sendFailed", httpSendResult.getInfo());
                }

            } catch (Exception ex) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.sendFailed", ex.getMessage());
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // Body 格式化按钮事件
        if (httpRequestForm.getBodyFormatButton() != null) {
            httpRequestForm.getBodyFormatButton().addActionListener(e -> {
                try {
                    String bodyType = (String) httpRequestForm.getBodyTypeComboBox().getSelectedItem();
                    String text = httpRequestForm.getBodyTextArea().getText();
                    if (StringUtils.isBlank(text)) {
                        MsgUtil.info(App.mainFrame, "msg.bodyEmpty");
                        return;
                    }
                    String formatted;
                    if ("application/json".equalsIgnoreCase(bodyType)) {
                        try {
                            formatted = JSONUtil.toJsonPrettyStr(text);
                        } catch (Exception ex) {
                            MsgUtil.errorWithDetail(App.mainFrame, "msg.jsonFormatFailed", ex.getMessage());
                            return;
                        }
                    } else if ("application/xml".equalsIgnoreCase(bodyType) || "text/xml".equalsIgnoreCase(bodyType)) {
                        try {
                            // pre-validate XML
                            javax.xml.parsers.DocumentBuilderFactory.newInstance()
                                    .newDocumentBuilder()
                                    .parse(new org.xml.sax.InputSource(new java.io.StringReader(text)));
                        } catch (Exception ex) {
                            MsgUtil.errorWithDetail(App.mainFrame, "msg.xmlFormatFailed", ex.getMessage());
                            return;
                        }
                        formatted = com.luoboduner.moo.tool.util.XmlReformatUtil.format(text);
                    } else {
                        MsgUtil.info(App.mainFrame, "msg.bodyTypeNoFormat");
                        return;
                    }
                    httpRequestForm.getBodyTextArea().setText(formatted);
                    httpRequestForm.getBodyTextArea().setCaretPosition(0);
                } catch (Exception ex) {
                    MsgUtil.errorWithDetail(App.mainFrame, "msg.formatFailedShort", ex.getMessage());
                    logger.error(ExceptionUtils.getStackTrace(ex));
                }
            });
        }

        // 搜索框变更事件
        httpRequestForm.getSearchTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                HttpRequestForm.initList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                HttpRequestForm.initList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
//                HttpRequestForm.initList();
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
        httpRequestForm.getCloseHistoryButton().addActionListener(e -> {
            httpRequestForm.getHistorySplitPane().setDividerLocation(httpRequestForm.getHistorySplitPane().getWidth());
            httpRequestForm.getHistoryPanel().setVisible(false);
        });

        // 点击历史记录表格事件
        httpRequestForm.getHistoryTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int focusedRowIndex = httpRequestForm.getHistoryTable().rowAtPoint(e.getPoint());
                if (focusedRowIndex == -1) {
                    return;
                }
                Integer historyId = (Integer) httpRequestForm.getHistoryTable().getValueAt(focusedRowIndex, 0);
                HttpRequestForm.initMsg(historyId);
                super.mousePressed(e);
            }
        });

        httpRequestForm.getDeleteHistoryButton().addActionListener(e -> {
            try {
                int[] selectedRows = httpRequestForm.getHistoryTable().getSelectedRows();

                if (selectedRows.length == 0) {
                    MsgUtil.info(App.mainFrame, "msg.selectAtLeastOne");
                } else {
                    int isDelete = MsgUtil.confirm(App.mainFrame, "msg.confirmDelete");
                    if (isDelete == JOptionPane.YES_OPTION) {
                        DefaultTableModel tableModel = (DefaultTableModel) httpRequestForm.getHistoryTable().getModel();

                        for (int i = 0; i < selectedRows.length; i++) {
                            int selectedRow = selectedRows[i];
                            Integer id = (Integer) tableModel.getValueAt(selectedRow, 0);
                            httpRequestHistoryMapper.deleteByPrimaryKey(id);
                        }

                        TMsgHttp tMsgHttp = msgHttpMapper.selectByMsgName(selectedName);
                        if (tMsgHttp != null) {
                            HttpRequestForm.initHistoryListTable(tMsgHttp.getId());
                        }
                    }
                }
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.deleteFailed", e1.getMessage());
                log.error(e1.toString());
            }
        });

        // 左侧列表增加右键菜单
        JPopupMenu noteListPopupMenu = new JPopupMenu();
        JMenuItem renameMenuItem = new JMenuItem("重命名");
        JMenuItem deleteMenuItem = new JMenuItem("删除");
        noteListPopupMenu.add(renameMenuItem);
        noteListPopupMenu.add(deleteMenuItem);
        httpRequestForm.getNoteList().setComponentPopupMenu(noteListPopupMenu);

        renameMenuItem.addActionListener(e -> renameSelectedMsg(httpRequestForm));

        deleteMenuItem.addActionListener(e -> {
            deleteFiles(httpRequestForm);
        });
    }

    private static void renameSelectedMsg(HttpRequestForm httpRequestForm) {
        int selectedIndex = httpRequestForm.getNoteList().getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }
        DefaultListModel<TMsgHttp> model = (DefaultListModel<TMsgHttp>) httpRequestForm.getNoteList().getModel();
        TMsgHttp item = model.getElementAt(selectedIndex);
        String beforeName = item.getMsgName();
        if (StringUtils.isBlank(beforeName)) {
            return;
        }
        suppressListEnterRename = true;
        String afterName = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), beforeName);
        if (StringUtils.isBlank(afterName) || afterName.equals(beforeName)) {
            return;
        }
        try {
            TMsgHttp tMsgHttp = new TMsgHttp();
            tMsgHttp.setId(item.getId());
            tMsgHttp.setMsgName(afterName);
            tMsgHttp.setModifiedTime(SqliteUtil.nowDateForSqlite());
            msgHttpMapper.updateByPrimaryKeySelective(tMsgHttp);
            selectedName = afterName;
            item.setMsgName(afterName);
            model.set(selectedIndex, item);
        } catch (Exception e) {
            MsgUtil.info(App.mainFrame, "msg.renameNoteFailed");
            HttpRequestForm.initList();
            log.error(e.toString());
        }
    }

    private static void deleteFiles(HttpRequestForm httpRequestForm) {
        try {
            int[] selectedIndices = httpRequestForm.getNoteList().getSelectedIndices();

            if (selectedIndices.length == 0) {
                MsgUtil.info(App.mainFrame, "msg.selectAtLeastOne");
            } else {
                int isDelete = MsgUtil.confirm(App.mainFrame, "msg.confirmDelete");
                if (isDelete == JOptionPane.YES_OPTION) {
                    DefaultListModel<TMsgHttp> listModel = (DefaultListModel<TMsgHttp>) httpRequestForm.getNoteList().getModel();

                    for (int selectedIndex : selectedIndices) {
                        Integer id = listModel.getElementAt(selectedIndex).getId();
                        msgHttpMapper.deleteByPrimaryKey(id);
                    }
                    selectedName = null;
                    HttpRequestForm.initList();
                }
            }
        } catch (Exception e1) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.deleteFailed", e1.getMessage());
            log.error(e1.toString());
        }
    }
}
