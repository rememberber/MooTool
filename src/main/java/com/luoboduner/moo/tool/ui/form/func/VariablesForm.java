package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.component.ToolbarUiUtil;
import com.luoboduner.moo.tool.ui.dialog.SystemEnvResultDialog;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.ScrollUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Map;
import java.util.Properties;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT;

@Getter
public class VariablesForm {
    private JTabbedPane tabbedPane1;
    private JPanel variablesPanel;
    private JTable sysEnvVarTable;
    private JTable javaPropsTable;
    private JScrollPane scrollPane1;
    private JScrollPane scrollPane2;
    private JButton refreshButton;
    private JButton exportButton;

    private static VariablesForm variablesForm;

    private static boolean i18nRegistered;

    private static final Log logger = LogFactory.get();

    private static final double[] COLUMN_WIDTH_PERCENT = {0.38, 0.62};

    private VariablesForm() {
        UndoUtil.register(this);
    }

    public static VariablesForm getInstance() {
        if (variablesForm == null) {
            variablesForm = new VariablesForm();
        }
        return variablesForm;
    }

    public static void init() {
        variablesForm = getInstance();

        initUi();

        initInfo();

        variablesForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(VariablesForm::applyI18nStatic);
            i18nRegistered = true;
        }
    }

    private void applyI18n() {
        I18nUiUtil.setTabTitle(tabbedPane1, 0, "variables.tab.sysEnv");
        I18nUiUtil.setTabTitle(tabbedPane1, 1, "variables.tab.javaProps");
        I18nUiUtil.setToolTip(refreshButton, "common.refresh");
        I18nUiUtil.setToolTip(exportButton, "common.export");
        updateTableHeaders();
    }

    private void updateTableHeaders() {
        String[] headerNames = {I18n.get("variables.col.key"), I18n.get("variables.col.value")};
        if (sysEnvVarTable.getModel() instanceof DefaultTableModel sysModel) {
            sysModel.setColumnIdentifiers(headerNames);
        }
        if (javaPropsTable.getModel() instanceof DefaultTableModel javaModel) {
            javaModel.setColumnIdentifiers(headerNames);
        }
    }

    private static void applyI18nStatic() {
        if (variablesForm != null) {
            variablesForm.applyI18n();
        }
    }

    private static void initUi() {
        getInstance().getSysEnvVarTable().setShowGrid(true);
        getInstance().getJavaPropsTable().setShowGrid(true);

        // 设置滚动条速度
        ScrollUtil.smoothPane(variablesForm.getScrollPane1());
        ScrollUtil.smoothPane(variablesForm.getScrollPane2());

        JToolBar trailing = new JToolBar();
        ToolbarUiUtil.configure(trailing);
        trailing.add(Box.createHorizontalGlue());

        variablesForm.refreshButton = new JButton(new FlatSVGIcon("icon/refresh.svg"));
        variablesForm.refreshButton.setToolTipText("刷新");
        variablesForm.refreshButton.addActionListener(e -> {
            initSysEnvVarTable();
            initJavaPropsTable();
        });

        variablesForm.exportButton = new JButton(new FlatSVGIcon("icon/export.svg"));
        variablesForm.exportButton.setToolTipText("导出");
        variablesForm.exportButton.addActionListener(e -> {
            try {
                SystemEnvResultDialog dialog = new SystemEnvResultDialog();

                dialog.appendTextArea("------------System.getenv---------------");
                Map<String, String> map = System.getenv();
                for (Map.Entry<String, String> envEntry : map.entrySet()) {
                    dialog.appendTextArea(envEntry.getKey() + "=" + envEntry.getValue());
                }

                dialog.appendTextArea("------------System.getProperties---------------");
                Properties properties = System.getProperties();
                for (Map.Entry<Object, Object> objectObjectEntry : properties.entrySet()) {
                    dialog.appendTextArea(objectObjectEntry.getKey() + "=" + objectObjectEntry.getValue());
                }

                dialog.pack();
                dialog.setVisible(true);
            } catch (Exception e2) {
                logger.error("查看系统环境变量失败", e2);
            }
        });

        trailing.add(variablesForm.refreshButton);
        trailing.add(variablesForm.exportButton);
        trailing.add(new JLabel("  "));

        getInstance().getTabbedPane1().putClientProperty(TABBED_PANE_TRAILING_COMPONENT, trailing);

    }

    private static void initInfo() {
        initSysEnvVarTable();
        initJavaPropsTable();
    }

    public static void initSysEnvVarTable() {
        String[] headerNames = {I18n.get("variables.col.key"), I18n.get("variables.col.value")};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);

        Map<String, String> map = System.getenv();
        Object[] data;
        for (Map.Entry<String, String> envEntry : map.entrySet()) {
            data = new Object[2];
            data[0] = envEntry.getKey();
            data[1] = envEntry.getValue();
            model.addRow(data);
        }

        JTable sysEnvVarTable = getInstance().getSysEnvVarTable();
        sysEnvVarTable.setModel(model);
        resizeColumns(sysEnvVarTable.getColumnModel());
    }

    public static void initJavaPropsTable() {
        String[] headerNames = {I18n.get("variables.col.key"), I18n.get("variables.col.value")};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);

        Properties properties = System.getProperties();
        Object[] data;
        for (Map.Entry<Object, Object> objectObjectEntry : properties.entrySet()) {
            data = new Object[2];
            data[0] = objectObjectEntry.getKey();
            data[1] = objectObjectEntry.getValue();
            model.addRow(data);
        }

        JTable javaPropsTable = getInstance().getJavaPropsTable();
        javaPropsTable.setModel(model);
        resizeColumns(javaPropsTable.getColumnModel());
    }

    private static void resizeColumns(TableColumnModel tableColumnModel) {
        TableColumn column;
        int tW = App.mainFrame.getWidth() - 20;
        int cantCols = tableColumnModel.getColumnCount();
        for (int i = 0; i < cantCols; i++) {
            column = tableColumnModel.getColumn(i);
            int pWidth = (int) Math.round(COLUMN_WIDTH_PERCENT[i] * tW);
            column.setPreferredWidth(pWidth);
        }
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
        variablesPanel = new JPanel();
        variablesPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        tabbedPane1 = new JTabbedPane();
        variablesPanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("系统环境变量", panel1);
        scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        sysEnvVarTable = new JTable();
        scrollPane1.setViewportView(sysEnvVarTable);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("Java properties", panel2);
        scrollPane2 = new JScrollPane();
        panel2.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        javaPropsTable = new JTable();
        scrollPane2.setViewportView(javaPropsTable);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return variablesPanel;
    }

}
