package com.luoboduner.moo.tool.ui.component;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.luoboduner.moo.tool.dao.TFuncHistoryMapper;
import com.luoboduner.moo.tool.domain.TFuncHistory;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.FuncHistoryUtil;
import com.luoboduner.moo.tool.util.JTableUtil;
import com.luoboduner.moo.tool.util.MybatisUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用功能历史记录面板
 */
public class FuncHistoryPanel extends JPanel {

    private static final String[] HISTORY_COLUMNS = {"id", "时间", "摘要", "操作"};

    private final String funcType;
    private final Consumer<TFuncHistory> applyHandler;

    private final JTextField searchField = new JTextField();
    private final JTable historyTable = new JTable();
    private final JTextArea inputTextArea = new JTextArea(4, 20);
    private final JTextArea outputTextArea = new JTextArea(4, 20);
    private final JLabel metaLabel = new JLabel(" ");
    private final JButton applyButton = new JButton("应用");
    private final JButton copyInputButton = new JButton(new FlatSVGIcon("icon/copy.svg"));
    private final JButton copyOutputButton = new JButton(new FlatSVGIcon("icon/copy.svg"));
    private final JButton deleteButton = new JButton(new FlatSVGIcon("icon/remove.svg"));
    private final JButton clearButton = new JButton("清空全部");

    private Integer selectedHistoryId;

    public FuncHistoryPanel(String funcType, Consumer<TFuncHistory> applyHandler) {
        this.funcType = funcType;
        this.applyHandler = applyHandler;
        initUi();
        addListeners();
        refreshList();
    }

    private void initUi() {
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "搜索摘要、输入或输出");
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());

        applyButton.setIcon(new FlatSVGIcon("icon/run.svg"));
        copyInputButton.setToolTipText("复制输入");
        copyOutputButton.setToolTipText("复制输出");
        deleteButton.setToolTipText("删除选中");
        inputTextArea.setLineWrap(true);
        outputTextArea.setLineWrap(true);
        outputTextArea.setEditable(false);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonBar.add(applyButton);
        buttonBar.add(copyInputButton);
        buttonBar.add(copyOutputButton);
        buttonBar.add(deleteButton);
        buttonBar.add(clearButton);

        JPanel detailPanel = new JPanel(new BorderLayout(0, 8));
        detailPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        detailPanel.add(metaLabel, BorderLayout.NORTH);

        JSplitPane detailSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        detailSplitPane.setTopComponent(new JScrollPane(inputTextArea));
        detailSplitPane.setBottomComponent(new JScrollPane(outputTextArea));
        detailSplitPane.setResizeWeight(0.5);
        detailSplitPane.setContinuousLayout(true);
        detailPanel.add(detailSplitPane, BorderLayout.CENTER);
        detailPanel.add(buttonBar, BorderLayout.SOUTH);

        JSplitPane historySplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        historySplitPane.setContinuousLayout(true);
        historySplitPane.setDividerSize(2);
        historySplitPane.setDividerLocation(260);
        historySplitPane.setLeftComponent(new JScrollPane(historyTable));
        historySplitPane.setRightComponent(detailPanel);

        add(searchField, BorderLayout.NORTH);
        add(historySplitPane, BorderLayout.CENTER);

        historyTable.setModel(createTableModel());
        JTableUtil.hideColumn(historyTable, 0);
    }

    private void addListeners() {
        applyButton.addActionListener(e -> applySelectedHistory());
        copyInputButton.addActionListener(e -> copyText(inputTextArea.getText(), copyInputButton));
        copyOutputButton.addActionListener(e -> copyText(outputTextArea.getText(), copyOutputButton));
        deleteButton.addActionListener(e -> deleteSelectedHistory());
        clearButton.addActionListener(e -> clearAllHistory());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = historyTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    historyTable.setRowSelectionInterval(row, row);
                    viewHistoryByRow(row);
                    if (e.getClickCount() >= 2) {
                        applySelectedHistory();
                    }
                }
            }
        });

        historyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = historyTable.getSelectedRow();
                if (row >= 0) {
                    viewHistoryByRow(row);
                }
            }
        });
    }

    public void refreshListIfVisible() {
        refreshList();
    }

    public void refreshList() {
        DefaultTableModel model = (DefaultTableModel) historyTable.getModel();
        loadIntoModel(model, searchField.getText());
        JTableUtil.hideColumn(historyTable, 0);

        if (selectedHistoryId != null) {
            selectRowById(selectedHistoryId);
        } else if (historyTable.getRowCount() > 0) {
            historyTable.setRowSelectionInterval(0, 0);
            viewHistoryByRow(0);
        } else {
            clearDetail();
        }
    }

    private DefaultTableModel createTableModel() {
        DefaultTableModel model = new DefaultTableModel(HISTORY_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        loadIntoModel(model, "");
        return model;
    }

    private void loadIntoModel(DefaultTableModel model, String keyword) {
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }

        List<TFuncHistory> histories;
        TFuncHistoryMapper mapper = MybatisUtil.getSqlSession().getMapper(TFuncHistoryMapper.class);
        if (StringUtils.isBlank(keyword)) {
            histories = mapper.selectByFuncType(funcType);
        } else {
            histories = mapper.selectByFuncTypeAndFilter(funcType, "%" + keyword.trim() + "%");
        }

        for (TFuncHistory history : histories) {
            model.addRow(new Object[]{
                    history.getId(),
                    history.getCreateTime(),
                    FuncHistoryUtil.previewText(history.getSummary(), 40),
                    FuncHistoryUtil.previewText(history.getExtraData(), 24)
            });
        }
    }

    private void viewHistoryByRow(int row) {
        Integer id = (Integer) historyTable.getValueAt(row, 0);
        if (id == null) {
            return;
        }
        selectedHistoryId = id;
        TFuncHistory history = MybatisUtil.getSqlSession().getMapper(TFuncHistoryMapper.class)
                .selectByPrimaryKey(id);
        if (history == null) {
            clearDetail();
            return;
        }
        inputTextArea.setText(StringUtils.defaultString(history.getInputText()));
        outputTextArea.setText(StringUtils.defaultString(history.getOutputText()));
        metaLabel.setText(StringUtils.defaultIfBlank(history.getExtraData(), " "));
        inputTextArea.setCaretPosition(0);
        outputTextArea.setCaretPosition(0);
    }

    private void selectRowById(Integer id) {
        for (int i = 0; i < historyTable.getRowCount(); i++) {
            if (id.equals(historyTable.getValueAt(i, 0))) {
                historyTable.setRowSelectionInterval(i, i);
                viewHistoryByRow(i);
                return;
            }
        }
    }

    private void clearDetail() {
        selectedHistoryId = null;
        inputTextArea.setText("");
        outputTextArea.setText("");
        metaLabel.setText(" ");
    }

    private void applySelectedHistory() {
        if (selectedHistoryId == null || applyHandler == null) {
            return;
        }
        TFuncHistory history = MybatisUtil.getSqlSession().getMapper(TFuncHistoryMapper.class)
                .selectByPrimaryKey(selectedHistoryId);
        if (history != null) {
            applyHandler.accept(history);
        }
    }

    private void deleteSelectedHistory() {
        if (selectedHistoryId == null) {
            return;
        }
        MybatisUtil.getSqlSession().getMapper(TFuncHistoryMapper.class).deleteByPrimaryKey(selectedHistoryId);
        selectedHistoryId = null;
        refreshList();
    }

    private void clearAllHistory() {
        int confirm = JOptionPane.showConfirmDialog(this, "确定清空全部历史记录？", "确认",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        MybatisUtil.getSqlSession().getMapper(TFuncHistoryMapper.class).deleteAllByFuncType(funcType);
        selectedHistoryId = null;
        refreshList();
    }

    private void copyText(String text, JButton button) {
        if (StringUtils.isNotBlank(text)) {
            ClipboardUtil.setStr(text);
            AlertUtil.buttonInfo(button, "", "已复制", 1500);
        }
    }
}
