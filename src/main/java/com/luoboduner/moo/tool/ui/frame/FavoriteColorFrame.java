package com.luoboduner.moo.tool.ui.frame;

import com.luoboduner.moo.tool.ui.form.func.FavoriteColorForm;
import com.luoboduner.moo.tool.util.ComponentUtil;

import javax.swing.*;
import java.awt.*;

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

    public void init() {
        String title = "调色板-收藏夹";
        this.setName(title);
        this.setTitle(title);
        FrameUtil.setFrameIcon(this);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.72, 0.68);
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
        }

        return favoriteColorFrame;
    }

    public static void showWindow() {
        getInstance().setVisible(true);
    }
}
