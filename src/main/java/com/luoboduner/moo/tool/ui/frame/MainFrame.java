package com.luoboduner.moo.tool.ui.frame;

import com.formdev.flatlaf.util.SystemInfo;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.component.TopMenuBar;
import com.luoboduner.moo.tool.ui.listener.FrameListener;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.SystemUtil;

import javax.swing.*;

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

    public static TopMenuBar topMenuBar;

    public void init() {
        this.setName(UiConsts.APP_NAME);
        this.setTitle(UiConsts.APP_NAME);
        FrameUtil.setFrameIcon(this);
        // Mac系统Dock图标
        if (SystemUtil.isMacOs()) {
//            Application application = Application.getApplication();
//            application.setDockIconImage(UiConsts.IMAGE_LOGO_512);
//            if (!SystemUtil.isMacM1()) {
//                application.setEnabledAboutMenu(false);
//                application.setEnabledPreferencesMenu(false);
//            }
            if( SystemInfo.isMacFullWindowContentSupported ) {
                this.getRootPane().putClientProperty( "apple.awt.fullWindowContent", true );
                this.getRootPane().putClientProperty( "apple.awt.transparentTitleBar", true );
                this.getRootPane().putClientProperty( "apple.awt.fullscreenable", true );
                this.getRootPane().putClientProperty( "apple.awt.windowTitleVisible", false );
            }
        }
        topMenuBar = TopMenuBar.getInstance();
        topMenuBar.init();
        setJMenuBar(topMenuBar);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.8, 0.88);
        FrameListener.addListeners();

    }

}
