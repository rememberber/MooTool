package com.luoboduner.moo.tool.ui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.dialog.FontSizeAdjustDialog;
import com.luoboduner.moo.tool.ui.form.AboutForm;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.SettingForm;
import com.luoboduner.moo.tool.ui.form.func.CryptoForm;
import com.luoboduner.moo.tool.ui.form.func.EnCodeForm;
import com.luoboduner.moo.tool.ui.form.func.HostForm;
import com.luoboduner.moo.tool.ui.form.func.HttpRequestForm;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.ui.form.func.QrCodeForm;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.ui.form.func.TimeConvertForm;
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
                    UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * 初始化所有tab
     */
    public static void initAllTab() {
        SettingForm.init();
        QuickNoteForm.init();
        JsonBeautyForm.init();
        TimeConvertForm.init();
        HostForm.init();
        HttpRequestForm.init();
        AboutForm.init();
        EnCodeForm.init();
        QrCodeForm.init();
        CryptoForm.init();

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
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();

                PopupMenu popupMenu = new PopupMenu();

                MenuItem openItem = new MenuItem("MooTool");
                MenuItem exitItem = new MenuItem("Quit");

                openItem.addActionListener(e -> {
                    App.mainFrame.setExtendedState(JFrame.NORMAL);
                    App.mainFrame.setVisible(true);
                    App.mainFrame.requestFocus();
                });
                exitItem.addActionListener(e -> {
                    FrameListener.saveBeforeExit();
                    App.config.setRecentTabIndex(MainWindow.getInstance().getTabbedPane().getSelectedIndex());
                    App.config.save();
                    App.sqlSession.close();
                    System.exit(0);
                });

                popupMenu.add(openItem);
                popupMenu.add(exitItem);

                TrayIcon trayIcon = new TrayIcon(UiConsts.IMAGE_LOGO_64, "MooTool", popupMenu);
                trayIcon.setImageAutoSize(true);

                trayIcon.addActionListener(e -> {
                    App.mainFrame.setExtendedState(JFrame.NORMAL);
                    App.mainFrame.setVisible(true);
                    App.mainFrame.requestFocus();
                });
                trayIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        switch (e.getButton()) {
                            case MouseEvent.BUTTON1: {
                                App.mainFrame.setExtendedState(JFrame.NORMAL);
                                App.mainFrame.setVisible(true);
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
                    tray.add(trayIcon);
                    trayIcon.displayMessage("MooTool", "MooTool已在系统托盘展示", TrayIcon.MessageType.INFO);
                } catch (AWTException e) {
                    e.printStackTrace();
                    logger.error(ExceptionUtils.getStackTrace(e));
                }

            }

        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }
}
