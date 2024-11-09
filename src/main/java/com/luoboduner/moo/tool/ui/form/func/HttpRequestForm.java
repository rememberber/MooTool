package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TMsgHttpMapper;
import com.luoboduner.moo.tool.domain.TMsgHttp;
import com.luoboduner.moo.tool.ui.Style;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.component.TableInCellButtonColumn;
import com.luoboduner.moo.tool.ui.listener.func.HttpRequestListener;
import com.luoboduner.moo.tool.util.*;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * Http请求
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/9/7.
 */
@Getter
public class HttpRequestForm {
    private JPanel httpRequestPanel;
    private JTable noteListTable;
    private JButton deleteButton;
    private JButton saveButton;
    private JSplitPane splitPane;
    private JButton addButton;
    private JComboBox methodComboBox;
    private JTextField urlTextField;
    private JTabbedPane tabbedPane1;
    private JTextField paramNameTextField;
    private JTextField paramValueTextField;
    private JButton paramAddButton;
    private JTable paramTable;
    private JTextField headerNameTextField;
    private JTextField headerValueTextField5;
    private JButton headerAddButton;
    private JTable headerTable;
    private JTextField cookieNameTextField;
    private JTextField cookieValueTextField;
    private JButton cookieAddButton;
    private JTable cookieTable;
    private JTextField cookieDomainTextField;
    private JTextField cookiePathTextField;
    private JTextField cookieExpiryTextField;
    private JTextArea bodyTextArea;
    private JComboBox bodyTypeComboBox;
    private JButton sendToWindowButton;
    private JPanel rightPanel;
    private JPanel controlPanel;
    private JPanel contentPanel;
    private JTextField searchTextField;
    private JTabbedPane tabbedPane2;
    private JScrollPane httpResultScrollPane;
    private JTextArea responseBodyTextArea;
    private JTextArea headersTextArea;
    private JTextArea cookiesTextArea;
    private JButton sendButton;
    private JButton historyButton;
    private JTable historyTable;
    private JButton deleteHistoryButton;
    private JLabel closeHistoryLabel;
    private JSplitPane historySplitPane;
    private JScrollPane historyTableScrollPane;
    private JPanel historyPanel;

    private static final Log logger = LogFactory.get();
    private static HttpRequestForm httpRequestForm;
    private static TMsgHttpMapper msgHttpMapper = MybatisUtil.getSqlSession().getMapper(TMsgHttpMapper.class);

    private HttpRequestForm() {
        UndoUtil.register(this);

    }

    public static HttpRequestForm getInstance() {
        if (httpRequestForm == null) {
            httpRequestForm = new HttpRequestForm();
        }
        return httpRequestForm;
    }

    public static void init() {
        httpRequestForm = getInstance();

        initUi();
        initListTable();

        HttpRequestListener.addListeners();
    }

    private static void initUi() {
        httpRequestForm.getSearchTextField().putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "搜索");
        httpRequestForm.getSearchTextField().putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSearchIcon());

        httpRequestForm.getAddButton().setIcon(new FlatSVGIcon("icon/add.svg"));
        httpRequestForm.getSaveButton().setIcon(new FlatSVGIcon("icon/save.svg"));
        httpRequestForm.getSendToWindowButton().setIcon(new FlatSVGIcon("icon/send.svg"));
        httpRequestForm.getParamAddButton().setIcon(new FlatSVGIcon("icon/add.svg"));
        httpRequestForm.getDeleteButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        httpRequestForm.getSendButton().setIcon(new FlatSVGIcon("icon/run.svg"));
        httpRequestForm.getHistoryButton().setIcon(new FlatSVGIcon("icon/history.svg"));
        httpRequestForm.getDeleteHistoryButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        httpRequestForm.getCloseHistoryLabel().setIcon(new FlatSVGIcon("icon/remove2.svg"));

        httpRequestForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
        httpRequestForm.getNoteListTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);

        Style.blackTextArea(httpRequestForm.getBodyTextArea());

        ScrollUtil.smoothPane(httpRequestForm.getHistoryTableScrollPane());

        httpRequestForm.getHistoryPanel().setVisible(false);

        httpRequestForm.getHttpRequestPanel().updateUI();
    }

    public static void initMsg(String msgName) {
        clearAllField();
        TMsgHttp tMsgHttp = msgHttpMapper.selectByMsgName(msgName);
        getInstance().getMethodComboBox().setSelectedItem(tMsgHttp.getMethod());
        getInstance().getUrlTextField().setText(tMsgHttp.getUrl());
        getInstance().getBodyTextArea().setText(tMsgHttp.getBody());
        getInstance().getBodyTypeComboBox().setSelectedItem(tMsgHttp.getBodyType());
        switchMethod(tMsgHttp.getMethod());

        getInstance().getResponseBodyTextArea().setText(tMsgHttp.getResponseBody());
        getInstance().getHeadersTextArea().setText(tMsgHttp.getResponseHeaders());
        getInstance().getCookiesTextArea().setText(tMsgHttp.getResponseCookies());

        // Params=====================================
        initParamTable();
        List<NameValueObject> params = JSONUtil.toList(JSONUtil.parseArray(tMsgHttp.getParams()), NameValueObject.class);
        String[] headerNames = {"Name", "Value", ""};
        Object[][] cellData = new String[params.size()][headerNames.length];
        for (int i = 0; i < params.size(); i++) {
            NameValueObject nameValueObject = params.get(i);
            cellData[i][0] = nameValueObject.getName();
            cellData[i][1] = nameValueObject.getValue();
        }
        DefaultTableModel model = new DefaultTableModel(cellData, headerNames);
        getInstance().getParamTable().setModel(model);
        TableColumnModel paramTableColumnModel = getInstance().getParamTable().getColumnModel();
        paramTableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(getInstance().getParamTable(), headerNames.length - 1));
        paramTableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(getInstance().getParamTable(), headerNames.length - 1));

        // 设置列宽
        paramTableColumnModel.getColumn(2).setPreferredWidth(46);
        paramTableColumnModel.getColumn(2).setMaxWidth(46);
        // Headers=====================================
        initHeaderTable();
        List<NameValueObject> headers = JSONUtil.toList(JSONUtil.parseArray(tMsgHttp.getHeaders()), NameValueObject.class);
        cellData = new String[headers.size()][headerNames.length];
        for (int i = 0; i < headers.size(); i++) {
            NameValueObject nameValueObject = headers.get(i);
            cellData[i][0] = nameValueObject.getName();
            cellData[i][1] = nameValueObject.getValue();
        }
        model = new DefaultTableModel(cellData, headerNames);
        getInstance().getHeaderTable().setModel(model);
        TableColumnModel headerTableColumnModel = getInstance().getHeaderTable().getColumnModel();
        headerTableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(getInstance().getHeaderTable(), headerNames.length - 1));
        headerTableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(getInstance().getHeaderTable(), headerNames.length - 1));

        // 设置列宽
        headerTableColumnModel.getColumn(2).setPreferredWidth(46);
        headerTableColumnModel.getColumn(2).setMaxWidth(46);
        // Cookies=====================================
        initCookieTable();
        List<CookieObject> cookies = JSONUtil.toList(JSONUtil.parseArray(tMsgHttp.getCookies()), CookieObject.class);
        headerNames = new String[]{"Name", "Value", "Domain", "Path", "Expiry", ""};
        cellData = new String[cookies.size()][headerNames.length];
        for (int i = 0; i < cookies.size(); i++) {
            CookieObject cookieObject = cookies.get(i);
            cellData[i][0] = cookieObject.getName();
            cellData[i][1] = cookieObject.getValue();
            cellData[i][2] = cookieObject.getDomain();
            cellData[i][3] = cookieObject.getPath();
            cellData[i][4] = cookieObject.getExpiry();
        }
        model = new DefaultTableModel(cellData, headerNames);
        getInstance().getCookieTable().setModel(model);
        TableColumnModel cookieTableColumnModel = getInstance().getCookieTable().getColumnModel();
        cookieTableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(getInstance().getCookieTable(), headerNames.length - 1));
        cookieTableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(getInstance().getCookieTable(), headerNames.length - 1));

        // 设置列宽
        cookieTableColumnModel.getColumn(5).setPreferredWidth(46);
        cookieTableColumnModel.getColumn(5).setMaxWidth(46);
    }

    public static void initListTable() {
        String[] headerNames = {"id", "名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        httpRequestForm.getNoteListTable().setModel(model);
        // 隐藏表头
        JTableUtil.hideTableHeader(httpRequestForm.getNoteListTable());
        // 隐藏id列
        JTableUtil.hideColumn(httpRequestForm.getNoteListTable(), 0);

        Object[] data;

        String titleFilterKeyWord = httpRequestForm.getSearchTextField().getText();
        titleFilterKeyWord = "%" + titleFilterKeyWord + "%";

        List<TMsgHttp> msgHttpList = msgHttpMapper.selectByFilter(titleFilterKeyWord);
        for (TMsgHttp tMsgHttp : msgHttpList) {
            data = new Object[2];
            data[0] = tMsgHttp.getId();
            data[1] = tMsgHttp.getMsgName();
            model.addRow(data);
        }
        if (msgHttpList.size() > 0) {
            initMsg(msgHttpList.get(0).getMsgName());
            httpRequestForm.getNoteListTable().setRowSelectionInterval(0, 0);
            HttpRequestListener.selectedName = msgHttpList.get(0).getMsgName();
        }
    }

    public static void save(String msgName) {
        boolean existSameMsg = false;

        TMsgHttp tMsgHttp1 = msgHttpMapper.selectByMsgName(msgName);
        if (tMsgHttp1 != null) {
            existSameMsg = true;
        }

        String method = (String) getInstance().getMethodComboBox().getSelectedItem();
        String url = getInstance().getUrlTextField().getText();
        String body = getInstance().getBodyTextArea().getText();
        String bodyType = (String) getInstance().getBodyTypeComboBox().getSelectedItem();
        String responseBody = getInstance().getResponseBodyTextArea().getText();
        String responseHeaders = getInstance().getHeadersTextArea().getText();
        String responseCookies = getInstance().getCookiesTextArea().getText();
        String now = SqliteUtil.nowDateForSqlite();

        TMsgHttp tMsgHttp = new TMsgHttp();
        tMsgHttp.setMsgName(msgName);
        tMsgHttp.setMethod(method);
        tMsgHttp.setUrl(url);
        tMsgHttp.setBody(body);
        tMsgHttp.setBodyType(bodyType);
        tMsgHttp.setResponseBody(responseBody);
        tMsgHttp.setResponseHeaders(responseHeaders);
        tMsgHttp.setResponseCookies(responseCookies);
        tMsgHttp.setCreateTime(now);
        tMsgHttp.setModifiedTime(now);

        // =============params
        // 如果table为空，则初始化
        if (getInstance().getParamTable().getModel().getRowCount() == 0) {
            initParamTable();
        }
        // 逐行读取
        DefaultTableModel paraTableModel = (DefaultTableModel) getInstance().getParamTable().getModel();
        int rowCount = paraTableModel.getRowCount();
        List<NameValueObject> params = new ArrayList<>();
        NameValueObject nameValueObject;
        for (int i = 0; i < rowCount; i++) {
            String name = (String) paraTableModel.getValueAt(i, 0);
            String value = (String) paraTableModel.getValueAt(i, 1);
            nameValueObject = new NameValueObject();
            nameValueObject.setName(name);
            nameValueObject.setValue(value);
            params.add(nameValueObject);
        }
        tMsgHttp.setParams(JSONUtil.toJsonStr(params));
        // =============headers
        // 如果table为空，则初始化
        if (getInstance().getHeaderTable().getModel().getRowCount() == 0) {
            initHeaderTable();
        }
        // 逐行读取
        DefaultTableModel headerTableModel = (DefaultTableModel) getInstance().getHeaderTable().getModel();
        rowCount = headerTableModel.getRowCount();
        List<NameValueObject> headers = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            String name = (String) headerTableModel.getValueAt(i, 0);
            String value = (String) headerTableModel.getValueAt(i, 1);
            nameValueObject = new NameValueObject();
            nameValueObject.setName(name);
            nameValueObject.setValue(value);
            headers.add(nameValueObject);
        }
        tMsgHttp.setHeaders(JSONUtil.toJsonStr(headers));
        // =============cookies
        // 如果table为空，则初始化
        if (getInstance().getCookieTable().getModel().getRowCount() == 0) {
            initCookieTable();
        }
        // 逐行读取
        DefaultTableModel cookiesTableModel = (DefaultTableModel) getInstance().getCookieTable().getModel();
        rowCount = cookiesTableModel.getRowCount();
        List<CookieObject> cookies = new ArrayList<>();
        CookieObject cookieObject;
        for (int i = 0; i < rowCount; i++) {
            String name = (String) cookiesTableModel.getValueAt(i, 0);
            String value = (String) cookiesTableModel.getValueAt(i, 1);
            String domain = (String) cookiesTableModel.getValueAt(i, 2);
            String path = (String) cookiesTableModel.getValueAt(i, 3);
            String expiry = (String) cookiesTableModel.getValueAt(i, 4);
            cookieObject = new CookieObject();
            cookieObject.setName(name);
            cookieObject.setValue(value);
            cookieObject.setDomain(domain);
            cookieObject.setPath(path);
            cookieObject.setExpiry(expiry);
            cookies.add(cookieObject);
        }
        tMsgHttp.setCookies(JSONUtil.toJsonStr(cookies));

        if (existSameMsg) {
            msgHttpMapper.updateByMsgName(tMsgHttp);
        } else {
            msgHttpMapper.insertSelective(tMsgHttp);
            initListTable();
        }
        JOptionPane.showMessageDialog(App.mainFrame, "保存成功！", "成功",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        getInstance().getMethodComboBox().setSelectedIndex(0);
        getInstance().getUrlTextField().setText("");
        getInstance().getParamNameTextField().setText("");
        getInstance().getParamValueTextField().setText("");
        getInstance().getHeaderNameTextField().setText("");
        getInstance().getHeaderValueTextField5().setText("");
        getInstance().getCookieNameTextField().setText("");
        getInstance().getCookieValueTextField().setText("");
        getInstance().getCookieDomainTextField().setText("");
        getInstance().getCookiePathTextField().setText("");
        getInstance().getCookieExpiryTextField().setText("");
        getInstance().getBodyTextArea().setText("");
        getInstance().getBodyTypeComboBox().setSelectedIndex(0);
        initParamTable();
        initHeaderTable();
        initCookieTable();
    }

    /**
     * 切换方法
     *
     * @param method
     */
    public static void switchMethod(String method) {
        if ("GET".equals(method)) {
            getInstance().getBodyTextArea().setText("");
            getInstance().getTabbedPane1().setEnabledAt(3, false);
        } else {
            getInstance().getTabbedPane1().setEnabledAt(3, true);
        }
    }

    /**
     * 初始化ParamTable
     */
    public static void initParamTable() {
        JTable paramTable = getInstance().getParamTable();
        paramTable.setRowHeight(36);
        String[] headerNames = {"Name", "Value", ""};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        paramTable.setModel(model);
        paramTable.updateUI();
        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) paramTable.getTableHeader().getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);

        TableColumnModel tableColumnModel = paramTable.getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(paramTable, headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(paramTable, headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(headerNames.length - 1).setPreferredWidth(46);
        tableColumnModel.getColumn(headerNames.length - 1).setMaxWidth(46);
    }

    /**
     * 初始化HeaderTable
     */
    public static void initHeaderTable() {
        JTable paramTable = getInstance().getHeaderTable();
        paramTable.setRowHeight(36);
        String[] headerNames = {"Name", "Value", ""};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        paramTable.setModel(model);
        paramTable.updateUI();
        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) paramTable.getTableHeader().getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);

        TableColumnModel tableColumnModel = paramTable.getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(paramTable, headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(paramTable, headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(headerNames.length - 1).setPreferredWidth(46);
        tableColumnModel.getColumn(headerNames.length - 1).setMaxWidth(46);
    }

    /**
     * 初始化CookieTable
     */
    public static void initCookieTable() {
        JTable paramTable = getInstance().getCookieTable();
        paramTable.setRowHeight(36);
        String[] headerNames = {"Name", "Value", "Domain", "Path", "Expiry", ""};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        paramTable.setModel(model);
        paramTable.updateUI();
        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) paramTable.getTableHeader().getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);

        TableColumnModel tableColumnModel = paramTable.getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(paramTable, headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(paramTable, headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(headerNames.length - 1).setPreferredWidth(46);
        tableColumnModel.getColumn(headerNames.length - 1).setMaxWidth(46);
    }

    @Getter
    @Setter
    public static class NameValueObject implements Serializable {
        private static final long serialVersionUID = -3828939498243146605L;

        private String name;

        private String value;
    }

    @Getter
    @Setter
    public static class CookieObject implements Serializable {

        private static final long serialVersionUID = 810193087944524307L;

        private String name;

        private String value;

        private String domain;

        private String path;

        private String expiry;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        httpRequestPanel = new JPanel();
        httpRequestPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        httpRequestPanel.setMinimumSize(new Dimension(400, 300));
        httpRequestPanel.setPreferredSize(new Dimension(400, 300));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(0);
        splitPane.setDividerSize(10);
        httpRequestPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setMinimumSize(new Dimension(0, 64));
        splitPane.setLeftComponent(panel1);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        noteListTable = new JTable();
        scrollPane1.setViewportView(noteListTable);
        searchTextField = new JTextField();
        panel1.add(searchTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        rightPanel = new JPanel();
        rightPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(rightPanel);
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        rightPanel.add(contentPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        contentPanel.add(spacer1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        historySplitPane = new JSplitPane();
        contentPanel.add(historySplitPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        historySplitPane.setLeftComponent(panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 10, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        methodComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("GET");
        defaultComboBoxModel1.addElement("POST");
        defaultComboBoxModel1.addElement("PUT");
        defaultComboBoxModel1.addElement("PATCH");
        defaultComboBoxModel1.addElement("DELETE");
        defaultComboBoxModel1.addElement("HEAD");
        defaultComboBoxModel1.addElement("OPTIONS");
        methodComboBox.setModel(defaultComboBoxModel1);
        panel3.add(methodComboBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlTextField = new JTextField();
        panel3.add(urlTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sendButton = new JButton();
        sendButton.setText("");
        sendButton.setToolTipText("发送请求");
        panel3.add(sendButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSplitPane splitPane1 = new JSplitPane();
        splitPane1.setAutoscrolls(false);
        splitPane1.setContinuousLayout(true);
        splitPane1.setDividerLocation(282);
        splitPane1.setDividerSize(9);
        splitPane1.setDoubleBuffered(true);
        splitPane1.setOrientation(0);
        panel2.add(splitPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setRightComponent(panel4);
        panel4.setBorder(BorderFactory.createTitledBorder(null, "响应：", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tabbedPane2 = new JTabbedPane();
        panel5.add(tabbedPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane2.addTab("Body", panel6);
        httpResultScrollPane = new JScrollPane();
        panel6.add(httpResultScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        responseBodyTextArea = new JTextArea();
        httpResultScrollPane.setViewportView(responseBodyTextArea);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane2.addTab("Headers", panel7);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel7.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        headersTextArea = new JTextArea();
        scrollPane2.setViewportView(headersTextArea);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane2.addTab("Cookies", panel8);
        final JScrollPane scrollPane3 = new JScrollPane();
        panel8.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        cookiesTextArea = new JTextArea();
        scrollPane3.setViewportView(cookiesTextArea);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setLeftComponent(panel9);
        panel9.setBorder(BorderFactory.createTitledBorder(null, "请求：", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tabbedPane1 = new JTabbedPane();
        panel10.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Params", panel11);
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(3, 3, new Insets(5, 5, 0, 0), -1, -1));
        panel11.add(panel12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        paramNameTextField = new JTextField();
        panel12.add(paramNameTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        paramValueTextField = new JTextField();
        panel12.add(paramValueTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        paramAddButton = new JButton();
        paramAddButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        paramAddButton.setText("");
        panel12.add(paramAddButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Name");
        panel12.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Value");
        panel12.add(label2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JScrollPane scrollPane4 = new JScrollPane();
        panel12.add(scrollPane4, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        paramTable = new JTable();
        paramTable.setRowHeight(36);
        scrollPane4.setViewportView(paramTable);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Headers", panel13);
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(3, 3, new Insets(5, 5, 0, 0), -1, -1));
        panel13.add(panel14, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        headerNameTextField = new JTextField();
        panel14.add(headerNameTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        headerValueTextField5 = new JTextField();
        panel14.add(headerValueTextField5, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        headerAddButton = new JButton();
        headerAddButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        headerAddButton.setText("");
        panel14.add(headerAddButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Name");
        panel14.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Value");
        panel14.add(label4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JScrollPane scrollPane5 = new JScrollPane();
        panel14.add(scrollPane5, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        headerTable = new JTable();
        headerTable.setRowHeight(36);
        scrollPane5.setViewportView(headerTable);
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Cookies", panel15);
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new GridLayoutManager(3, 6, new Insets(5, 5, 0, 0), -1, -1));
        panel15.add(panel16, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cookieNameTextField = new JTextField();
        panel16.add(cookieNameTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cookieValueTextField = new JTextField();
        panel16.add(cookieValueTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cookieAddButton = new JButton();
        cookieAddButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        cookieAddButton.setText("");
        panel16.add(cookieAddButton, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cookieDomainTextField = new JTextField();
        panel16.add(cookieDomainTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cookiePathTextField = new JTextField();
        panel16.add(cookiePathTextField, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Name");
        panel16.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Value");
        panel16.add(label6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JLabel label7 = new JLabel();
        label7.setText("Domain");
        panel16.add(label7, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Path");
        panel16.add(label8, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Expiry DateTime");
        panel16.add(label9, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cookieExpiryTextField = new JTextField();
        panel16.add(cookieExpiryTextField, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane6 = new JScrollPane();
        panel16.add(scrollPane6, new GridConstraints(2, 0, 1, 6, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        cookieTable = new JTable();
        cookieTable.setRowHeight(36);
        scrollPane6.setViewportView(cookieTable);
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Body", panel17);
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new GridLayoutManager(2, 2, new Insets(5, 0, 0, 0), -1, -1));
        panel17.add(panel18, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        bodyTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("text/plain");
        defaultComboBoxModel2.addElement("application/json");
        defaultComboBoxModel2.addElement("application/javascript");
        defaultComboBoxModel2.addElement("application/xml");
        defaultComboBoxModel2.addElement("text/xml");
        defaultComboBoxModel2.addElement("text/html");
        bodyTypeComboBox.setModel(defaultComboBoxModel2);
        panel18.add(bodyTypeComboBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel18.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JScrollPane scrollPane7 = new JScrollPane();
        panel18.add(scrollPane7, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        bodyTextArea = new JTextArea();
        scrollPane7.setViewportView(bodyTextArea);
        final Spacer spacer3 = new Spacer();
        panel2.add(spacer3, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        historyPanel = new JPanel();
        historyPanel.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        historySplitPane.setRightComponent(historyPanel);
        historyPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel19 = new JPanel();
        panel19.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        historyPanel.add(panel19, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        closeHistoryLabel = new JLabel();
        closeHistoryLabel.setText("");
        closeHistoryLabel.setToolTipText("关闭");
        panel19.add(closeHistoryLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel19.add(spacer4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        historyTableScrollPane = new JScrollPane();
        historyPanel.add(historyTableScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        historyTable = new JTable();
        historyTableScrollPane.setViewportView(historyTable);
        final JPanel panel20 = new JPanel();
        panel20.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        historyPanel.add(panel20, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        deleteHistoryButton = new JButton();
        deleteHistoryButton.setText("");
        deleteHistoryButton.setToolTipText("删除");
        panel20.add(deleteHistoryButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel20.add(spacer5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 5), -1, -1));
        contentPanel.add(controlPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        saveButton = new JButton();
        saveButton.setIcon(new ImageIcon(getClass().getResource("/icon/menu-saveall_dark.png")));
        saveButton.setText("");
        saveButton.setToolTipText("保存");
        controlPanel.add(saveButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        controlPanel.add(spacer6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        addButton = new JButton();
        addButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        addButton.setText("");
        addButton.setToolTipText("新建");
        controlPanel.add(addButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sendToWindowButton = new JButton();
        sendToWindowButton.setIcon(new ImageIcon(getClass().getResource("/icon/send.png")));
        sendToWindowButton.setText("");
        sendToWindowButton.setToolTipText("发送至新窗口");
        controlPanel.add(sendToWindowButton, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        deleteButton.setText("");
        deleteButton.setToolTipText("删除");
        controlPanel.add(deleteButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        historyButton = new JButton();
        historyButton.setText("");
        historyButton.setToolTipText("历史记录");
        controlPanel.add(historyButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return httpRequestPanel;
    }

}
