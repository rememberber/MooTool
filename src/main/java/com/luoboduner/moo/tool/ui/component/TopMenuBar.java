package com.luoboduner.moo.tool.ui.component;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.Init;
import com.luoboduner.moo.tool.ui.dialog.AboutDialog;
import com.luoboduner.moo.tool.ui.dialog.KeyMapDialog;
import com.luoboduner.moo.tool.ui.dialog.SettingDialog;
import com.luoboduner.moo.tool.ui.dialog.SystemEnvResultDialog;
import com.luoboduner.moo.tool.util.SystemUtil;

import javax.swing.*;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

/**
 * 顶部菜单栏
 */
public class TopMenuBar extends JMenuBar {
    private static final Log logger = LogFactory.get();

    private static TopMenuBar menuBar;

    private static JMenu themeMenu;

    private static String[] themeNames = {
            "Darcula",
            "BeautyEye",
            "系统默认",
            "Flat Light",
            "Flat IntelliJ",
            "Flat Dark",
            "Flat Darcula(推荐)",
            "Dark purple",
            "IntelliJ Cyan",
            "IntelliJ Light"};

    private TopMenuBar() {
    }

    public static TopMenuBar getInstance() {
        if (menuBar == null) {
            menuBar = new TopMenuBar();
        }
        return menuBar;
    }

    public void init() {
        TopMenuBar topMenuBar = getInstance();
        // ---------应用
        JMenu appMenu = new JMenu();
        appMenu.setText("应用");
        // 退出
        JMenuItem exitMenuItem = new JMenuItem();
        exitMenuItem.setText("退出");
        exitMenuItem.addActionListener(e -> exitActionPerformed());
        appMenu.add(exitMenuItem);
        topMenuBar.add(appMenu);
        // ---------设置
        JMenu settingMenu = new JMenu();
        settingMenu.setText("设置");
        settingMenu.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                settingActionPerformed();
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        topMenuBar.add(settingMenu);
        // ---------外观
        JMenu appearanceMenu = new JMenu();
        appearanceMenu.setText("外观");

        themeMenu = new JMenu();
        themeMenu.setText("主题风格");

        initThemesMenu();

        appearanceMenu.add(themeMenu);

        JMenu fontFamilyMenu = new JMenu();
        fontFamilyMenu.setText("字体");

        appearanceMenu.add(fontFamilyMenu);

        JMenu fontSizeMenu = new JMenu();
        fontSizeMenu.setText("字号");

        appearanceMenu.add(fontSizeMenu);

        topMenuBar.add(appearanceMenu);
        // ---------快捷键
        JMenu keyMapMenu = new JMenu();
        keyMapMenu.setText("快捷键");
        keyMapMenu.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                keyMapActionPerformed();
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        topMenuBar.add(keyMapMenu);
        // ---------调试
        JMenu debugMenu = new JMenu();
        debugMenu.setText("调试");
        // 查看日志
        JMenuItem logMenuItem = new JMenuItem();
        logMenuItem.setText("查看日志");
        logMenuItem.addActionListener(e -> logActionPerformed());

        debugMenu.add(logMenuItem);
        // 系统环境变量
        JMenuItem sysEnvMenuItem = new JMenuItem();
        sysEnvMenuItem.setText("系统环境变量");
        sysEnvMenuItem.addActionListener(e -> sysEnvActionPerformed());

        debugMenu.add(sysEnvMenuItem);

        topMenuBar.add(debugMenu);
        // ---------关于
        JMenu aboutMenu = new JMenu();
        aboutMenu.setText("关于");
        aboutMenu.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                aboutActionPerformed();
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        topMenuBar.add(aboutMenu);
    }

    private void initThemesMenu() {
        int itemCount = -1;
        if (itemCount < 0)
            itemCount = themeMenu.getItemCount();
        else {
            // remove old font items
            for (int i = themeMenu.getItemCount() - 1; i >= itemCount; i--)
                themeMenu.remove(i);
        }
        for (String themeName : themeNames) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(themeName);
            item.setSelected(themeName.equals(App.config.getTheme()));
            item.addActionListener(this::themeChanged);
            themeMenu.add(item);
        }
    }

    private void themeChanged(ActionEvent actionEvent) {
        String selectedThemeName = actionEvent.getActionCommand();

        FlatAnimatedLafChange.showSnapshot();

        App.config.setTheme(selectedThemeName);
        App.config.save();

        FlatAnimatedLafChange.hideSnapshotWithAnimation();

        initThemesMenu();
    }

    private void keyMapActionPerformed() {
        try {
            KeyMapDialog dialog = new KeyMapDialog();

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error(e2);
        }
    }

    private void aboutActionPerformed() {
        try {
            AboutDialog dialog = new AboutDialog();

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error(e2);
        }
    }

    private void sysEnvActionPerformed() {
        try {
            SystemEnvResultDialog dialog = new SystemEnvResultDialog();

            dialog.appendTextArea("------------System.getenv---------------");
            Map<String, String> map = System.getenv();
            for (Map.Entry<String, String> envEntry : map.entrySet()) {
                dialog.appendTextArea(envEntry.getKey() + "=" + envEntry.getValue());
            }

            dialog.appendTextArea("------------System.getProperties---------------");
            Properties properties = System.getProperties();
            for (Map.Entry<Object, Object> objectObjectEntry : properties.entrySet()) {
                dialog.appendTextArea(objectObjectEntry.getKey() + "=" + objectObjectEntry.getValue());
            }

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error("查看系统环境变量失败", e2);
        }
    }

    private void logActionPerformed() {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.open(new File(SystemUtil.LOG_DIR));
        } catch (Exception e2) {
            logger.error("查看日志打开失败", e2);
        }
    }

    private void exitActionPerformed() {
        Init.shutdown();
    }

    private void settingActionPerformed() {
        try {
            SettingDialog dialog = new SettingDialog();

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error(e2);
        }
    }
}
