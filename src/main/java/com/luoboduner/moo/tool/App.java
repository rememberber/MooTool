package com.luoboduner.moo.tool;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatDesktop;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.util.SystemInfo;
import com.luoboduner.moo.tool.ui.Init;
import com.luoboduner.moo.tool.ui.dialog.AboutDialog;
import com.luoboduner.moo.tool.ui.dialog.SettingDialog;
import com.luoboduner.moo.tool.ui.form.LoadingForm;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.frame.MainFrame;
import com.luoboduner.moo.tool.ui.startup.EdtLagMonitor;
import com.luoboduner.moo.tool.ui.startup.StartupCoordinator;
import com.luoboduner.moo.tool.ui.startup.StartupMetrics;
import com.luoboduner.moo.tool.util.ConfigUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.MacApplicationMenuUtil;
import com.luoboduner.moo.tool.util.TesseractEnvUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.ibatis.session.SqlSession;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.AppReopenedListener;
import java.io.File;

/**
 * <pre>
 * Main Enter!
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/8/10.
 */
@Slf4j
public class App {

    public static ConfigUtil config = ConfigUtil.getInstance();

    public static MainFrame mainFrame;

    /**
     * 延迟初始化，避免类加载阶段打开数据库阻塞首屏。
     */
    public static volatile SqlSession sqlSession;

    public static SystemTray tray;

    public static TrayIcon trayIcon;

    public static JPopupMenu popupMenu;

    public static File tempDir = null;

    public static void main(String[] args) {
        StartupMetrics.mark("app.main.enter");
        I18n.init();
        TesseractEnvUtil.ensureConfigured();
        StartupMetrics.mark("bootstrap.config.ready");

        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "MooTool");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MooTool");
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("flatlaf.useRoundedPopupBorder", "true");

            FlatDesktop.setAboutHandler(() -> {
                try {
                    AboutDialog dialog = new AboutDialog();
                    dialog.pack();
                    dialog.setVisible(true);
                } catch (Exception e2) {
                    log.error(ExceptionUtils.getStackTrace(e2));
                }
            });
            FlatDesktop.setPreferencesHandler(() -> {
                try {
                    SettingDialog dialog = new SettingDialog();
                    dialog.pack();
                    dialog.setVisible(true);
                } catch (Exception e2) {
                    log.error(ExceptionUtils.getStackTrace(e2));
                }
            });
            FlatDesktop.setQuitHandler(FlatDesktop.QuitResponse::performQuit);

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().addAppEventListener((AppReopenedListener) e ->
                        SwingUtilities.invokeLater(Init::showMainFrame));
            }
        }

        FlatLaf.registerCustomDefaultsSource("themes");

        EventQueue.invokeLater(() -> {
            try {
                FlatJetBrainsMonoFont.install();
                Init.initTheme();
                StartupMetrics.mark("laf.ready");

                mainFrame = new MainFrame();
                mainFrame.init();
                mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

                JPanel loadingPanel = new LoadingForm().getLoadingPanel();
                mainFrame.setContentPane(loadingPanel);
                mainFrame.pack();
                mainFrame.setVisible(true);

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                if (config.isDefaultMaxWindow() || screenSize.getWidth() <= 1366) {
                    mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }

                EdtLagMonitor.startIfEnabled();

                // 尽快返回当前 EDT 任务，让首帧与输入事件得到处理。
                EventQueue.invokeLater(() -> {
                    Init.initGlobalFont();
                    MainWindow mainWindow = MainWindow.getInstance();
                    mainFrame.setContentPane(mainWindow.getMainPanel());
                    mainWindow.init();
                    if (SystemInfo.isMacOS) {
                        MacApplicationMenuUtil.installCheckForUpdatesMenu();
                    }
                    StartupCoordinator.getInstance().startAfterWindowVisible();
                });
            } catch (Exception e) {
                log.error("Failed to start UI", e);
            }
        });
    }
}
