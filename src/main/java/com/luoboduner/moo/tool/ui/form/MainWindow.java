package com.luoboduner.moo.tool.ui.form;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemInfo;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.func.*;
import com.luoboduner.moo.tool.ui.listener.TabListener;
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

    private static MainWindow mainWindow;

    private static final String[] ICON_PATH = {"icon/edit.svg", "icon/time.svg", "icon/json.svg", "icon/check.svg", "icon/global.svg", "icon/exchange.svg", "icon/QRcode.svg", "icon/method.svg", "icon/calculate.svg", "icon/network.svg", "icon/color.svg", "icon/image.svg", "icon/translate.svg", "icon/schedule.svg", "icon/reg.svg", "icon/java.svg", "icon/gear.svg", "icon/pdf.svg"};

    private MainWindow() {
    }

    public static MainWindow getInstance() {
        if (mainWindow == null) {
            mainWindow = new MainWindow();
        }
        return mainWindow;
    }

    private static GridConstraints gridConstraints = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false);

    public void init() {
        mainWindow = getInstance();
        mainWindow.getMainPanel().updateUI();
        if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) mainPanel.getLayout();
            gridLayoutManager.setMargin(new Insets(25, 0, 0, 0));
        }

        initTabPlacement();

        mainWindow.getQuickNotePanel().add(QuickNoteForm.getInstance().getQuickNotePanel(), gridConstraints);
        mainWindow.getJsonBeautyPanel().add(JsonBeautyForm.getInstance().getJsonBeautyPanel(), gridConstraints);
        mainWindow.getTimeConvertPanel().add(TimeConvertForm.getInstance().getTimeConvertPanel(), gridConstraints);
        mainWindow.getHostPanel().add(HostForm.getInstance().getHostPanel(), gridConstraints);
        mainWindow.getHttpRequestPanel().add(HttpRequestForm.getInstance().getHttpRequestPanel(), gridConstraints);
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
        mainWindow.getMainPanel().updateUI();

        TabListener.addListeners();
    }

    public void initTabPlacement() {
        // 设置所有tab的图标
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setIconAt(i, new FlatSVGIcon(ICON_PATH[i], 16, 16));
        }

        // 设置所有tab的tips和标题一致
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setToolTipTextAt(i, tabbedPane.getTitleAt(i));
        }

        if ("左侧".equals(App.config.getFuncTabPosition())) {
            tabbedPane.setTabPlacement(JTabbedPane.LEFT);
            tabbedPane.putClientProperty(TABBED_PANE_TAB_ALIGNMENT, TABBED_PANE_ALIGN_LEADING);
        } else {
            tabbedPane.setTabPlacement(JTabbedPane.TOP);
        }

        // 紧凑型tab标题
        tabbedPane.putClientProperty(TABBED_PANE_TAB_WIDTH_MODE, TABBED_PANE_TAB_WIDTH_MODE_COMPACT);

        // 隐藏所有 tab 的标题
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setTitleAt(i, "");
            // 设置所有tab的icon放大
            tabbedPane.setIconAt(i, new FlatSVGIcon(ICON_PATH[i], 19, 19));
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(1);
        mainPanel.add(tabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        quickNotePanel = new JPanel();
        quickNotePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("随手记", new ImageIcon(getClass().getResource("/icon/edit.png")), quickNotePanel);
        timeConvertPanel = new JPanel();
        timeConvertPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("时间转换", new ImageIcon(getClass().getResource("/icon/clock.png")), timeConvertPanel);
        jsonBeautyPanel = new JPanel();
        jsonBeautyPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("JSON", new ImageIcon(getClass().getResource("/icon/object_dark.png")), jsonBeautyPanel);
        hostPanel = new JPanel();
        hostPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Host", new ImageIcon(getClass().getResource("/icon/check.png")), hostPanel);
        httpRequestPanel = new JPanel();
        httpRequestPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("HTTP", new ImageIcon(getClass().getResource("/icon/global.png")), httpRequestPanel);
        encodePanel = new JPanel();
        encodePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("编码", new ImageIcon(getClass().getResource("/icon/exchange.png")), encodePanel);
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
        translationPanel = new JPanel();
        translationPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("翻译", new ImageIcon(getClass().getResource("/icon/translate.png")), translationPanel);
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
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
