package com.luoboduner.moo.tool.ui.frame;

import com.apple.eawt.Application;
import com.google.common.collect.Lists;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.listener.AboutListener;
import com.luoboduner.moo.tool.ui.listener.FrameListener;
import com.luoboduner.moo.tool.ui.listener.TabListener;
import com.luoboduner.moo.tool.ui.listener.func.CryptoListener;
import com.luoboduner.moo.tool.ui.listener.func.EnCodeListener;
import com.luoboduner.moo.tool.ui.listener.func.HostListener;
import com.luoboduner.moo.tool.ui.listener.func.HttpRequestListener;
import com.luoboduner.moo.tool.ui.listener.func.JsonBeautyListener;
import com.luoboduner.moo.tool.ui.listener.func.QrCodeListener;
import com.luoboduner.moo.tool.ui.listener.func.QuickNoteListener;
import com.luoboduner.moo.tool.ui.listener.SettingListener;
import com.luoboduner.moo.tool.ui.listener.func.TimeConvertListener;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.SystemUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * <pre>
 * 主窗口
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/8/10.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = -332963894416012132L;

    public void init() {
        this.setName(UiConsts.APP_NAME);
        this.setTitle(UiConsts.APP_NAME);
        List<Image> images = Lists.newArrayList();
        images.add(UiConsts.IMAGE_LOGO_1024);
        images.add(UiConsts.IMAGE_LOGO_512);
        images.add(UiConsts.IMAGE_LOGO_256);
        images.add(UiConsts.IMAGE_LOGO_128);
        images.add(UiConsts.IMAGE_LOGO_64);
        images.add(UiConsts.IMAGE_LOGO_48);
        images.add(UiConsts.IMAGE_LOGO_32);
        images.add(UiConsts.IMAGE_LOGO_24);
        images.add(UiConsts.IMAGE_LOGO_16);
        this.setIconImages(images);
        // Mac系统Dock图标
        if (SystemUtil.isMacOs()) {
            Application application = Application.getApplication();
            application.setDockIconImage(UiConsts.IMAGE_LOGO_512);
            application.setEnabledAboutMenu(false);
            application.setEnabledPreferencesMenu(false);
        }

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.8, 0.88);
    }

    /**
     * 添加事件监听
     */
    public void addListeners() {
        FrameListener.addListeners();
        AboutListener.addListeners();
        QuickNoteListener.addListeners();
        TimeConvertListener.addListeners();
        JsonBeautyListener.addListeners();
        HostListener.addListeners();
        HttpRequestListener.addListeners();
        QrCodeListener.addListeners();
        EnCodeListener.addListeners();
        CryptoListener.addListeners();
        SettingListener.addListeners();
        TabListener.addListeners();
    }
}
