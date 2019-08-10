package com.luoboduner.moo.tool.ui.frame;

import com.apple.eawt.Application;
import com.google.common.collect.Lists;
import com.luoboduner.moo.tool.ui.UiConsts;
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
        images.add(UiConsts.IMAGE_ICON_LG);
        images.add(UiConsts.IMAGE_ICON_MD);
        images.add(UiConsts.IMAGE_ICON_SM);
        images.add(UiConsts.IMAGE_ICON_XS);
        this.setIconImages(images);
        // Mac系统Dock图标
        if (SystemUtil.isMacOs()) {
            Application application = Application.getApplication();
            application.setDockIconImage(UiConsts.IMAGE_ICON_LG);
            application.setEnabledAboutMenu(false);
            application.setEnabledPreferencesMenu(false);
        }

        ComponentUtil.setPrefersizeAndLocateToCenter(this, 0.8, 0.88);
    }

    /**
     * 添加事件监听
     */
    public void addListeners() {
    }
}
