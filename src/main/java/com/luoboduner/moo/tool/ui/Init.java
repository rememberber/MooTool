package com.luoboduner.moo.tool.ui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.jthemedetecor.OsThemeDetector;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.component.JPopupMenuMouseAdapter;
import com.luoboduner.moo.tool.ui.dialog.FontSizeAdjustDialog;
import com.luoboduner.moo.tool.ui.dialog.SettingDialog;
import com.luoboduner.moo.tool.ui.dialog.TranslationDialog;
import com.luoboduner.moo.tool.ui.form.AboutForm;
import com.luoboduner.moo.tool.ui.form.func.*;
import com.luoboduner.moo.tool.ui.frame.ColorPickerFrame;
import com.luoboduner.moo.tool.ui.listener.FrameListener;
import com.luoboduner.moo.tool.util.SystemUtil;
import com.luoboduner.moo.tool.util.UpgradeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;

/**
 * <pre>
 * 初始化类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/15.
 */
public class Init {

    private static final Log logger = LogFactory.get();

    /**
     * 字号初始化KEY
     */
    private static final String FONT_SIZE_INIT_PROP = "fontSizeInit";

    /**
     * 设置全局字体
     */
    public static void initGlobalFont() {
        if (StringUtils.isEmpty(App.config.getProps(FONT_SIZE_INIT_PROP))) {
            // 根据DPI调整字号
            // 得到屏幕的分辨率dpi
            // dell 1920*1080/24寸=96
            // 小米air 1920*1080/13.3寸=144
            // 小米air 1366*768/13.3寸=96
            int fontSize = 12;

            // Mac等高分辨率屏幕字号初始化
            if (SystemUtil.isMacOs()) {
                fontSize = 12;
            } else {
//                fontSize = (int) (UIUtil.getScreenScale() * fontSize);
            }
            App.config.setFontSize(fontSize);
        }

        Font font = new Font(App.config.getFont(), Font.PLAIN, App.config.getFontSize());
        FontUIResource fontRes = new FontUIResource(font);
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontRes);
            }
        }

    }

    /**
     * 其他初始化
     */
    public static void initOthers() {

    }

    /**
     * 初始化look and feel
     */
    public static void initTheme() {
        try {

            if (App.config.isThemeColorFollowSystem()) {
                final OsThemeDetector detector = OsThemeDetector.getDetector();
                final boolean isDarkThemeUsed = detector.isDark();
                if (isDarkThemeUsed) {
                    FlatMacDarkLaf.setup();
                } else {
                    FlatMacLightLaf.setup();
                }
            } else {
                switch (App.config.getTheme()) {
                    case "系统默认":
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        break;
                    case "Flat Light":
                        setAccentColor();
                        FlatLightLaf.setup();
                        break;
                    case "Flat IntelliJ":
                        setAccentColor();
                        FlatIntelliJLaf.setup();
                        break;
                    case "Flat Dark":
                        setAccentColor();
                        FlatDarkLaf.setup();
                        break;
                    case "Dark purple":
                        FlatDarkPurpleIJTheme.setup();
                        break;
                    case "IntelliJ Cyan":
                        FlatCyanLightIJTheme.setup();
                        break;
                    case "IntelliJ Light":
                        FlatLightFlatIJTheme.setup();
                        break;
                    case "Monocai":
                        FlatMonocaiIJTheme.setup();
                        break;
                    case "Monokai Pro":
                        FlatMonokaiProIJTheme.setup();
                        UIManager.put("Button.arc", 5);
                        break;
                    case "One Dark":
                        FlatOneDarkIJTheme.setup();
                        break;
                    case "Gray":
                        FlatGrayIJTheme.setup();
                        break;
                    case "High contrast":
                        FlatHighContrastIJTheme.setup();
                        break;
                    case "GitHub Dark":
                        FlatMTGitHubDarkIJTheme.setup();
                        break;
                    case "Xcode-Dark":
                        FlatXcodeDarkIJTheme.setup();
                        break;
                    case "Vuesion":
                        FlatVuesionIJTheme.setup();
                        break;
                    case "Flat macOS Light":
                        FlatMacLightLaf.setup();
                        break;
                    case "Flat macOS Dark":
                        FlatMacDarkLaf.setup();
                        break;
                    default:
                        setAccentColor();
                        FlatDarculaLaf.setup();
                }
            }

            if (FlatLaf.isLafDark()) {
//                FlatSVGIcon.ColorFilter.getInstance().setMapper(color -> color.brighter().brighter());
            } else {
                FlatSVGIcon.ColorFilter.getInstance().setMapper(color -> color.darker().darker());
//                SwingUtilities.windowForComponent(App.mainFrame).repaint();
            }

            if (App.config.isUnifiedBackground()) {
                UIManager.put("TitlePane.unifiedBackground", true);
            }

        } catch (Exception e) {
            logger.error(e);
        }
    }

    private static void setAccentColor() {
        String accentColor = App.config.getAccentColor();
        FlatLaf.setGlobalExtraDefaults((!accentColor.equals(SettingDialog.accentColorKeys[0]))
                ? Collections.singletonMap("@accentColor", "$" + accentColor)
                : null);
    }

    /**
     * 初始化所有tab
     */
    public static void initAllTab() {
        ThreadUtil.execute(AboutForm::init);
        ThreadUtil.execute(QuickNoteForm::init);
        ThreadUtil.execute(JsonBeautyForm::init);
        ThreadUtil.execute(TimeConvertForm::init);
        ThreadUtil.execute(HostForm::init);
        ThreadUtil.execute(HttpRequestForm::init);
        ThreadUtil.execute(EnCodeForm::init);
        ThreadUtil.execute(QrCodeForm::init);
        ThreadUtil.execute(CryptoForm::init);
        ThreadUtil.execute(CalculatorForm::init);
        ThreadUtil.execute(ColorBoardForm::init);
        ThreadUtil.execute(NetForm::init);
        ThreadUtil.execute(TranslationForm::init);
        ThreadUtil.execute(CronForm::init);
        ThreadUtil.execute(RegexForm::init);
        ThreadUtil.execute(ImageForm::init);
        ThreadUtil.execute(VariablesForm::init);
        ThreadUtil.execute(YmlPropertiesForm::init);
        ThreadUtil.execute(FileReformattingForm::init);

        // 检查新版版
        if (App.config.isAutoCheckUpdate()) {
            ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1);
            threadPoolExecutor.scheduleAtFixedRate(() -> UpgradeUtil.checkUpdate(true), 0, 24, TimeUnit.HOURS);
        }
    }

    /**
     * 引导用户调整字号
     */
    public static void fontSizeGuide() {
        if (StringUtils.isEmpty(App.config.getProps(FONT_SIZE_INIT_PROP))) {
            FontSizeAdjustDialog fontSizeAdjustDialog = new FontSizeAdjustDialog();
            fontSizeAdjustDialog.pack();
            fontSizeAdjustDialog.setVisible(true);
        }

        App.config.setProps(FONT_SIZE_INIT_PROP, "true");
        App.config.save();
    }

    /**
     * 初始化系统托盘
     */
    public static void initTray() {

        try {
            if (SystemTray.isSupported() && App.tray == null) {
                App.tray = SystemTray.getSystemTray();

                App.popupMenu = new JPopupMenu();
//                App.popupMenu.setFont(App.mainFrame.getContentPane().getFont());

                JMenuItem openItem = new JMenuItem("MooTool");
                JMenuItem colorPickerItem = new JMenuItem("取色器");
                JMenuItem translationItem = new JMenuItem("翻译");
                JMenuItem exitItem = new JMenuItem("Quit");

                openItem.addActionListener(e -> {
                    showMainFrame();
                });
                colorPickerItem.addActionListener(e -> {
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice gd = ge.getDefaultScreenDevice();
                    if (gd.isWindowTranslucencySupported(TRANSLUCENT)) {
                        App.mainFrame.setVisible(false);
                        ColorPickerFrame.showPicker();
                    }
                });
                translationItem.addActionListener(e -> {
                    TranslationDialog translationDialog = new TranslationDialog();
                    translationDialog.pack();
                    translationDialog.setVisible(true);
                });
                exitItem.addActionListener(e -> {
                    shutdown();
                });

                App.popupMenu.add(openItem);
                App.popupMenu.addSeparator();
                App.popupMenu.add(colorPickerItem);
                App.popupMenu.add(translationItem);
                App.popupMenu.addSeparator();
                App.popupMenu.add(exitItem);

                App.trayIcon = new TrayIcon(UiConsts.IMAGE_LOGO_64, "MooTool");
                App.trayIcon.setImageAutoSize(true);

                App.trayIcon.addActionListener(e -> {
                    App.mainFrame.setVisible(true);
                    App.mainFrame.setExtendedState(JFrame.NORMAL);
                    App.mainFrame.requestFocus();
                });

                JPopupMenuMouseAdapter jPopupMenuMouseAdapter = new JPopupMenuMouseAdapter(App.popupMenu);
                App.trayIcon.addMouseListener(jPopupMenuMouseAdapter);

                try {
                    App.tray.add(App.trayIcon);
                } catch (AWTException e) {
                    e.printStackTrace();
                    logger.error(ExceptionUtils.getStackTrace(e));
                }

            }

        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    public static void showMainFrame() {
        App.mainFrame.setVisible(true);
        if (App.mainFrame.getExtendedState() == Frame.ICONIFIED) {
            App.mainFrame.setExtendedState(Frame.NORMAL);
        } else if (App.mainFrame.getExtendedState() == 7) {
            App.mainFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        App.mainFrame.requestFocus();
    }

    public static void shutdown() {
        FrameListener.saveBeforeExit();
        App.sqlSession.close();
        App.mainFrame.dispose();
        System.exit(0);
    }
}
