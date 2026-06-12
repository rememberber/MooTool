package com.luoboduner.moo.tool.ui.frame;

import com.luoboduner.moo.tool.ui.form.func.FavoriteRegexForm;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * <pre>
 * 正则表达式收藏夹窗口Frame
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2022/03/14.
 */
public class FavoriteRegexFrame extends JFrame {
    private static FavoriteRegexFrame favoriteRegexFrame;

    private static boolean i18nRegistered;

    public FavoriteRegexFrame() throws HeadlessException {
        addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }
        });
    }

    public void init() {
        applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(FavoriteRegexFrame::applyI18nStatic);
            i18nRegistered = true;
        }
        FrameUtil.setFrameIcon(this);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.6, 0.6);
    }

    private void applyI18n() {
        String title = I18n.get("favorite.frame.regex.title");
        this.setName(title);
        this.setTitle(title);
    }

    private static void applyI18nStatic() {
        if (favoriteRegexFrame != null) {
            favoriteRegexFrame.applyI18n();
        }
    }

    public static FavoriteRegexFrame getInstance() {
        if (favoriteRegexFrame == null) {
            favoriteRegexFrame = new FavoriteRegexFrame();
            favoriteRegexFrame.init();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (screenSize.getWidth() <= 1366) {
                // 低分辨率下自动最大化窗口
                favoriteRegexFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            favoriteRegexFrame.setContentPane(FavoriteRegexForm.getInstance().getFavoriteRegexPanel());
            favoriteRegexFrame.pack();
            FavoriteRegexForm.getInstance().init();
        }

        return favoriteRegexFrame;
    }

    public static void showWindow() {
        getInstance().setVisible(true);
    }

    public static void exit() {
        getInstance().setVisible(false);
        favoriteRegexFrame = null;
        FavoriteRegexForm.favoriteRegexForm = null;
    }
}
