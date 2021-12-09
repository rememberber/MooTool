package com.luoboduner.moo.tool.ui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.IntelliJTheme;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.dialog.FontSizeAdjustDialog;
import com.luoboduner.moo.tool.ui.form.func.*;
import com.luoboduner.moo.tool.ui.frame.ColorPickerFrame;
import com.luoboduner.moo.tool.ui.listener.FrameListener;
import com.luoboduner.moo.tool.util.SystemUtil;
import com.luoboduner.moo.tool.util.UIUtil;
import com.luoboduner.moo.tool.util.UpgradeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
                fontSize = 15;
            } else {
                fontSize = (int) (UIUtil.getScreenScale() * fontSize);
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
        if (SystemUtil.isMacM1()) {
            try {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
                logger.warn("FlatDarculaLaf theme set.");
            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e2) {
                    logger.error(ExceptionUtils.getStackTrace(e2));
                }
                logger.error(ExceptionUtils.getStackTrace(e));
            }
            return;
        }

        if (App.config.isUnifiedBackground()) {
            UIManager.put("TitlePane.unifiedBackground", true);
        }

        try {
            switch (App.config.getTheme()) {
                case "系统默认":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "Flat Light":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    FlatLightLaf.install();
                    break;
                case "Flat IntelliJ":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    UIManager.setLookAndFeel("com.formdev.flatlaf.FlatIntelliJLaf");
                    break;
                case "Flat Dark":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
                    break;
                case "BeautyEye":
                case "Darcula":
                case "Darcula(推荐)":
                case "weblaf":
                case "Flat Darcula(推荐)":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");

                    UIManager.put("PopupMenu.background", UIManager.getColor("Panel.background"));

/**
 If you don't like/want it, you can disable it with:
 UIManager.put( "TitlePane.useWindowDecorations", false );

 It is also possible to disable only the embedded menu bar (and keep the dark title pane) with:
 UIManager.put( "TitlePane.menuBarEmbedded", false );

 It is also possible to disable this on command line with following VM options:
 -Dflatlaf.useWindowDecorations=false
 -Dflatlaf.menuBarEmbedded=false

 If you have following code in your app, you can remove it (no longer necessary):
 // enable window decorations
 JFrame.setDefaultLookAndFeelDecorated( true );
 JDialog.setDefaultLookAndFeelDecorated( true );
 **/
                    break;
                case "Dark purple":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    IntelliJTheme.install(App.class.getResourceAsStream(
                            "/theme/DarkPurple.theme.json"));
                    break;
                case "IntelliJ Cyan":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    IntelliJTheme.install(App.class.getResourceAsStream(
                            "/theme/Cyan.theme.json"));
                    break;
                case "IntelliJ Light":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    IntelliJTheme.install(App.class.getResourceAsStream(
                            "/theme/Light.theme.json"));
                    break;

                default:
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * 初始化所有tab
     */
    public static void initAllTab() {
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

                App.popupMenu = new PopupMenu();
                App.popupMenu.setFont(App.mainFrame.getContentPane().getFont());

                MenuItem openItem = new MenuItem("MooTool");
                MenuItem colorPickerItem = new MenuItem("取色器");
                MenuItem exitItem = new MenuItem("Quit");

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
                exitItem.addActionListener(e -> {
                    shutdown();
                });

                App.popupMenu.add(openItem);
                App.popupMenu.addSeparator();
                App.popupMenu.add(colorPickerItem);
                App.popupMenu.addSeparator();
                App.popupMenu.add(exitItem);

                App.trayIcon = new TrayIcon(UiConsts.IMAGE_LOGO_64, "MooTool", App.popupMenu);
                App.trayIcon.setImageAutoSize(true);

                App.trayIcon.addActionListener(e -> {
                    App.mainFrame.setVisible(true);
                    App.mainFrame.setExtendedState(JFrame.NORMAL);
                    App.mainFrame.requestFocus();
                });
                App.trayIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        switch (e.getButton()) {
                            case MouseEvent.BUTTON1: {
                                showMainFrame();
                                break;
                            }
                            case MouseEvent.BUTTON2: {
                                logger.debug("托盘图标中键事件");
                                break;
                            }
                            case MouseEvent.BUTTON3: {
                                logger.debug("托盘图标右键事件");
                                break;
                            }
                            default: {
                                break;
                            }
                        }
                    }
                });

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
