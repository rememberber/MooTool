package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.form.TranslationLayoutForm;
import com.luoboduner.moo.tool.ui.listener.func.TranslationListener;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.JTableUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.AccessLevel;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * TranslationForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/12/13.
 */
@Getter
public class TranslationForm {
    private JPanel translationPanel;
    private JTable listTable;
    private JButton button4;
    private JSplitPane splitPane;
    private JPanel translatePanel;

    private JButton wordBookAddButton;
    private JTextField wordBookSearchField;
    private JPanel wordBookDetailPanel;
    private JTextArea wordSourceTextArea;
    private JTextArea wordTargetTextArea;
    private JLabel wordLangLabel;
    private JTextField wordRemarkTextField;
    private JButton wordSaveButton;
    private JButton wordRetranslateButton;
    private JButton wordCopySourceButton;
    private JButton wordCopyTargetButton;

    @Getter(AccessLevel.NONE)
    private JTabbedPane translationTabbedPane;
    @Getter(AccessLevel.NONE)
    private JPanel historyPanel;
    private JTable historyTable;
    private JTextField historySearchField;
    private JButton historyDeleteButton;
    private JButton historyClearButton;
    private JButton historyApplyButton;
    private JButton historyCopySourceButton;
    private JButton historyCopyTargetButton;
    private JTextArea historySourceTextArea;
    private JTextArea historyTargetTextArea;
    private JLabel historyMetaLabel;

    private static TranslationForm translationForm;

    private static boolean i18nRegistered;

    private TranslationLayoutForm translationLayoutForm;

    private static final Log logger = LogFactory.get();

    public JTabbedPane getTranslationTabbedPane() {
        bindGeneratedUiRefs();
        return translationTabbedPane;
    }

    public JPanel getHistoryPanel() {
        bindGeneratedUiRefs();
        return historyPanel;
    }

    /**
     * GUI Designer 生成的 $$$setupUI$$$ 使用局部变量，此处从已挂载组件树回填字段引用。
     */
    private void bindGeneratedUiRefs() {
        if (translationTabbedPane == null && translationPanel != null) {
            for (Component child : translationPanel.getComponents()) {
                if (child instanceof JTabbedPane tabbedPane) {
                    translationTabbedPane = tabbedPane;
                    break;
                }
            }
        }
        if (historyPanel == null && translationTabbedPane != null && translationTabbedPane.getTabCount() > 2) {
            Component historyTab = translationTabbedPane.getComponentAt(2);
            if (historyTab instanceof JPanel panel) {
                historyPanel = panel;
            }
        }
    }

    private TranslationForm(TranslationLayoutForm translationLayoutForm) {
        this.translationLayoutForm = translationLayoutForm;
        bindGeneratedUiRefs();
        translatePanel.setLayout(new BorderLayout());
        translatePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        translatePanel.add(translationLayoutForm.getMainLayoutPanel());

        initWordBookPanel();
        initHistoryPanel();

        UndoUtil.register(this);
    }

    private void initWordBookPanel() {
        wordBookSearchField = new JTextField();
        wordBookSearchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "搜索单词或译文");
        wordBookSearchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());

        wordBookAddButton = new JButton(new FlatSVGIcon("icon/add.svg"));
        wordBookAddButton.setToolTipText("新建单词");
        button4.setIcon(new FlatSVGIcon("icon/remove.svg"));
        button4.setToolTipText("删除选中");

        JPanel originalLeft = (JPanel) splitPane.getLeftComponent();
        JScrollPane tableScrollPane = null;
        for (Component child : originalLeft.getComponents()) {
            if (child instanceof JPanel) {
                for (Component grandChild : ((JPanel) child).getComponents()) {
                    if (grandChild instanceof JScrollPane) {
                        tableScrollPane = (JScrollPane) grandChild;
                        break;
                    }
                }
            }
            if (tableScrollPane != null) {
                break;
            }
        }

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonBar.add(wordBookAddButton);
        buttonBar.add(button4);

        JPanel tableArea = new JPanel(new BorderLayout());
        if (tableScrollPane != null) {
            tableArea.add(tableScrollPane, BorderLayout.CENTER);
        }
        tableArea.add(buttonBar, BorderLayout.SOUTH);

        JPanel newLeft = new JPanel(new BorderLayout(0, 6));
        newLeft.add(wordBookSearchField, BorderLayout.NORTH);
        newLeft.add(tableArea, BorderLayout.CENTER);
        splitPane.setLeftComponent(newLeft);

        wordSourceTextArea = new JTextArea(4, 20);
        wordTargetTextArea = new JTextArea(4, 20);
        wordSourceTextArea.setLineWrap(true);
        wordTargetTextArea.setLineWrap(true);
        wordTargetTextArea.setEditable(false);

        wordLangLabel = new JLabel(" ");
        wordRemarkTextField = new JTextField();
        wordRemarkTextField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "备注（可选）");

        wordSaveButton = new JButton("保存");
        wordSaveButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        wordRetranslateButton = new JButton("重新翻译");
        wordRetranslateButton.setIcon(new FlatSVGIcon("icon/translate.svg"));
        wordCopySourceButton = new JButton(new FlatSVGIcon("icon/copy.svg"));
        wordCopySourceButton.setToolTipText("复制原文");
        wordCopyTargetButton = new JButton(new FlatSVGIcon("icon/copy.svg"));
        wordCopyTargetButton.setToolTipText("复制译文");

        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        toolbarPanel.add(wordSaveButton);
        toolbarPanel.add(wordRetranslateButton);
        toolbarPanel.add(wordCopySourceButton);
        toolbarPanel.add(wordCopyTargetButton);

        JPanel detailPanel = new JPanel(new BorderLayout(0, 8));
        detailPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        detailPanel.add(wordLangLabel, BorderLayout.NORTH);

        JSplitPane detailSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        detailSplitPane.setTopComponent(new JScrollPane(wordSourceTextArea));
        detailSplitPane.setBottomComponent(new JScrollPane(wordTargetTextArea));
        detailSplitPane.setResizeWeight(0.5);
        detailSplitPane.setContinuousLayout(true);
        detailPanel.add(detailSplitPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 6));
        bottomPanel.add(wordRemarkTextField, BorderLayout.CENTER);
        bottomPanel.add(toolbarPanel, BorderLayout.SOUTH);
        detailPanel.add(bottomPanel, BorderLayout.SOUTH);

        wordBookDetailPanel = detailPanel;
        splitPane.setRightComponent(wordBookDetailPanel);
    }

    private void initHistoryPanel() {
        bindGeneratedUiRefs();
        if (historyPanel == null) {
            historyPanel = new JPanel();
            JTabbedPane tabbedPane = getTranslationTabbedPane();
            if (tabbedPane != null && tabbedPane.getTabCount() >= 3) {
                tabbedPane.setComponentAt(2, historyPanel);
            } else if (tabbedPane != null) {
                tabbedPane.addTab("历史记录", historyPanel);
            }
        }

        historySearchField = new JTextField();
        historySearchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "搜索原文、译文或语言");
        historySearchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());

        historyDeleteButton = new JButton(new FlatSVGIcon("icon/remove.svg"));
        historyDeleteButton.setToolTipText("删除选中");
        historyClearButton = new JButton("清空全部");
        historyApplyButton = new JButton("应用到翻译");
        historyApplyButton.setIcon(new FlatSVGIcon("icon/translate.svg"));
        historyCopySourceButton = new JButton(new FlatSVGIcon("icon/copy.svg"));
        historyCopySourceButton.setToolTipText("复制原文");
        historyCopyTargetButton = new JButton(new FlatSVGIcon("icon/copy.svg"));
        historyCopyTargetButton.setToolTipText("复制译文");

        historyTable = new JTable();
        historySourceTextArea = new JTextArea(4, 20);
        historyTargetTextArea = new JTextArea(4, 20);
        historySourceTextArea.setLineWrap(true);
        historyTargetTextArea.setLineWrap(true);
        historyTargetTextArea.setEditable(false);
        historyMetaLabel = new JLabel(" ");

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonBar.add(historyApplyButton);
        buttonBar.add(historyCopySourceButton);
        buttonBar.add(historyCopyTargetButton);
        buttonBar.add(historyDeleteButton);
        buttonBar.add(historyClearButton);

        JPanel detailPanel = new JPanel(new BorderLayout(0, 8));
        detailPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        detailPanel.add(historyMetaLabel, BorderLayout.NORTH);

        JSplitPane detailSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        detailSplitPane.setTopComponent(new JScrollPane(historySourceTextArea));
        detailSplitPane.setBottomComponent(new JScrollPane(historyTargetTextArea));
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

        historyPanel.setLayout(new BorderLayout(0, 6));
        historyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        historyPanel.add(historySearchField, BorderLayout.NORTH);
        historyPanel.add(historySplitPane, BorderLayout.CENTER);
    }

    public static TranslationForm getInstance() {
        if (translationForm == null) {
            TranslationLayoutForm translationLayoutForm = new TranslationLayoutForm();
            translationForm = new TranslationForm(translationLayoutForm);
        }
        return translationForm;
    }

    public static void init() {
        translationForm = getInstance();
        translationForm.bindGeneratedUiRefs();

        initUi();
        initWordBookTable();
        initHistoryTable();
        TranslationListener.addListeners();

        translationForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(TranslationForm::applyI18nStatic);
            i18nRegistered = true;
        }
    }

    private void applyI18n() {
        JTabbedPane tabbedPane = getTranslationTabbedPane();
        if (tabbedPane != null) {
            if (tabbedPane.getTabCount() > 0) {
                I18nUiUtil.setTabTitle(tabbedPane, 0, "translation.tab.translate");
            }
            if (tabbedPane.getTabCount() > 1) {
                I18nUiUtil.setTabTitle(tabbedPane, 1, "translation.tab.wordBook");
            }
            if (tabbedPane.getTabCount() > 2) {
                I18nUiUtil.setTabTitle(tabbedPane, 2, "translation.tab.history");
            }
        }
        I18nUiUtil.setPlaceholder(wordBookSearchField, "translation.searchWord");
        I18nUiUtil.setToolTip(wordBookAddButton, "translation.tooltip.newWord");
        I18nUiUtil.setToolTip(button4, "translation.tooltip.deleteSelected");
        I18nUiUtil.setPlaceholder(wordRemarkTextField, "translation.remarkPlaceholder");
        I18nUiUtil.setText(wordSaveButton, "common.save");
        I18nUiUtil.setText(wordRetranslateButton, "translation.retranslate");
        I18nUiUtil.setToolTip(wordCopySourceButton, "translation.tooltip.copySource");
        I18nUiUtil.setToolTip(wordCopyTargetButton, "translation.tooltip.copyTarget");
        I18nUiUtil.setPlaceholder(historySearchField, "translation.searchHistory");
        I18nUiUtil.setToolTip(historyDeleteButton, "translation.tooltip.deleteSelected");
        I18nUiUtil.setText(historyClearButton, "translation.clearAll");
        I18nUiUtil.setText(historyApplyButton, "translation.applyToTranslate");
        I18nUiUtil.setToolTip(historyCopySourceButton, "translation.tooltip.copySource");
        I18nUiUtil.setToolTip(historyCopyTargetButton, "translation.tooltip.copyTarget");
        if (translationLayoutForm != null) {
            translationLayoutForm.applyI18n();
        }
    }

    private static void applyI18nStatic() {
        if (translationForm != null) {
            translationForm.applyI18n();
        }
    }

    private static void initUi() {
        translationForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
        translationForm.getListTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);
        translationForm.getHistoryTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);

        translationForm.getTranslationLayoutForm().getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 2) - 80);

        translationForm.getTranslationPanel().updateUI();
    }

    public static void initWordBookTable() {
        TranslationForm form = getInstance();
        form.getListTable().setModel(TranslationListener.createWordBookTableModel());
        form.getListTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        form.getListTable().getTableHeader().setReorderingAllowed(false);
        JTableUtil.hideColumn(form.getListTable(), 0);
        TranslationListener.refreshWordBookList();
    }

    public static void initHistoryTable() {
        TranslationForm form = getInstance();
        form.getHistoryTable().setModel(TranslationListener.createHistoryTableModel());
        form.getHistoryTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        form.getHistoryTable().getTableHeader().setReorderingAllowed(false);
        JTableUtil.hideColumn(form.getHistoryTable(), 0);
        TranslationListener.refreshHistoryList();
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
        translationPanel = new JPanel();
        translationPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        translationPanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        translatePanel = new JPanel();
        translatePanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("翻译", translatePanel);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("单词本", panel1);
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(2);
        panel1.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setLeftComponent(panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        listTable = new JTable();
        scrollPane1.setViewportView(listTable);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        button4 = new JButton();
        button4.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        button4.setText("");
        panel4.add(button4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel4.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel5);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("历史记录", panel6);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return translationPanel;
    }

}
