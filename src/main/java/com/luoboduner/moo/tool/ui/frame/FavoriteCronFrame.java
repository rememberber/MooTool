package com.luoboduner.moo.tool.ui.frame;

import com.luoboduner.moo.tool.ui.form.func.FavoriteCronForm;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * <pre>
 * Cron表达式收藏夹窗口Frame
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2023/9/5
 */
public class FavoriteCronFrame extends JFrame {
    private static FavoriteCronFrame favoriteCronFrame;

    private static boolean i18nRegistered;

    public FavoriteCronFrame() throws HeadlessException {
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
            I18nUiUtil.register(FavoriteCronFrame::applyI18nStatic);
            i18nRegistered = true;
        }
        FrameUtil.setFrameIcon(this);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.6, 0.6);
    }

    private void applyI18n() {
        String title = I18n.get("favorite.frame.cron.title");
        this.setName(title);
        this.setTitle(title);
    }

    private static void applyI18nStatic() {
        if (favoriteCronFrame != null) {
            favoriteCronFrame.applyI18n();
        }
    }

    public static FavoriteCronFrame getInstance() {
        if (favoriteCronFrame == null) {
            favoriteCronFrame = new FavoriteCronFrame();
            favoriteCronFrame.init();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (screenSize.getWidth() <= 1366) {
                // 低分辨率下自动最大化窗口
                favoriteCronFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            favoriteCronFrame.setContentPane(FavoriteCronForm.getInstance().getFavoriteCronPanel());
            favoriteCronFrame.pack();
            FavoriteCronForm.getInstance().init();
        }

        return favoriteCronFrame;
    }

    public static void showWindow() {
        getInstance().setVisible(true);
    }

    public static void exit() {
        getInstance().setVisible(false);
        favoriteCronFrame = null;
        FavoriteCronForm.favoriteCronForm = null;
    }
}
