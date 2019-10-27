package com.luoboduner.moo.tool.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.Init;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.SettingForm;
import com.luoboduner.moo.tool.ui.form.func.CryptoForm;
import com.luoboduner.moo.tool.ui.form.func.HostForm;
import com.luoboduner.moo.tool.ui.form.func.HttpRequestForm;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.ui.form.func.QrCodeForm;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Objects;

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

        settingForm.getHttpSaveButton().addActionListener(e -> {
            try {
                App.config.setHttpUseProxy(settingForm.getHttpUseProxyCheckBox().isSelected());
                App.config.setHttpProxyHost(settingForm.getHttpProxyHostTextField().getText());
                App.config.setHttpProxyPort(settingForm.getHttpProxyPortTextField().getText());
                App.config.setHttpProxyUserName(settingForm.getHttpProxyUserTextField().getText());
                App.config.setHttpProxyPassword(settingForm.getHttpProxyPasswordTextField().getText());
                App.config.save();

//                HttpMsgSender.proxy = null;
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
    }

}
