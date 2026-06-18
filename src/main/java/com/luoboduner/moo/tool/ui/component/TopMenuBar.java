package com.luoboduner.moo.tool.ui.component;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.jthemedetecor.OsThemeDetector;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.Init;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteRSyntaxTextViewer;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteRSyntaxTextViewerManager;
import org.fife.ui.rtextarea.RTextScrollPane;
import com.luoboduner.moo.tool.ui.dialog.*;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.*;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.MacApplicationMenuUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import com.luoboduner.moo.tool.util.UpgradeUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * 顶部菜单栏
 */
public class TopMenuBar extends JMenuBar {
    private static final Log logger = LogFactory.get();

    private static TopMenuBar menuBar;

    private static JMenu themeMenu;

    private static JMenu fontFamilyMenu;

    private static JMenu fontSizeMenu;

    private static JMenu appMenu;
    private static JMenu appearanceMenu;
    private static JMenu aboutMenu;
    private static JMenu supportMeMenu;

    private static JMenuItem settingMenuItem;
    private static JMenuItem syncAndBackupMenuItem;
    private static JMenuItem keyMapMenuItem;
    private static JMenuItem funcNavigatorMenuItem;
    private static JMenuItem logMenuItem;
    private static JMenuItem sysEnvMenuItem;
    private static JMenuItem exitMenuItem;
    private static JCheckBoxMenuItem defaultMaxWindowItem;
    private static JCheckBoxMenuItem unifiedBackgroundItem;
    private static JCheckBoxMenuItem systemColorItem;
    private static JMenuItem checkUpdateMenuItem;
    private static JMenuItem aboutMenuItem;
    private static JMenuItem supportMeMenuItem;

    private static int initialThemeItemCount = -1;

    private static int initialFontFamilyItemCount = -1;

    private static int initialFontSizeItemCount = -1;

    private static String[] themeNames = {
            "系统默认",
            "Flat Light",
            "Flat IntelliJ",
            "Flat Dark",
            "Flat Darcula",
            "Flat macOS Light",
            "Flat macOS Dark",
            "Dark purple",
            "IntelliJ Cyan",
            "IntelliJ Light",
            "Monocai",
            "Monokai Pro",
            "One Dark",
            "Gray",
            "High contrast",
            "GitHub Dark",
            "Xcode-Dark",
            "Vuesion"
    };

    public static String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

    private static String[] fontSizes = {
            "5",
            "6",
            "7",
            "8",
            "9",
            "10",
            "11",
            "12",
            "13",
            "14",
            "15",
            "16",
            "17",
            "18",
            "19",
            "20",
            "21",
            "22",
            "23",
            "24",
            "25",
            "26"};

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
        topMenuBar.removeAll();

        boolean isMac = SystemUtil.isMacOs();

        appMenu = new JMenu();
        settingMenuItem = new JMenuItem();
        settingMenuItem.addActionListener(e -> settingActionPerformed());
        if (!isMac) {
            appMenu.add(settingMenuItem);
        }
        syncAndBackupMenuItem = new JMenuItem();
        syncAndBackupMenuItem.addActionListener(e -> syncAndBackupActionPerformed());
        appMenu.add(syncAndBackupMenuItem);
        keyMapMenuItem = new JMenuItem();
        keyMapMenuItem.addActionListener(e -> keyMapActionPerformed());
        appMenu.add(keyMapMenuItem);
        funcNavigatorMenuItem = new JMenuItem();
        funcNavigatorMenuItem.addActionListener(e -> FuncNavigatorDialog.showDialog());
        refreshFuncNavigatorMenuVisibility();
        logMenuItem = new JMenuItem();
        logMenuItem.addActionListener(e -> logActionPerformed());
        appMenu.add(logMenuItem);
        sysEnvMenuItem = new JMenuItem();
        sysEnvMenuItem.addActionListener(e -> sysEnvActionPerformed());
        appMenu.add(sysEnvMenuItem);
        exitMenuItem = new JMenuItem();
        exitMenuItem.addActionListener(e -> exitActionPerformed());
        if (!isMac) {
            appMenu.add(exitMenuItem);
        }
        topMenuBar.add(appMenu);

        appearanceMenu = new JMenu();

        defaultMaxWindowItem = new JCheckBoxMenuItem();
        defaultMaxWindowItem.setSelected(App.config.isDefaultMaxWindow());
        defaultMaxWindowItem.addActionListener(e -> {
            boolean selected = defaultMaxWindowItem.isSelected();
            if (selected) {
                App.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else {
                App.mainFrame.setExtendedState(JFrame.NORMAL);
            }
            App.config.setDefaultMaxWindow(selected);
            App.config.save();
        });
        appearanceMenu.add(defaultMaxWindowItem);

        unifiedBackgroundItem = new JCheckBoxMenuItem();
        unifiedBackgroundItem.setSelected(App.config.isUnifiedBackground());
        unifiedBackgroundItem.addActionListener(e -> {
            boolean selected = unifiedBackgroundItem.isSelected();
            App.config.setUnifiedBackground(selected);
            App.config.save();
            UIManager.put("TitlePane.unifiedBackground", selected);
            Init.refreshFlatLafUi();
        });
        appearanceMenu.add(unifiedBackgroundItem);

        systemColorItem = new JCheckBoxMenuItem();
        systemColorItem.setSelected(App.config.isThemeColorFollowSystem());
        systemColorItem.addActionListener(e -> {
            boolean selected = systemColorItem.isSelected();
            App.config.setThemeColorFollowSystem(selected);
            App.config.save();
            if (selected) {
                final OsThemeDetector detector = OsThemeDetector.getDetector();
                final boolean isDarkThemeUsed = detector.isDark();
                if (isDarkThemeUsed) {
                    changeTheme("Flat macOS Dark");
                } else {
                    changeTheme("Flat macOS Light");
                }
            }
        });
        appearanceMenu.add(systemColorItem);

        themeMenu = new JMenu();
        initThemesMenu();
        appearanceMenu.add(themeMenu);

        fontFamilyMenu = new JMenu();
        fontFamilyMenu.setAutoscrolls(true);
        initFontFamilyMenu();
        appearanceMenu.add(fontFamilyMenu);

        fontSizeMenu = new JMenu();
        initFontSizeMenu();
        appearanceMenu.add(fontSizeMenu);

        topMenuBar.add(appearanceMenu);

        aboutMenu = new JMenu();
        checkUpdateMenuItem = new JMenuItem();
        checkUpdateMenuItem.addActionListener(e -> checkUpdateActionPerformed());
        aboutMenuItem = new JMenuItem();
        aboutMenuItem.addActionListener(e -> aboutActionPerformed());
        if (!SystemUtil.isMacOs()) {
            aboutMenu.add(checkUpdateMenuItem);
            aboutMenu.add(aboutMenuItem);
            topMenuBar.add(aboutMenu);
        }

        supportMeMenu = new JMenu();
        supportMeMenuItem = new JMenuItem();
        supportMeMenuItem.addActionListener(e -> supportMeActionPerformed());
        supportMeMenu.add(supportMeMenuItem);
        topMenuBar.add(supportMeMenu);

        refreshTexts();

        final OsThemeDetector detector = OsThemeDetector.getDetector();
        detector.registerListener(isDark -> SwingUtilities.invokeLater(() -> {
            if (App.config.isThemeColorFollowSystem()) {
                if (isDark) {
                    changeTheme("Flat macOS Dark");
                } else {
                    changeTheme("Flat macOS Light");
                }
            }
        }));
    }

    public static void refreshFuncNavigatorMenuVisibility() {
        if (appMenu == null || funcNavigatorMenuItem == null || keyMapMenuItem == null) {
            return;
        }
        boolean grouped = App.config.isFuncTabGrouped();
        boolean inMenu = funcNavigatorMenuItem.getParent() == appMenu;
        if (grouped && !inMenu) {
            int index = 0;
            for (int i = 0; i < appMenu.getMenuComponentCount(); i++) {
                if (appMenu.getMenuComponent(i) == keyMapMenuItem) {
                    index = i + 1;
                    break;
                }
            }
            appMenu.insert(funcNavigatorMenuItem, index);
        } else if (!grouped && inMenu) {
            appMenu.remove(funcNavigatorMenuItem);
        }
        if (grouped) {
            funcNavigatorMenuItem.setText(I18n.get("menu.funcNavigator"));
        }
    }

    public void refreshTexts() {
        if (appMenu == null) {
            return;
        }
        appMenu.setText(I18n.get("menu.app"));
        settingMenuItem.setText(I18n.get("menu.settings"));
        syncAndBackupMenuItem.setText(I18n.get("menu.syncBackup"));
        keyMapMenuItem.setText(I18n.get("menu.keymap"));
        if (funcNavigatorMenuItem != null) {
            funcNavigatorMenuItem.setText(I18n.get("menu.funcNavigator"));
        }
        logMenuItem.setText(I18n.get("menu.viewLog"));
        sysEnvMenuItem.setText(I18n.get("menu.systemEnv"));
        exitMenuItem.setText(I18n.get("menu.exit"));
        appearanceMenu.setText(I18n.get("menu.appearance"));
        defaultMaxWindowItem.setText(I18n.get("menu.defaultMaxWindow"));
        unifiedBackgroundItem.setText(I18n.get("menu.unifiedBackground"));
        systemColorItem.setText(I18n.get("menu.themeFollowSystem"));
        themeMenu.setText(I18n.get("menu.theme"));
        fontFamilyMenu.setText(I18n.get("menu.font"));
        fontSizeMenu.setText(I18n.get("menu.fontSize"));
        checkUpdateMenuItem.setText(I18n.get("menu.checkUpdate"));
        if (!SystemUtil.isMacOs()) {
            aboutMenu.setText(I18n.get("menu.about"));
            aboutMenuItem.setText(I18n.get("menu.about"));
        } else {
            MacApplicationMenuUtil.refreshCheckForUpdatesMenuTitle();
        }
        supportMeMenu.setText(I18n.get("menu.support"));
        supportMeMenuItem.setText(I18n.get("menu.supportMe"));
        initThemesMenu();
    }

    private void supportMeActionPerformed() {
        try {
            SupportMeDialog dialog = new SupportMeDialog();

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error(e2);
        }
    }

    private void syncAndBackupActionPerformed() {
        try {
            SyncAndBackupDialog dialog = new SyncAndBackupDialog();

            dialog.pack();
            dialog.setVisible(true);
        } catch (Exception e2) {
            logger.error(e2);
        }
    }

    private void checkUpdateActionPerformed() {
        UpgradeUtil.checkUpdate(false);
    }

    private void initFontSizeMenu() {

        if (initialFontSizeItemCount < 0)
            initialFontSizeItemCount = fontSizeMenu.getItemCount();
        else {
            // remove old items
            for (int i = fontSizeMenu.getItemCount() - 1; i >= initialFontSizeItemCount; i--)
                fontSizeMenu.remove(i);
        }
        for (String fontSize : fontSizes) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(fontSize);
            item.setSelected(fontSize.equals(String.valueOf(App.config.getFontSize())));
            item.addActionListener(this::fontSizeChanged);
            fontSizeMenu.add(item);
        }
    }

    private void initFontFamilyMenu() {

        if (initialFontFamilyItemCount < 0)
            initialFontFamilyItemCount = fontFamilyMenu.getItemCount();
        else {
            // remove old items
            for (int i = fontFamilyMenu.getItemCount() - 1; i >= initialFontFamilyItemCount; i--)
                fontFamilyMenu.remove(i);
        }
        for (String font : fontNames) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(font);
            item.setSelected(font.equals(App.config.getFont()));
            item.addActionListener(this::fontFamilyChanged);
            fontFamilyMenu.add(item);
        }
    }

    private void initThemesMenu() {

        if (initialThemeItemCount < 0)
            initialThemeItemCount = themeMenu.getItemCount();
        else {
            // remove old items
            for (int i = themeMenu.getItemCount() - 1; i >= initialThemeItemCount; i--)
                themeMenu.remove(i);
        }
        for (String themeName : themeNames) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(displayThemeName(themeName));
            item.setActionCommand(themeName);
            item.setSelected(themeName.equals(App.config.getTheme()));
            item.addActionListener(this::themeChanged);
            themeMenu.add(item);
        }
    }

    private void fontSizeChanged(ActionEvent actionEvent) {
        try {
            String selectedFontSize = actionEvent.getActionCommand();

            FlatAnimatedLafChange.showSnapshot();

            App.config.setFontSize(Integer.parseInt(selectedFontSize));
            App.config.save();

            Init.initGlobalFont();
            SwingUtilities.updateComponentTreeUI(App.mainFrame);
            SwingUtilities.updateComponentTreeUI(MainWindow.getInstance().getTabbedPane());

//                FlatLaf.updateUI();

            FlatAnimatedLafChange.hideSnapshotWithAnimation();

//            JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "部分细节重启应用后生效！\n\n", "成功",
//                    JOptionPane.INFORMATION_MESSAGE);

            initFontSizeMenu();

        } catch (Exception e1) {
            JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(),
                    I18n.format("common.saveFailed", e1.getMessage()), I18n.get("common.failure"),
                    JOptionPane.ERROR_MESSAGE);
            logger.error(e1);
        }
    }


    public void fontFamilyChanged(ActionEvent actionEvent) {
        try {
            String selectedFamily = actionEvent.getActionCommand();

            FlatAnimatedLafChange.showSnapshot();

            App.config.setFont(selectedFamily);
            App.config.save();

            Init.initGlobalFont();
            SwingUtilities.updateComponentTreeUI(App.mainFrame);
            SwingUtilities.updateComponentTreeUI(MainWindow.getInstance().getTabbedPane());

//                FlatLaf.updateUI();

            FlatAnimatedLafChange.hideSnapshotWithAnimation();

//            JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "部分细节重启应用后生效！\n\n", "成功",
//                    JOptionPane.INFORMATION_MESSAGE);

            initFontFamilyMenu();

        } catch (Exception e1) {
            JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(),
                    I18n.format("common.saveFailed", e1.getMessage()), I18n.get("common.failure"),
                    JOptionPane.ERROR_MESSAGE);
            logger.error(e1);
        }
    }

    private void themeChanged(ActionEvent actionEvent) {
        try {
            String selectedThemeName = actionEvent.getActionCommand();

            changeTheme(selectedThemeName);

//            JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "部分细节重启应用后生效！\n\n", "成功",
//                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e1) {
            JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(),
                    I18n.format("common.saveFailed", e1.getMessage()), I18n.get("common.failure"),
                    JOptionPane.ERROR_MESSAGE);
            logger.error(e1);
        }
    }

    private static String displayThemeName(String themeName) {
        if ("系统默认".equals(themeName)) {
            return I18n.get("theme.systemDefault");
        }
        return themeName;
    }

    private void changeTheme(String selectedThemeName) {
        FlatAnimatedLafChange.showSnapshot();

        App.config.setTheme(selectedThemeName);
        App.config.save();

        Init.initTheme();

        if (FlatLaf.isLafDark()) {
            FlatSVGIcon.ColorFilter.getInstance().setMapper(color -> color.brighter().brighter());
        }

        SwingUtilities.updateComponentTreeUI(App.mainFrame);
        SwingUtilities.updateComponentTreeUI(MainWindow.getInstance().getTabbedPane());
        MainWindow.getInstance().initTabPlacement();

//                FlatLaf.updateUI();

        FlatAnimatedLafChange.hideSnapshotWithAnimation();

        initThemesMenu();

        QuickNoteRSyntaxTextViewerManager.viewMap.forEach((name, editorPanel) -> {
            RTextScrollPane rTextScrollPane = editorPanel.getEditorScrollPane();
            ((QuickNoteRSyntaxTextViewer) rTextScrollPane.getTextArea()).updateTheme();
            QuickNoteRSyntaxTextViewerManager.updateGutter(rTextScrollPane);
            rTextScrollPane.updateUI();
            editorPanel.updatePreviewTheme();
        });

        JsonBeautyForm.getInstance().getTextArea().updateTheme();
        JsonBeautyForm.getInstance().getScrollPane().updateTheme();
        JsonBeautyForm.initTextAreaFont();
        JsonBeautyForm.getInstance().getScrollPane().updateUI();

        HostForm.getInstance().getTextArea().updateTheme();
        HostForm.getInstance().getScrollPane().updateTheme();
        HostForm.getInstance().getScrollPane().updateUI();

        RegexForm.getInstance().getTextArea().updateTheme();
        RegexForm.getInstance().getScrollPane().updateTheme();
        RegexForm.getInstance().getScrollPane().updateUI();

        JavaConsoleForm.getInstance().getTextArea().updateTheme();
        JavaConsoleForm.getInstance().getScrollPane().updateTheme();
        JavaConsoleForm.getInstance().getScrollPane().updateUI();

        TextDiffForm.getInstance().getLeftTextArea().updateTheme();
        TextDiffForm.getInstance().getRightTextArea().updateTheme();

        ColorBoardForm.updateTheme();

        SwingUtilities.updateComponentTreeUI(App.popupMenu);
        App.popupMenu.updateUI();
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
