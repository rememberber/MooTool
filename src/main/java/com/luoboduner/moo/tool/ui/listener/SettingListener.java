package com.luoboduner.moo.tool.ui.listener;

import cn.hutool.core.io.FileUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.FlatLaf;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.service.HttpMsgSender;
import com.luoboduner.moo.tool.ui.Init;
import com.luoboduner.moo.tool.ui.dialog.SystemEnvResultDialog;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.SettingForm;
import com.luoboduner.moo.tool.ui.form.func.CryptoForm;
import com.luoboduner.moo.tool.ui.form.func.HostForm;
import com.luoboduner.moo.tool.ui.form.func.HttpRequestForm;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.ui.form.func.QrCodeForm;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * <pre>
 * 设置tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/16.
 */
public class SettingListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        SettingForm settingForm = SettingForm.getInstance();
        JPanel settingPanel = settingForm.getSettingPanel();

        // 设置-常规-启动时自动检查更新
        settingForm.getAutoCheckUpdateCheckBox().addActionListener(e -> {
            App.config.setAutoCheckUpdate(settingForm.getAutoCheckUpdateCheckBox().isSelected());
            App.config.save();
        });

        // 设置-常规-默认最大化窗口
        settingForm.getDefaultMaxWindowCheckBox().addActionListener(e -> {
            App.config.setDefaultMaxWindow(settingForm.getDefaultMaxWindowCheckBox().isSelected());
            App.config.save();
        });

        // 设置-常规-窗口颜色沉浸式
        settingForm.getUnifiedBackgroundCheckBox().addActionListener(e -> {
            boolean selected = settingForm.getUnifiedBackgroundCheckBox().isSelected();
            App.config.setUnifiedBackground(selected);
            App.config.save();
            UIManager.put("TitlePane.unifiedBackground", selected);
            FlatLaf.updateUI();
        });

        settingForm.getHttpSaveButton().addActionListener(e -> {
            try {
                App.config.setHttpUseProxy(settingForm.getHttpUseProxyCheckBox().isSelected());
                App.config.setHttpProxyHost(settingForm.getHttpProxyHostTextField().getText());
                App.config.setHttpProxyPort(settingForm.getHttpProxyPortTextField().getText());
                App.config.setHttpProxyUserName(settingForm.getHttpProxyUserTextField().getText());
                App.config.setHttpProxyPassword(settingForm.getHttpProxyPasswordTextField().getText());
                App.config.save();

                HttpMsgSender.proxy = null;
                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 外观-保存
        settingForm.getSettingAppearanceSaveButton().addActionListener(e -> {
            try {
                App.config.setTheme(Objects.requireNonNull(settingForm.getSettingThemeComboBox().getSelectedItem()).toString());
                App.config.setFont(Objects.requireNonNull(settingForm.getSettingFontNameComboBox().getSelectedItem()).toString());
                App.config.setFontSize(Integer.parseInt(Objects.requireNonNull(settingForm.getSettingFontSizeComboBox().getSelectedItem()).toString()));
                App.config.save();

                Init.initTheme();
                Init.initGlobalFont();
                SwingUtilities.updateComponentTreeUI(App.mainFrame);
                SwingUtilities.updateComponentTreeUI(MainWindow.getInstance().getTabbedPane());

                JOptionPane.showMessageDialog(settingPanel, "保存成功！\n\n重启应用生效！\n\n", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        settingForm.getHttpUseProxyCheckBox().addChangeListener(e -> SettingForm.toggleHttpProxyPanel());

        // 使用习惯-菜单栏位置
        settingForm.getMenuBarPositionComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                App.config.setMenuBarPosition(e.getItem().toString());
                App.config.save();
                QuickNoteForm.init();
                JsonBeautyForm.init();
                HostForm.init();
                HttpRequestForm.init();
                QrCodeForm.init();
                CryptoForm.init();
            }
        });

        // 高级 数据文件路径设置
        settingForm.getDbFilePathSaveButton().addActionListener(e -> {
            try {
                String dbFilePath = settingForm.getDbFilePathTextField().getText();

                // 复制之前的数据文件到新位置
                String dbFilePathBefore = App.config.getDbFilePathBefore();
                if (dbFilePathBefore.equals(dbFilePath)) {
                    JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (StringUtils.isBlank(dbFilePathBefore)) {
                    dbFilePathBefore = SystemUtil.CONFIG_HOME;
                }
                if (StringUtils.isNotBlank(dbFilePath)) {
                    FileUtil.copy(dbFilePathBefore + File.separator + "MooTool.db", dbFilePath, false);
                }

                MybatisUtil.setSqlSession(null);

                App.config.setDbFilePath(dbFilePath);
                App.config.setDbFilePathBefore(dbFilePath);
                App.config.save();
                JOptionPane.showMessageDialog(settingPanel, "保存成功！\n\n需要重启MooTool生效", "成功", JOptionPane.INFORMATION_MESSAGE);
                JOptionPane.showMessageDialog(settingPanel, "MooTool即将关闭！\n\n关闭后需要手动再次打开", "MooTool即将关闭", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        settingForm.getDbFilePathExploreButton().addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(settingForm.getDbFilePathTextField().getText());
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int approve = fileChooser.showOpenDialog(settingForm.getSettingPanel());
            String dbFilePath;
            if (approve == JFileChooser.APPROVE_OPTION) {
                dbFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                settingForm.getDbFilePathTextField().setText(dbFilePath);
            }
        });
    }

}
