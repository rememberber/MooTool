package com.luoboduner.moo.tool.ui.form.func;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.ui.listener.func.PDFMergerListener;
import com.luoboduner.moo.tool.ui.listener.func.PDFSplitterListener;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

@Getter
public class PdfForm {
    private JPanel pdfPanel;
    private JButton startTask;
    private JButton newTask;
    private JPanel taskPane;
    private JPanel splitPane;
    private JLabel helpBtn;
    private JPanel mergePane;
    private JButton startMerge;
    private JButton addFile;
    private JLabel helpMerge;
    private JPanel filePane;

    private static PdfForm instance;
    private PDFSplitterListener listener;
    private PDFMergerListener mergerListener;

    private PdfForm() {
        $$$setupUI$$$();
        initSplitContent();
        initMergeContent();
    }

    public static PdfForm getInstance() {
        if (null == instance) {
            synchronized (PdfForm.class) {
                if (null == instance) {
                    instance = new PdfForm();
                }
            }
        }
        return instance;
    }

    private void initMergeContent() {
        FileTableModel fileTableModel = new FileTableModel(10);
        JTable table = new JTable(fileTableModel);
        PDFTableModel.CommonComponentRenderer componentRenderer = fileTableModel.new CommonComponentRenderer();
        table.setDefaultRenderer(JCheckBox.class, componentRenderer);
        table.setDefaultRenderer(JButton.class, componentRenderer);
        table.setDefaultRenderer(JTextField.class, componentRenderer);
        table.setDefaultRenderer(JProgressBar.class, componentRenderer);
        table.setDefaultRenderer(JLabel.class, componentRenderer);

        table.setDefaultEditor(JTextField.class, fileTableModel.new CommonTextFieldEditor());
        table.setDefaultEditor(JCheckBox.class, fileTableModel.new CommonCheckBoxEditor());
        table.setDefaultEditor(JButton.class, fileTableModel.new CommonUploadEditor());

        table.setRowHeight(30);
        table.setRowMargin(5);
        JTableHeader jTableHeader = table.getTableHeader();

        table.getColumnModel().getColumn(0).setMaxWidth(100);
        table.getColumnModel().getColumn(1).setMaxWidth(250);
        table.getColumnModel().getColumn(1).setMinWidth(250);
        table.getColumnModel().getColumn(5).setMaxWidth(200);

        getFilePane().add(jTableHeader, BorderLayout.NORTH);
        getFilePane().add(table, BorderLayout.CENTER);

        mergerListener = new PDFMergerListener(getFilePane(), fileTableModel);
        mergerListener.setComponents(fileTableModel.getComps());
        mergerListener.setStartMerge(getStartMerge());
        mergerListener.setHelpMerge(getHelpMerge());
        mergerListener.setAddFile(getAddFile());
        mergerListener.startListener();

        getHelpMerge().setIcon(new FlatSVGIcon("icon/help.svg"));
    }

    private void initSplitContent() {
        SplitTableModel taskTableModel = new SplitTableModel(10);
        JTable table = new JTable(taskTableModel);
        SplitTableModel.CommonComponentRenderer testRenderer = taskTableModel.new CommonComponentRenderer();
        table.setDefaultRenderer(JCheckBox.class, testRenderer);
        table.setDefaultRenderer(JButton.class, testRenderer);
        table.setDefaultRenderer(JTextField.class, testRenderer);
        table.setDefaultRenderer(JComboBox.class, testRenderer);
        table.setDefaultRenderer(JProgressBar.class, testRenderer);
        table.setDefaultRenderer(JLabel.class, testRenderer);

        table.setDefaultEditor(JCheckBox.class, taskTableModel.new CommonCheckBoxEditor());
        table.setDefaultEditor(JButton.class, taskTableModel.new CommonUploadEditor());
        table.setDefaultEditor(JTextField.class, taskTableModel.new CommonTextFieldEditor());
        table.setDefaultEditor(JComboBox.class, taskTableModel.new CommonComboBoxEditor());

        table.setRowHeight(30);
        table.setRowMargin(5);
        JTableHeader jTableHeader = table.getTableHeader();

        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(4).setMaxWidth(150);
        table.getColumnModel().getColumn(7).setMaxWidth(150);

        getTaskPane().add(jTableHeader, BorderLayout.NORTH);
        getTaskPane().add(table, BorderLayout.CENTER);

        listener = new PDFSplitterListener(taskPane, taskTableModel);
        listener.setNewTask(getNewTask());
        listener.setStartTask(getStartTask());
        listener.setHelpBtn(getHelpBtn());
        listener.setComponents(taskTableModel.getComps());
        listener.startListener();

        getHelpBtn().setIcon(new FlatSVGIcon("icon/help.svg"));
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        pdfPanel = new JPanel();
        pdfPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        tabbedPane1.setTabLayoutPolicy(1);
        tabbedPane1.setTabPlacement(1);
        pdfPanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        splitPane = new JPanel();
        splitPane.setLayout(new GridLayoutManager(2, 4, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("PDF拆分", splitPane);
        startTask = new JButton();
        startTask.setText("开始拆分");
        splitPane.add(startTask, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        splitPane.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        taskPane = new JPanel();
        taskPane.setLayout(new BorderLayout(0, 0));
        splitPane.add(taskPane, new GridConstraints(1, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        taskPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        newTask = new JButton();
        newTask.setText("新增处理任务");
        splitPane.add(newTask, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, 30), null, 0, false));
        helpBtn = new JLabel();
        helpBtn.setText("");
        splitPane.add(helpBtn, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mergePane = new JPanel();
        mergePane.setLayout(new GridLayoutManager(2, 4, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("PDF合并", mergePane);
        final Spacer spacer2 = new Spacer();
        mergePane.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        startMerge = new JButton();
        startMerge.setText("开始合并");
        mergePane.add(startMerge, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addFile = new JButton();
        addFile.setText("新增合并文件");
        mergePane.add(addFile, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        helpMerge = new JLabel();
        helpMerge.setText("");
        mergePane.add(helpMerge, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        filePane = new JPanel();
        filePane.setLayout(new BorderLayout(0, 0));
        mergePane.add(filePane, new GridConstraints(1, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        filePane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return pdfPanel;
    }

    public abstract class PDFTableModel extends AbstractTableModel {

        protected String[] COLUMN_NAMES = new String[]{};

        protected Class<?>[] COLUMN_TYPES = new Class<?>[]{};

        protected List<Object[]> comps;

        protected int count;

        private int maxRow = 20;

        private int rowCount;

        public PDFTableModel(int rowCount) {
            this.comps = new LinkedList<>();
            this.rowCount = rowCount;
        }

        @Override
        public int getRowCount() {
            return rowCount;
        }

        @Override
        public int getColumnCount() {
            if (COLUMN_NAMES.length == 0) {
                this.COLUMN_NAMES = this.initColumns();
            }
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (COLUMN_NAMES.length == 0) {
                this.COLUMN_NAMES = this.initColumns();
            }
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (COLUMN_TYPES.length == 0) {
                this.COLUMN_TYPES = this.initColumnTypes();
            }
            return COLUMN_TYPES[columnIndex];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (comps.isEmpty() || comps.size() <= rowIndex) {
                comps.add(newRow());
            }
            return comps.get(rowIndex)[columnIndex];
        }

        public void add() {
            if (count >= maxRow) {
                JOptionPane.showMessageDialog(taskPane, "最大为20个！", "警告", JOptionPane.WARNING_MESSAGE);
                return;
            }
            rowCount++;
            getComps().add(newRow());
            this.fireTableRowsUpdated(count - 1, count - 1);
        }

        protected List<Object[]> getComps() {
            return comps;
        }

        protected abstract Object[] newRow();

        protected abstract String[] initColumns();

        protected abstract Class<?>[] initColumnTypes();

        protected void selectEditor(boolean isSelected, int row) {
        }

        protected void uploadEditor(File file, int row) {
        }

        protected void textChange(String text, int row, int column) {
        }

        protected void comboBoxChange(String text, int row) {
        }

        protected class CommonComponentRenderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Class<?> columnType = getColumnClass(column);
                return (Component) columnType.cast(value);
            }
        }

        protected class CommonCheckBoxEditor extends DefaultCellEditor {

            public CommonCheckBoxEditor() {
                super(new JTextField());
                this.setClickCountToStart(1);
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                JCheckBox checkBox = (JCheckBox) value;
                if (checkBox.getActionListeners().length == 0) {
                    checkBox.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            selectEditor(checkBox.isSelected(), row);
                        }
                    });
                }
                return checkBox;
            }
        }

        protected class CommonUploadEditor extends DefaultCellEditor {
            private static final long serialVersionUID = -6546334664166791132L;

            public CommonUploadEditor() {
                super(new JTextField());
                this.setClickCountToStart(1);
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                JButton button = (JButton) value;
                if (button.getActionListeners().length == 0) {
                    button.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            JFileChooser fileChooser = new JFileChooser();
                            int option = fileChooser.showOpenDialog(getTaskPane());
                            if (option == JFileChooser.APPROVE_OPTION) {
                                File file = fileChooser.getSelectedFile();
                                uploadEditor(file, row);
                            }
                        }
                    });
                }
                return button;
            }
        }

        protected class CommonTextFieldEditor extends DefaultCellEditor {

            public CommonTextFieldEditor() {
                super(new JTextField());
                this.setClickCountToStart(1);
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                JTextField textField = (JTextField) value;
                textField.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        textChange(textField.getText(), row, column);
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                    }
                });
                return textField;
            }
        }

        protected class CommonComboBoxEditor extends DefaultCellEditor {
            public CommonComboBoxEditor() {
                super(new JTextField());
                this.setClickCountToStart(1);
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                JComboBox comboBox = (JComboBox) value;
                comboBox.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            JComboBox<String> combo = (JComboBox<String>) e.getSource();
                            String selectedItem = (String) combo.getSelectedItem();
                            comboBoxChange(selectedItem, row);
                        }
                    }
                });
                return comboBox;
            }
        }
    }

    public class FileTableModel extends PDFTableModel {

        public FileTableModel(int rowCount) {
            super(rowCount);
        }

        protected Object[] newRow() {
            Object[] components = new Object[7];
            JCheckBox jCheckBox = new JCheckBox();
            jCheckBox.setHorizontalAlignment(JLabel.CENTER);
            jCheckBox.setText(++count + "");
            components[0] = jCheckBox;
            components[1] = new JButton(COLUMN_NAMES[1]);
            components[2] = new JLabel();
            components[3] = new JTextField();
            JProgressBar progressBar = new JProgressBar();
            progressBar.setPreferredSize(new Dimension(150, 2));
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
            progressBar.setBounds(0, 0, 150, 2);
            components[4] = progressBar;
            JLabel label = new JLabel("未开始");
            label.setHorizontalAlignment(JLabel.CENTER);
            components[5] = label;
            return components;
        }

        @Override
        protected String[] initColumns() {
            return new String[]{"#", "选择文件", "文件名", "PDF合并范围", "合并进度", "状态"};
        }

        @Override
        protected Class<?>[] initColumnTypes() {
            return new Class<?>[]{JCheckBox.class, JButton.class, JLabel.class, JTextField.class, JProgressBar.class, JLabel.class};
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                case 3:
                    return true;
                default:
                    return false;
            }
        }

        protected void uploadEditor(File file, int row) {
            mergerListener.selectFile(file, row);
        }

        protected void textChange(String text, int row, int column) {
            if (3 == column) {
                mergerListener.changeMergeRange(text, row);
            }
        }

        protected void selectEditor(boolean isSelected, int row) {
            mergerListener.setMerge(isSelected, row);
        }
    }

    public class SplitTableModel extends PDFTableModel {

        public SplitTableModel(int rowCount) {
            super(rowCount);
        }

        @Override
        protected Object[] newRow() {
            Object[] components = new Object[9];
            JCheckBox jCheckBox = new JCheckBox();
            jCheckBox.setHorizontalAlignment(JLabel.CENTER);
            jCheckBox.setText(++count + "");
            components[0] = jCheckBox;
            components[1] = new JButton(COLUMN_NAMES[1]);
            components[2] = new JLabel();
            components[3] = new JTextField();

            JComboBox<String> comboBox = new JComboBox<>();
            final DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
            comboBoxModel.addElement("奇数");
            comboBoxModel.addElement("偶数");
            comboBoxModel.addElement("自定义");
            comboBox.setModel(comboBoxModel);

            components[4] = comboBox;
            JTextField jTextField = new JTextField();
            jTextField.setEnabled(false);
            components[5] = jTextField;
            JProgressBar progressBar = new JProgressBar();
            progressBar.setPreferredSize(new Dimension(150, 2));
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
            progressBar.setBounds(0, 0, 150, 2);
            components[6] = progressBar;
            JLabel label = new JLabel("未开始");
            label.setHorizontalAlignment(JLabel.CENTER);
            components[7] = label;
            return components;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                case 3:
                case 4:
                case 5:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        protected String[] initColumns() {
            return new String[]{"#", "选择文件", "文件名", "PDF拆分范围", "拆分规则", "自定义规则", "拆分进度", "状态"};
        }

        @Override
        protected Class<?>[] initColumnTypes() {
            return new Class<?>[]{JCheckBox.class, JButton.class, JLabel.class, JTextField.class, JComboBox.class, JTextField.class, JProgressBar.class, JLabel.class};
        }

        protected void selectEditor(boolean isSelected, int row) {
            listener.setSplit(row, isSelected);
        }

        protected void uploadEditor(File file, int row) {
            listener.selectFile(file, row);
        }

        protected void textChange(String text, int row, int column) {
            if (3 == column) {
                listener.changeSplitRange(text, row);
            } else if (5 == column) {
                listener.changeSplitRule(text, row);
            }
        }

        protected void comboBoxChange(String text, int row) {
            listener.changeSplitRuleType(text, row);
        }
    }
}
