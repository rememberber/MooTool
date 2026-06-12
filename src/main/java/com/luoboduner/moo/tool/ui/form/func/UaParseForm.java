package com.luoboduner.moo.tool.ui.form.func;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.domain.TFuncHistory;
import com.luoboduner.moo.tool.ui.FuncConsts;
import com.luoboduner.moo.tool.ui.Style;
import com.luoboduner.moo.tool.ui.component.FuncHistoryPanel;
import com.luoboduner.moo.tool.ui.listener.func.UaParseListener;
import com.luoboduner.moo.tool.util.FuncHistorySupport;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.UaParseUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * UA 分析
 */
@Getter
public class UaParseForm {
    private JPanel uaParsePanel;
    private JTextArea uaInputTextArea;
    private JTable resultTable;
    private JButton parseButton;
    private JButton clearButton;
    private JButton pasteButton;
    private JComboBox<String> presetComboBox;
    private JPanel inputPanel;
    private JPanel resultPanel;

    private static UaParseForm uaParseForm;

    private static boolean i18nRegistered;

    private static FuncHistoryPanel historyPanel;

    private UaParseForm() {
        UndoUtil.register(this);
    }

    public static UaParseForm getInstance() {
        if (uaParseForm == null) {
            uaParseForm = new UaParseForm();
        }
        return uaParseForm;
    }

    public static void init() {
        uaParseForm = getInstance();
        initUi();
        historyPanel = FuncHistorySupport.wrapWithTabs(
                uaParseForm.getUaParsePanel(), "UA分析", FuncConsts.UA_PARSE, UaParseForm::applyHistory);
        UaParseListener.addListeners();

        uaParseForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(UaParseForm::applyI18nStatic);
            i18nRegistered = true;
        }
    }

    private void applyI18n() {
        I18nUiUtil.setTitledBorder(inputPanel, "ua.inputBorder");
        I18nUiUtil.setTitledBorder(resultPanel, "ua.resultBorder");
        I18nUiUtil.setText(parseButton, "ua.parse");
        I18nUiUtil.setText(pasteButton, "common.paste");
        I18nUiUtil.setText(clearButton, "common.clear");
        if (presetComboBox.getItemCount() > 0) {
            int selectedIndex = presetComboBox.getSelectedIndex();
            presetComboBox.removeItemAt(0);
            presetComboBox.insertItemAt(I18n.get("ua.presetPlaceholder"), 0);
            if (selectedIndex >= 0) {
                presetComboBox.setSelectedIndex(selectedIndex);
            }
        }
    }

    private static void applyI18nStatic() {
        if (uaParseForm != null) {
            uaParseForm.applyI18n();
        }
    }

    public static FuncHistoryPanel getHistoryPanel() {
        return historyPanel;
    }

    public static void applyHistory(TFuncHistory history) {
        if (history == null) {
            return;
        }
        uaParseForm.getUaInputTextArea().setText(history.getInputText());
        UaParseListener.parse(uaParseForm, false);
    }

    private static void initUi() {
        Style.blackTextArea(uaParseForm.getUaInputTextArea());

        uaParseForm.getParseButton().setIcon(new FlatSVGIcon("icon/run.svg"));
        uaParseForm.getPasteButton().setIcon(new FlatSVGIcon("icon/copy.svg"));
        uaParseForm.getClearButton().setIcon(new FlatSVGIcon("icon/remove.svg"));

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("选择预设 UA...");
        for (String[] preset : UaParseUtil.presetUserAgents()) {
            model.addElement(preset[0]);
        }
        uaParseForm.getPresetComboBox().setModel(model);

        uaParseForm.getUaParsePanel().updateUI();
    }

    {
        $$$setupUI$$$();
    }

    private void $$$setupUI$$$() {
        uaParsePanel = new JPanel();
        uaParsePanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        uaParsePanel.setMinimumSize(new Dimension(400, 300));
        uaParsePanel.setPreferredSize(new Dimension(400, 300));

        inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 8, 0), -1, -1));
        inputPanel.setBorder(BorderFactory.createTitledBorder(null, "User-Agent 输入", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        uaParsePanel.add(inputPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(-1, 150), null, 0, false));

        final JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 4, 0), 8, -1));
        inputPanel.add(toolbarPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        presetComboBox = new JComboBox<>();
        toolbarPanel.add(presetComboBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(180, -1), null, 0, false));

        parseButton = new JButton("解析");
        toolbarPanel.add(parseButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        pasteButton = new JButton("粘贴");
        toolbarPanel.add(pasteButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        clearButton = new JButton("清空");
        toolbarPanel.add(clearButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JScrollPane inputScroll = new JScrollPane();
        inputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        inputPanel.add(inputScroll, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        uaInputTextArea = new JTextArea();
        uaInputTextArea.setLineWrap(true);
        uaInputTextArea.setWrapStyleWord(true);
        inputScroll.setViewportView(uaInputTextArea);

        resultPanel = new JPanel();
        resultPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        resultPanel.setBorder(BorderFactory.createTitledBorder(null, "解析结果", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        uaParsePanel.add(resultPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        final JScrollPane resultScroll = new JScrollPane();
        resultPanel.add(resultScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        resultTable = new JTable();
        resultTable.setFillsViewportHeight(true);
        resultScroll.setViewportView(resultTable);
    }

    public JComponent $$$getRootComponent$$$() {
        return uaParsePanel;
    }
}
