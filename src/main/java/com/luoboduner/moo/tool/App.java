package com.luoboduner.moo.tool;

import com.luoboduner.moo.tool.ui.Init;
import com.luoboduner.moo.tool.ui.form.LoadingForm;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.frame.MainFrame;
import com.luoboduner.moo.tool.util.ConfigUtil;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.UpgradeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;

import javax.swing.*;
import java.awt.*;

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

    public static SqlSession sqlSession = MybatisUtil.getSqlSession();

    public static SystemTray tray;

    public static TrayIcon trayIcon;

    public static PopupMenu popupMenu;

    public static void main(String[] args) {
        Init.initTheme();
        mainFrame = new MainFrame();
        mainFrame.init();
        JPanel loadingPanel = new LoadingForm().getLoadingPanel();
        mainFrame.add(loadingPanel);
        mainFrame.pack();
        mainFrame.setVisible(true);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (config.isDefaultMaxWindow() || screenSize.getWidth() <= 1366) {
            // 低分辨率下自动最大化窗口
            mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        UpgradeUtil.smoothUpgrade();

        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        Init.initGlobalFont();
        mainFrame.setContentPane(MainWindow.getInstance().getMainPanel());
        if (App.config.getRecentTabIndex() != 3) {
            MainWindow.getInstance().getTabbedPane().setSelectedIndex(App.config.getRecentTabIndex());
        }
        MainWindow.getInstance().init();
        Init.initAllTab();
        Init.initOthers();
        mainFrame.addListeners();
        mainFrame.remove(loadingPanel);
        Init.fontSizeGuide();
    }
}
