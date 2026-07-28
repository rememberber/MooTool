package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.component.ToolbarUiUtil;
import com.luoboduner.moo.tool.ui.dialog.SystemEnvResultDialog;
import com.luoboduner.moo.tool.util.EnvironmentVariableService;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

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
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JComboBox<EnvironmentVariableService.Scope> scopeComboBox;

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
        I18nUiUtil.setToolTip(addButton, "variables.action.add");
        I18nUiUtil.setToolTip(editButton, "variables.action.edit");
        I18nUiUtil.setToolTip(deleteButton, "common.delete");
        scopeComboBox.repaint();
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

        variablesForm.scopeComboBox = new JComboBox<>(EnvironmentVariableService.Scope.values());
        variablesForm.scopeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EnvironmentVariableService.Scope scope) {
                    setText(I18n.get(switch (scope) {
                        case CURRENT -> "variables.scope.current";
                        case USER -> "variables.scope.user";
                        case SYSTEM -> "variables.scope.system";
                    }));
                }
                return this;
            }
        });
        variablesForm.scopeComboBox.addActionListener(e -> initSysEnvVarTable());

        variablesForm.addButton = new JButton(new FlatSVGIcon("icon/add.svg"));
        variablesForm.addButton.addActionListener(e -> variablesForm.showEnvironmentEditor(null));

        variablesForm.editButton = new JButton(new FlatSVGIcon("icon/edit.svg"));
        variablesForm.editButton.addActionListener(e -> variablesForm.editSelectedEnvironmentVariable());

        variablesForm.deleteButton = new JButton(new FlatSVGIcon("icon/remove.svg"));
        variablesForm.deleteButton.addActionListener(e -> variablesForm.deleteSelectedEnvironmentVariable());

        trailing.add(variablesForm.scopeComboBox);
        trailing.addSeparator();
        trailing.add(variablesForm.addButton);
        trailing.add(variablesForm.editButton);
        trailing.add(variablesForm.deleteButton);
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

                dialog.appendTextArea("------------Current process environment---------------");
                for (EnvironmentVariableService.Entry entry
                        : EnvironmentVariableService.list(EnvironmentVariableService.Scope.CURRENT)) {
                    dialog.appendTextArea(entry.key() + "=" + entry.value());
                }

                dialog.appendTextArea("------------User environment---------------");
                for (EnvironmentVariableService.Entry entry
                        : EnvironmentVariableService.list(EnvironmentVariableService.Scope.USER)) {
                    dialog.appendTextArea(entry.key() + "=" + entry.value());
                }

                dialog.appendTextArea("------------System environment---------------");
                for (EnvironmentVariableService.Entry entry
                        : EnvironmentVariableService.list(EnvironmentVariableService.Scope.SYSTEM)) {
                    dialog.appendTextArea(entry.key() + "=" + entry.value());
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
        variablesForm.sysEnvVarTable.getSelectionModel().addListSelectionListener(e -> variablesForm.updateActionState());
        variablesForm.sysEnvVarTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    variablesForm.editSelectedEnvironmentVariable();
                }
            }
        });
        variablesForm.tabbedPane1.addChangeListener(e -> variablesForm.updateActionState());
        variablesForm.updateActionState();

    }

    private static void initInfo() {
        initSysEnvVarTable();
        initJavaPropsTable();
    }

    public static void initSysEnvVarTable() {
        String[] headerNames = {I18n.get("variables.col.key"), I18n.get("variables.col.value")};
        DefaultTableModel model = readOnlyTableModel(headerNames);

        EnvironmentVariableService.Scope scope = getInstance().selectedScope();
        try {
            List<EnvironmentVariableService.Entry> entries = EnvironmentVariableService.list(scope);
            for (EnvironmentVariableService.Entry entry : entries) {
                model.addRow(new Object[]{entry.key(), entry.value()});
            }
        } catch (Exception error) {
            logger.error("读取系统环境变量失败", error);
        }

        JTable sysEnvVarTable = getInstance().getSysEnvVarTable();
        sysEnvVarTable.setModel(model);
        resizeColumns(sysEnvVarTable.getColumnModel());
        getInstance().updateActionState();
    }

    public static void initJavaPropsTable() {
        String[] headerNames = {I18n.get("variables.col.key"), I18n.get("variables.col.value")};
        DefaultTableModel model = readOnlyTableModel(headerNames);

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

    private static DefaultTableModel readOnlyTableModel(String[] headers) {
        return new DefaultTableModel(null, headers) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private EnvironmentVariableService.Scope selectedScope() {
        Object selected = scopeComboBox == null ? null : scopeComboBox.getSelectedItem();
        return selected instanceof EnvironmentVariableService.Scope scope
                ? scope : EnvironmentVariableService.Scope.CURRENT;
    }

    private void editSelectedEnvironmentVariable() {
        int row = sysEnvVarTable.getSelectedRow();
        if (row < 0 || tabbedPane1.getSelectedIndex() != 0) {
            return;
        }
        showEnvironmentEditor(new EnvironmentVariableService.Entry(
                String.valueOf(sysEnvVarTable.getValueAt(row, 0)),
                String.valueOf(sysEnvVarTable.getValueAt(row, 1))));
    }

    private void showEnvironmentEditor(EnvironmentVariableService.Entry existing) {
        if (!EnvironmentVariableService.supportsDirectModification()) {
            JOptionPane.showMessageDialog(variablesPanel, I18n.get("msg.systemEnvUnsupported"));
            return;
        }
        JTextField keyField = new JTextField(existing == null ? "" : existing.key(), 34);
        keyField.setEditable(existing == null);
        JTextArea valueArea = new JTextArea(existing == null ? "" : existing.value(), 6, 42);
        valueArea.setLineWrap(true);
        valueArea.setWrapStyleWord(false);

        JPanel editor = new JPanel(new BorderLayout(8, 8));
        JPanel keyPanel = new JPanel(new BorderLayout(8, 0));
        keyPanel.add(new JLabel(I18n.get("variables.col.key")), BorderLayout.WEST);
        keyPanel.add(keyField, BorderLayout.CENTER);
        editor.add(keyPanel, BorderLayout.NORTH);
        editor.add(new JScrollPane(valueArea), BorderLayout.CENTER);

        EnvironmentVariableService.Scope selectedScope = selectedScope();
        JComboBox<EnvironmentVariableService.Scope> targetScope = null;
        if (selectedScope == EnvironmentVariableService.Scope.CURRENT) {
            targetScope = new JComboBox<>(new EnvironmentVariableService.Scope[]{
                    EnvironmentVariableService.Scope.USER,
                    EnvironmentVariableService.Scope.SYSTEM
            });
            targetScope.setRenderer(scopeComboBox.getRenderer());
            JPanel scopePanel = new JPanel(new BorderLayout(8, 0));
            scopePanel.add(new JLabel(I18n.get("variables.targetScope")), BorderLayout.WEST);
            scopePanel.add(targetScope, BorderLayout.CENTER);
            editor.add(scopePanel, BorderLayout.SOUTH);
        }

        String title = I18n.get(existing == null ? "variables.dialog.addTitle" : "variables.dialog.editTitle");
        int result = JOptionPane.showConfirmDialog(variablesPanel, editor, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String key = keyField.getText().trim();
        String value = valueArea.getText();
        EnvironmentVariableService.Scope mutationScope = selectedScope == EnvironmentVariableService.Scope.CURRENT
                ? (EnvironmentVariableService.Scope) targetScope.getSelectedItem()
                : selectedScope;
        runMutation(() -> {
            EnvironmentVariableService.set(mutationScope, key, value);
            return null;
        });
    }

    private void deleteSelectedEnvironmentVariable() {
        int row = sysEnvVarTable.getSelectedRow();
        if (row < 0 || tabbedPane1.getSelectedIndex() != 0) {
            return;
        }
        String key = String.valueOf(sysEnvVarTable.getValueAt(row, 0));
        int result = JOptionPane.showConfirmDialog(variablesPanel,
                I18n.format("variables.confirmDelete", key),
                I18n.get("common.confirm"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        runMutation(() -> {
            EnvironmentVariableService.delete(selectedScope(), key);
            return null;
        });
    }

    private void runMutation(Callable<Void> mutation) {
        setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                return mutation.call();
            }

            @Override
            protected void done() {
                try {
                    get();
                    initSysEnvVarTable();
                    JOptionPane.showMessageDialog(variablesPanel, I18n.get("variables.saved"),
                            I18n.get("common.confirm"), JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    Throwable cause = error.getCause() == null ? error : error.getCause();
                    logger.error("修改系统环境变量失败", cause);
                    JOptionPane.showMessageDialog(variablesPanel,
                            I18n.format("variables.saveFailed", cause.getMessage()),
                            I18n.get("msg.failedTitle"), JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        variablesPanel.setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        scopeComboBox.setEnabled(!busy);
        refreshButton.setEnabled(!busy);
        addButton.setEnabled(!busy && tabbedPane1.getSelectedIndex() == 0);
        editButton.setEnabled(!busy && tabbedPane1.getSelectedIndex() == 0 && sysEnvVarTable.getSelectedRow() >= 0);
        deleteButton.setEnabled(!busy && tabbedPane1.getSelectedIndex() == 0
                && selectedScope() != EnvironmentVariableService.Scope.CURRENT
                && sysEnvVarTable.getSelectedRow() >= 0);
    }

    private void updateActionState() {
        boolean environmentTab = tabbedPane1.getSelectedIndex() == 0;
        boolean selected = sysEnvVarTable.getSelectedRow() >= 0;
        boolean persistentScope = selectedScope() != EnvironmentVariableService.Scope.CURRENT;
        scopeComboBox.setVisible(environmentTab);
        addButton.setVisible(environmentTab);
        editButton.setVisible(environmentTab);
        deleteButton.setVisible(environmentTab);
        addButton.setEnabled(environmentTab);
        editButton.setEnabled(environmentTab && selected);
        deleteButton.setEnabled(environmentTab && persistentScope && selected);
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
