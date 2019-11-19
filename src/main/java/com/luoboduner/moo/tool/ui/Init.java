package com.luoboduner.moo.tool.ui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.dialog.FontSizeAdjustDialog;
import com.luoboduner.moo.tool.ui.form.AboutForm;
import com.luoboduner.moo.tool.ui.form.SettingForm;
import com.luoboduner.moo.tool.ui.form.func.CalculatorForm;
import com.luoboduner.moo.tool.ui.form.func.ColorBoardForm;
import com.luoboduner.moo.tool.ui.form.func.CryptoForm;
import com.luoboduner.moo.tool.ui.form.func.EnCodeForm;
import com.luoboduner.moo.tool.ui.form.func.HostForm;
import com.luoboduner.moo.tool.ui.form.func.HttpRequestForm;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.ui.form.func.NetForm;
import com.luoboduner.moo.tool.ui.form.func.QrCodeForm;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.ui.form.func.TimeConvertForm;
import com.luoboduner.moo.tool.ui.frame.ColorPickerFrame;
import com.luoboduner.moo.tool.ui.listener.FrameListener;
import com.luoboduner.moo.tool.util.SystemUtil;
import com.luoboduner.moo.tool.util.UIUtil;
import com.luoboduner.moo.tool.util.UpgradeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

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

        try {
            switch (App.config.getTheme()) {
                case "BeautyEye":
                    BeautyEyeLNFHelper.launchBeautyEyeLNF();
                    UIManager.put("RootPane.setupButtonVisible", false);
                    break;
                case "系统默认":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "weblaf":
                case "Darcula(推荐)":
                default:
                    if (SystemUtil.isLinuxOs()) {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } else {
                        UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
                    }
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * 初始化所有tab
     */
    public static void initAllTab() {
        ThreadUtil.execute(SettingForm::init);
        ThreadUtil.execute(QuickNoteForm::init);
        ThreadUtil.execute(JsonBeautyForm::init);
        ThreadUtil.execute(TimeConvertForm::init);
        ThreadUtil.execute(HostForm::init);
        ThreadUtil.execute(HttpRequestForm::init);
        ThreadUtil.execute(AboutForm::init);
        ThreadUtil.execute(EnCodeForm::init);
        ThreadUtil.execute(QrCodeForm::init);
        ThreadUtil.execute(CryptoForm::init);
        ThreadUtil.execute(CalculatorForm::init);
        ThreadUtil.execute(ColorBoardForm::init);
        ThreadUtil.execute(NetForm::init);

        // 检查新版版
        if (App.config.isAutoCheckUpdate()) {
            ThreadUtil.execute(() -> UpgradeUtil.checkUpdate(true));
        }
    }

    /**
     * 引导用户调整字号
     */
    public static void initFontSize() {
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
                                App.mainFrame.setVisible(true);
                                App.mainFrame.setExtendedState(JFrame.NORMAL);
                                App.mainFrame.requestFocus();
                                break;
                            }
                            case MouseEvent.BUTTON2: {
                                logger.debug("托盘图标被鼠标中键被点击");
                                break;
                            }
                            case MouseEvent.BUTTON3: {
                                logger.debug("托盘图标被鼠标右键被点击");
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
        App.mainFrame.setExtendedState(JFrame.NORMAL);
        App.mainFrame.setVisible(true);
        App.mainFrame.requestFocus();
    }

    public static void shutdown() {
        FrameListener.saveBeforeExit();
        App.sqlSession.close();
        App.mainFrame.dispose();
        System.exit(0);
    }
}
