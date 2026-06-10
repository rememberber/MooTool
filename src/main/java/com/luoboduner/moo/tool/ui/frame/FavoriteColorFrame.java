package com.luoboduner.moo.tool.ui.frame;

import com.luoboduner.moo.tool.ui.form.func.FavoriteColorForm;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * <pre>
 * 调色板收藏夹窗口Frame
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/21.
 */
public class FavoriteColorFrame extends JFrame {
    private static FavoriteColorFrame favoriteColorFrame;

    private static boolean i18nRegistered;

    public FavoriteColorFrame() throws HeadlessException {
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
            I18nUiUtil.register(FavoriteColorFrame::applyI18nStatic);
            i18nRegistered = true;
        }
        FrameUtil.setFrameIcon(this);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.6, 0.6);
    }

    private void applyI18n() {
        String title = I18n.get("favorite.frame.color.title");
        this.setName(title);
        this.setTitle(title);
    }

    private static void applyI18nStatic() {
        if (favoriteColorFrame != null) {
            favoriteColorFrame.applyI18n();
        }
    }

    public static FavoriteColorFrame getInstance() {
        if (favoriteColorFrame == null) {
            favoriteColorFrame = new FavoriteColorFrame();
            favoriteColorFrame.init();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (screenSize.getWidth() <= 1366) {
                // 低分辨率下自动最大化窗口
                favoriteColorFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            favoriteColorFrame.setContentPane(FavoriteColorForm.getInstance().getFavoriteColorPanel());
            favoriteColorFrame.pack();
            FavoriteColorForm.getInstance().init();
        }

        return favoriteColorFrame;
    }

    public static void showWindow() {
        getInstance().setVisible(true);
    }

    public static void exit() {
        getInstance().setVisible(false);
        favoriteColorFrame = null;
        FavoriteColorForm.favoriteColorForm = null;
    }
}
