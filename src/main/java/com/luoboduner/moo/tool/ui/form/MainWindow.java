package com.luoboduner.moo.tool.ui.form;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemInfo;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.FuncTabCatalog;
import com.luoboduner.moo.tool.ui.UiMetrics;
import com.luoboduner.moo.tool.ui.component.FuncTabGroupSidebar;
import com.luoboduner.moo.tool.ui.component.TabUiUtil;
import com.luoboduner.moo.tool.ui.form.func.*;
import com.luoboduner.moo.tool.ui.listener.TabListener;
import com.luoboduner.moo.tool.ui.dialog.FuncNavigatorDialog;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.SystemUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

import static com.formdev.flatlaf.FlatClientProperties.*;

/**
 * <pre>
 * 主界面
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/8/11.
 */
@Getter
public class MainWindow {
    private JTabbedPane tabbedPane;
    private JPanel mainPanel;
    private JPanel quickNotePanel;
    private JPanel jsonBeautyPanel;
    private JPanel timeConvertPanel;
    private JPanel hostPanel;
    private JPanel httpRequestPanel;
    private JPanel uaParsePanel;
    private JPanel encodePanel;
    private JPanel qrCodePanel;
    private JPanel cryptoPanel;
    private JPanel calculatorPanel;
    private JPanel netPanel;
    private JPanel colorBoardPanel;
    private JPanel translationPanel;
    private JPanel cronPanel;
    private JPanel regexPanel;
    private JPanel imagePanel;
    private JPanel javaConsolePanel;
    private JPanel reformattingPanel;
    private JPanel pdfPanel;
    private JPanel variablesPanel;
    private JPanel hardwareInfoPanel;
    private JPanel ymlProperties;
    private JPanel textDiffPanel;
    private JPanel protoBufPanel;
    private JPanel aboutPanel;

    private static MainWindow mainWindow;

    private boolean tabUiRestoreListenerInstalled;

    private boolean tabStripLayoutListenerInstalled;

    private JButton toggleTitleButton;

    private JButton funcNavigatorButton;

    private JPanel tabLeadingPanel;

    private JPanel funcShellPanel;

    private FuncTabGroupSidebar funcTabGroupSidebar;

    private MainWindow() {
    }

    public static MainWindow getInstance() {
        if (mainWindow == null) {
            mainWindow = new MainWindow();
            TabUiUtil.installSafeUi(mainWindow.tabbedPane, App.config.isFuncTabOnLeft());
            mainWindow.syncTabStripBeforeLayout();
        }
        return mainWindow;
    }

    /**
     * 在首次 layout 前同步 Tab 位置与 i18n 标题，避免仍按设计器中文标题估算 Tab 栏宽度。
     */
    private void syncTabStripBeforeLayout() {
        if (App.config.isFuncTabOnLeft()) {
            tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        } else {
            tabbedPane.setTabPlacement(JTabbedPane.TOP);
        }
        applyTabTitleVisibility(App.config.isTabHideTitle());
    }

    private static GridConstraints gridConstraints = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(0, 200), null, 0, false);

    public void init() {
        mainWindow = getInstance();
        if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) mainPanel.getLayout();
            gridLayoutManager.setMargin(UiMetrics.macMainPanelMarginTop());
        }

        ensureTabUiRestoreListener();
        ensureTabStripLayoutListener();
        initTabPlacement();

        mainWindow.getAboutPanel().add(AboutForm.getInstance().getAboutPanel(), gridConstraints);
        mainWindow.getQuickNotePanel().add(QuickNoteForm.getInstance().getQuickNotePanel(), gridConstraints);
        mainWindow.getJsonBeautyPanel().add(JsonBeautyForm.getInstance().getJsonBeautyPanel(), gridConstraints);
        mainWindow.getTimeConvertPanel().add(TimeConvertForm.getInstance().getTimeConvertPanel(), gridConstraints);
        mainWindow.getHostPanel().add(HostForm.getInstance().getHostPanel(), gridConstraints);
        mainWindow.getHttpRequestPanel().add(HttpRequestForm.getInstance().getHttpRequestPanel(), gridConstraints);
        mainWindow.getUaParsePanel().add(UaParseForm.getInstance().getUaParsePanel(), gridConstraints);
        mainWindow.getEncodePanel().add(EnCodeForm.getInstance().getEnCodePanel(), gridConstraints);
        mainWindow.getQrCodePanel().add(QrCodeForm.getInstance().getQrCodePanel(), gridConstraints);
        mainWindow.getCryptoPanel().add(CryptoForm.getInstance().getCryptoPanel(), gridConstraints);
        mainWindow.getCalculatorPanel().add(CalculatorForm.getInstance().getCalculatorPanel(), gridConstraints);
        mainWindow.getNetPanel().add(NetForm.getInstance().getNetPanel(), gridConstraints);
        mainWindow.getColorBoardPanel().add(ColorBoardForm.getInstance().getColorBoardPanel(), gridConstraints);
        mainWindow.getTranslationPanel().add(TranslationForm.getInstance().getTranslationPanel(), gridConstraints);
        mainWindow.getCronPanel().add(CronForm.getInstance().getCronPanel(), gridConstraints);
        mainWindow.getRegexPanel().add(RegexForm.getInstance().getRegexPanel(), gridConstraints);
        mainWindow.getImagePanel().add(ImageForm.getInstance().getImagePanel(), gridConstraints);
        mainWindow.getJavaConsolePanel().add(JavaConsoleForm.getInstance().getJavaConsolePanel(), gridConstraints);
        mainWindow.getReformattingPanel().add(FileReformattingForm.getInstance().getReformattingPanel(), gridConstraints);
        mainWindow.getPdfPanel().add(PdfForm.getInstance().getPdfPanel(), gridConstraints);
        mainWindow.getVariablesPanel().add(VariablesForm.getInstance().getVariablesPanel(), gridConstraints);
        mainWindow.getHardwareInfoPanel().add(HardwareInfoForm.getInstance().getHardwareInfoPanel(), gridConstraints);
        mainWindow.getYmlProperties().add(YmlPropertiesForm.getInstance().getYmlPropertiesPanel(), gridConstraints);
        mainWindow.getTextDiffPanel().add(TextDiffForm.getInstance().getTextDiffPanel(), gridConstraints);
        mainWindow.getProtoBufPanel().add(ProtoBufForm.getInstance().getProtoBufPanel(), gridConstraints);

        refreshTabbedPaneUi();
        TabUiUtil.applySafeTabbedPaneUi(mainPanel, tabbedPane);
        TabUiUtil.relaxTabContentMinimumSizes(tabbedPane);
        initFuncTabShell();
        refreshFuncTabNavigation();
        TabListener.addListeners();
        relayoutAfterTabStripChanged();
    }

    private void initFuncTabShell() {
        if (funcShellPanel != null) {
            return;
        }
        funcShellPanel = new JPanel(new BorderLayout());
        mainPanel.removeAll();
        mainPanel.add(funcShellPanel, gridConstraints);
        funcShellPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    public void refreshFuncTabNavigation() {
        initFuncTabShell();
        boolean grouped = App.config.isFuncTabGrouped();
        if (grouped) {
            TabUiUtil.installSafeUi(tabbedPane, false);
            if (funcTabGroupSidebar == null) {
                funcTabGroupSidebar = new FuncTabGroupSidebar(tabbedPane);
            } else {
                funcTabGroupSidebar.refresh();
            }
            if (funcTabGroupSidebar.getParent() != funcShellPanel) {
                funcShellPanel.add(funcTabGroupSidebar, BorderLayout.WEST);
            }
            setTabStripHidden(true);
            tabbedPane.putClientProperty(TABBED_PANE_LEADING_COMPONENT, null);
        } else {
            if (funcTabGroupSidebar != null && funcTabGroupSidebar.getParent() == funcShellPanel) {
                funcShellPanel.remove(funcTabGroupSidebar);
            }
            setTabStripHidden(false);
            ensureTabLeadingComponent();
        }
        relayoutAfterTabStripChanged();
    }

    private void setTabStripHidden(boolean hidden) {
        TabUiUtil.setTabStripHidden(tabbedPane, hidden);
        if (hidden) {
            tabbedPane.putClientProperty(TABBED_PANE_SHOW_CONTENT_SEPARATOR, false);
        } else {
            tabbedPane.putClientProperty(TABBED_PANE_SHOW_CONTENT_SEPARATOR, null);
            tabbedPane.putClientProperty(TABBED_PANE_TAB_HEIGHT, null);
        }
    }

    public void refreshTabbedPaneUi() {
        if (App.config.isFuncTabGrouped()) {
            TabUiUtil.installSafeUi(tabbedPane, false);
            return;
        }
        TabUiUtil.installSafeUi(tabbedPane, App.config.isFuncTabOnLeft());
        ensureTabLeadingComponent();
    }

    public void initTabPlacement() {
        if (App.config.isFuncTabGrouped()) {
            tabbedPane.setTabPlacement(JTabbedPane.TOP);
            if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
                GridLayoutManager gridLayoutManager = (GridLayoutManager) mainPanel.getLayout();
                gridLayoutManager.setMargin(UiMetrics.macMainPanelMarginTop());
            } else {
                GridLayoutManager gridLayoutManager = (GridLayoutManager) mainPanel.getLayout();
                gridLayoutManager.setMargin(UiMetrics.zero());
            }
            applyTabTitleVisibility(App.config.isTabHideTitle());
            refreshTabbedPaneUi();
            refreshFuncTabNavigation();
            return;
        }
        if (App.config.isFuncTabOnLeft()) {
            tabbedPane.setTabPlacement(JTabbedPane.LEFT);
            tabbedPane.putClientProperty(TABBED_PANE_TAB_ALIGNMENT, TABBED_PANE_ALIGN_LEADING);

            if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
                GridLayoutManager gridLayoutManager = (GridLayoutManager) mainPanel.getLayout();
                gridLayoutManager.setMargin(UiMetrics.macTabLeftPanelMarginTop());
            } else {
                GridLayoutManager gridLayoutManager = (GridLayoutManager) mainPanel.getLayout();
                gridLayoutManager.setMargin(UiMetrics.tabLeftPanelMarginTop());
            }
        } else {
            tabbedPane.setTabPlacement(JTabbedPane.TOP);

            if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
                GridLayoutManager gridLayoutManager = (GridLayoutManager) mainPanel.getLayout();
                gridLayoutManager.setMargin(UiMetrics.macMainPanelMarginTop());
            } else {
                GridLayoutManager gridLayoutManager = (GridLayoutManager) mainPanel.getLayout();
                gridLayoutManager.setMargin(UiMetrics.zero());
            }
        }

        applyTabTitleVisibility(App.config.isTabHideTitle());

        if (App.config.isTabCard()) {
            tabbedPane.putClientProperty(TABBED_PANE_TAB_TYPE, TABBED_PANE_TAB_TYPE_CARD);
        } else {
            tabbedPane.putClientProperty(TABBED_PANE_TAB_TYPE, TABBED_PANE_TAB_TYPE_UNDERLINED);
        }

        if (App.config.isTabSeparator()) {
            tabbedPane.putClientProperty(TABBED_PANE_SHOW_TAB_SEPARATORS, true);
        } else {
            tabbedPane.putClientProperty(TABBED_PANE_SHOW_TAB_SEPARATORS, false);
        }

        refreshTabbedPaneUi();
        refreshFuncTabNavigation();
        relayoutAfterTabStripChanged();
    }

    public void refreshTabTitles() {
        applyTabTitleVisibility(App.config.isTabHideTitle());
        if (App.config.isFuncTabGrouped() && funcTabGroupSidebar != null) {
            funcTabGroupSidebar.refresh();
        }
        relayoutAfterTabStripChanged();
    }

    private void relayoutAfterTabStripChanged() {
        TabUiUtil.relayoutAfterTabStripChanged(tabbedPane, mainPanel);
    }

    private String tabTitleAt(int index) {
        return FuncTabCatalog.titleAt(index);
    }

    private void applyTabWidthMode(boolean iconOnly) {
        if (iconOnly) {
            tabbedPane.putClientProperty(TABBED_PANE_TAB_WIDTH_MODE, TABBED_PANE_TAB_WIDTH_MODE_ICON_ONLY);
        } else if (App.config.isTabCompact()) {
            tabbedPane.putClientProperty(TABBED_PANE_TAB_WIDTH_MODE, TABBED_PANE_TAB_WIDTH_MODE_COMPACT);
        } else {
            tabbedPane.putClientProperty(TABBED_PANE_TAB_WIDTH_MODE, TABBED_PANE_TAB_WIDTH_MODE_PREFERRED);
        }
    }

    private void applyTabTitleVisibility(boolean iconOnly) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String title = tabTitleAt(i);
            tabbedPane.setToolTipTextAt(i, title);
            tabbedPane.setIconAt(i, FuncTabCatalog.iconAt(i, iconOnly));
            tabbedPane.setTitleAt(i, iconOnly ? "" : title);
        }
        applyTabWidthMode(iconOnly);
        TabUiUtil.forceTabContentLayout(tabbedPane);
    }

    private void ensureTabLeadingComponent() {
        if (App.config.isFuncTabGrouped()) {
            return;
        }
        if (funcNavigatorButton == null) {
            funcNavigatorButton = new JButton(new FlatSVGIcon("icon/find.svg"));
            funcNavigatorButton.setToolTipText(I18n.get("funcGroup.navigator.tooltip"));
            funcNavigatorButton.addActionListener(e -> FuncNavigatorDialog.showDialog());
        }
        if (toggleTitleButton == null) {
            toggleTitleButton = new JButton(new FlatSVGIcon("icon/list.svg"));
            toggleTitleButton.addActionListener(e -> {
                boolean iconOnly = !App.config.isTabHideTitle();
                App.config.setTabHideTitle(iconOnly);
                App.config.save();
                refreshTabTitles();
            });
        }
        if (tabLeadingPanel == null) {
            tabLeadingPanel = new JPanel();
        }
        tabLeadingPanel.removeAll();
        if (App.config.isFuncTabOnLeft()) {
            tabLeadingPanel.setLayout(new GridLayoutManager(2, 1, UiMetrics.tabLeadingInsets(true), -1, -1));
        } else {
            tabLeadingPanel.setLayout(new GridLayoutManager(1, 2, UiMetrics.tabLeadingInsets(false), -1, -1));
        }
        tabLeadingPanel.add(funcNavigatorButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(-1, -1), null, 0, false));
        tabLeadingPanel.add(toggleTitleButton, new GridConstraints(App.config.isFuncTabOnLeft() ? 1 : 0, App.config.isFuncTabOnLeft() ? 0 : 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(-1, -1), null, 0, false));
        tabbedPane.putClientProperty(TABBED_PANE_LEADING_COMPONENT, tabLeadingPanel);
    }

    private void ensureTabUiRestoreListener() {
        if (tabUiRestoreListenerInstalled) {
            return;
        }
        tabbedPane.addPropertyChangeListener("UI", evt -> refreshTabbedPaneUi());
        tabUiRestoreListenerInstalled = true;
    }

    private void ensureTabStripLayoutListener() {
        if (tabStripLayoutListenerInstalled) {
            return;
        }
        tabbedPane.addPropertyChangeListener(TabUiUtil.createTabStripLayoutListener(tabbedPane));
        tabStripLayoutListenerInstalled = true;
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(1);
        mainPanel.add(tabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        aboutPanel = new JPanel();
        aboutPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("MooTool", aboutPanel);
        quickNotePanel = new JPanel();
        quickNotePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("随手记", new ImageIcon(getClass().getResource("/icon/edit.png")), quickNotePanel);
        timeConvertPanel = new JPanel();
        timeConvertPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("时间转换", new ImageIcon(getClass().getResource("/icon/clock.png")), timeConvertPanel);
        jsonBeautyPanel = new JPanel();
        jsonBeautyPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("JSON", new ImageIcon(getClass().getResource("/icon/object_dark.png")), jsonBeautyPanel);
        translationPanel = new JPanel();
        translationPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("翻译", new ImageIcon(getClass().getResource("/icon/translate.png")), translationPanel);
        hostPanel = new JPanel();
        hostPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Host", new ImageIcon(getClass().getResource("/icon/check.png")), hostPanel);
        httpRequestPanel = new JPanel();
        httpRequestPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("HTTP", new ImageIcon(getClass().getResource("/icon/global.png")), httpRequestPanel);
        uaParsePanel = new JPanel();
        uaParsePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("UA分析", uaParsePanel);
        encodePanel = new JPanel();
        encodePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("编码转换", new ImageIcon(getClass().getResource("/icon/exchange.png")), encodePanel);
        qrCodePanel = new JPanel();
        qrCodePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("二维码", new ImageIcon(getClass().getResource("/icon/QR_code.png")), qrCodePanel);
        cryptoPanel = new JPanel();
        cryptoPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("加解密/随机", new ImageIcon(getClass().getResource("/icon/method.png")), cryptoPanel);
        calculatorPanel = new JPanel();
        calculatorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("计算", new ImageIcon(getClass().getResource("/icon/calculator.png")), calculatorPanel);
        netPanel = new JPanel();
        netPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("网络/IP", new ImageIcon(getClass().getResource("/icon/network.png")), netPanel);
        colorBoardPanel = new JPanel();
        colorBoardPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("调色板", new ImageIcon(getClass().getResource("/icon/color.png")), colorBoardPanel);
        imagePanel = new JPanel();
        imagePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("图片助手", new ImageIcon(getClass().getResource("/icon/image.png")), imagePanel);
        cronPanel = new JPanel();
        cronPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Cron", new ImageIcon(getClass().getResource("/icon/cron.png")), cronPanel);
        regexPanel = new JPanel();
        regexPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("正则", new ImageIcon(getClass().getResource("/icon/reg.png")), regexPanel);
        javaConsolePanel = new JPanel();
        javaConsolePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Java", javaConsolePanel);
        reformattingPanel = new JPanel();
        reformattingPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        reformattingPanel.setToolTipText("格式化");
        tabbedPane.addTab("格式化", reformattingPanel);
        pdfPanel = new JPanel();
        pdfPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("PDF", pdfPanel);
        variablesPanel = new JPanel();
        variablesPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("环境变量", variablesPanel);
        hardwareInfoPanel = new JPanel();
        hardwareInfoPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("系统信息", hardwareInfoPanel);
        ymlProperties = new JPanel();
        ymlProperties.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("配置文件转换", ymlProperties);
        textDiffPanel = new JPanel();
        textDiffPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("文本对比", new ImageIcon(getClass().getResource("/icon/find.png")), textDiffPanel);
        protoBufPanel = new JPanel();
        protoBufPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Protobuf", protoBufPanel);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
