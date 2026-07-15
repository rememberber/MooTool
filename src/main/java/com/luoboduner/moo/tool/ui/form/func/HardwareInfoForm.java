package com.luoboduner.moo.tool.ui.form.func;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.ui.Style;
import com.luoboduner.moo.tool.ui.component.ToolbarUiUtil;
import com.luoboduner.moo.tool.ui.listener.func.HardwareInfoListener;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.ScrollUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT;

@Getter
public class HardwareInfoForm {

    public static final String TAB_TITLE = "系统信息";

    private JPanel hardwareInfoPanel;
    private JTabbedPane tabbedPane;
    private JTextArea systemTextArea;
    private JTextArea cpuTextArea;
    private JTextArea memoryTextArea;
    private JTextArea diskTextArea;
    private JTextArea networkTextArea;
    private JScrollPane systemScrollPane;
    private JScrollPane cpuScrollPane;
    private JScrollPane memoryScrollPane;
    private JScrollPane diskScrollPane;
    private JScrollPane networkScrollPane;
    private JButton refreshButton;

    private static HardwareInfoForm hardwareInfoForm;

    private static boolean i18nRegistered;

    private static final String PLACEHOLDER_ZH =
            "点击「刷新」或首次进入本页时自动采集系统与硬件信息";
    private static final String PLACEHOLDER_EN =
            "Click Refresh or open this tab to collect system and hardware info";

    private HardwareInfoForm() {
        UndoUtil.register(this);
    }

    public static HardwareInfoForm getInstance() {
        if (hardwareInfoForm == null) {
            hardwareInfoForm = new HardwareInfoForm();
        }
        return hardwareInfoForm;
    }

    public static void init() {
        hardwareInfoForm = getInstance();
        initUi();
        HardwareInfoListener.addListeners();

        hardwareInfoForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(HardwareInfoForm::applyI18nStatic);
            i18nRegistered = true;
        }
    }

    private void applyI18n() {
        I18nUiUtil.setTabTitle(tabbedPane, 0, "hardware.tab.system");
        I18nUiUtil.setTabTitle(tabbedPane, 1, "hardware.tab.cpu");
        I18nUiUtil.setTabTitle(tabbedPane, 2, "hardware.tab.memory");
        I18nUiUtil.setTabTitle(tabbedPane, 3, "hardware.tab.disk");
        I18nUiUtil.setTabTitle(tabbedPane, 4, "hardware.tab.network");
        I18nUiUtil.setToolTip(refreshButton, "common.refresh");
        String placeholder = I18n.get("hardware.placeholder");
        updatePlaceholderIfNeeded(systemTextArea, placeholder);
        updatePlaceholderIfNeeded(cpuTextArea, placeholder);
        updatePlaceholderIfNeeded(memoryTextArea, placeholder);
        updatePlaceholderIfNeeded(diskTextArea, placeholder);
        updatePlaceholderIfNeeded(networkTextArea, placeholder);
    }

    private static void updatePlaceholderIfNeeded(JTextArea textArea, String placeholder) {
        String text = textArea.getText();
        if (text.isEmpty() || text.equals(PLACEHOLDER_ZH) || text.equals(PLACEHOLDER_EN)) {
            textArea.setText(placeholder);
        }
    }

    private static void applyI18nStatic() {
        if (hardwareInfoForm != null) {
            hardwareInfoForm.applyI18n();
        }
    }

    private static void initUi() {
        HardwareInfoForm form = getInstance();

        Style.blackTextArea(form.getSystemTextArea());
        Style.blackTextArea(form.getCpuTextArea());
        Style.blackTextArea(form.getMemoryTextArea());
        Style.blackTextArea(form.getDiskTextArea());
        Style.blackTextArea(form.getNetworkTextArea());

        ScrollUtil.smoothPane(form.getSystemScrollPane());
        ScrollUtil.smoothPane(form.getCpuScrollPane());
        ScrollUtil.smoothPane(form.getMemoryScrollPane());
        ScrollUtil.smoothPane(form.getDiskScrollPane());
        ScrollUtil.smoothPane(form.getNetworkScrollPane());

        String placeholder = I18n.get("hardware.placeholder");
        form.getSystemTextArea().setText(placeholder);
        form.getCpuTextArea().setText(placeholder);
        form.getMemoryTextArea().setText(placeholder);
        form.getDiskTextArea().setText(placeholder);
        form.getNetworkTextArea().setText(placeholder);

        JToolBar trailing = new JToolBar();
        ToolbarUiUtil.configure(trailing);
        trailing.add(Box.createHorizontalGlue());

        form.refreshButton = new JButton(new FlatSVGIcon("icon/refresh.svg"));
        form.refreshButton.setToolTipText("刷新");
        trailing.add(form.refreshButton);
        trailing.add(new JLabel("  "));

        form.getTabbedPane().putClientProperty(TABBED_PANE_TRAILING_COMPONENT, trailing);
    }

    {
        $$$setupUI$$$();
    }

    private void $$$setupUI$$$() {
        hardwareInfoPanel = new JPanel();
        hardwareInfoPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        tabbedPane = new JTabbedPane();
        hardwareInfoPanel.add(tabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));

        JPanel systemPanel = createTextPanel();
        systemScrollPane = (JScrollPane) systemPanel.getComponent(0);
        systemTextArea = (JTextArea) systemScrollPane.getViewport().getView();
        tabbedPane.addTab("系统", systemPanel);

        JPanel cpuPanel = createTextPanel();
        cpuScrollPane = (JScrollPane) cpuPanel.getComponent(0);
        cpuTextArea = (JTextArea) cpuScrollPane.getViewport().getView();
        tabbedPane.addTab("处理器", cpuPanel);

        JPanel memoryPanel = createTextPanel();
        memoryScrollPane = (JScrollPane) memoryPanel.getComponent(0);
        memoryTextArea = (JTextArea) memoryScrollPane.getViewport().getView();
        tabbedPane.addTab("内存", memoryPanel);

        JPanel diskPanel = createTextPanel();
        diskScrollPane = (JScrollPane) diskPanel.getComponent(0);
        diskTextArea = (JTextArea) diskScrollPane.getViewport().getView();
        tabbedPane.addTab("存储", diskPanel);

        JPanel networkPanel = createTextPanel();
        networkScrollPane = (JScrollPane) networkPanel.getComponent(0);
        networkTextArea = (JTextArea) networkScrollPane.getViewport().getView();
        tabbedPane.addTab("网络", networkPanel);

        refreshButton = new JButton();
    }

    private static JPanel createTextPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        JScrollPane scrollPane = new JScrollPane();
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        scrollPane.setViewportView(textArea);
        panel.add(scrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        return panel;
    }

    public JComponent $$$getRootComponent$$$() {
        return hardwareInfoPanel;
    }
}
