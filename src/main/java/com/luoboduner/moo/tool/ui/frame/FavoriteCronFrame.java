package com.luoboduner.moo.tool.ui.frame;

import com.luoboduner.moo.tool.ui.form.func.FavoriteCronForm;
import com.luoboduner.moo.tool.util.ComponentUtil;

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
        String title = "Cron表达式-收藏夹";
        this.setName(title);
        this.setTitle(title);
        FrameUtil.setFrameIcon(this);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.6, 0.6);
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
