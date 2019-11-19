package com.luoboduner.moo.tool.ui.frame;

import com.luoboduner.moo.tool.ui.listener.ScreenMouseListener;

import javax.swing.*;
import java.awt.*;

/**
 * 屏幕透明框架
 */
public class ScreenFrame extends JFrame {
    private static ScreenFrame screenFrame;

    private ScreenFrame() {
    }

    public static ScreenFrame getInstance() {
        if (screenFrame == null) {
            screenFrame = new ScreenFrame();
            screenFrame.init();
        }

        return screenFrame;
    }

    private void init() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(false);
        setAutoRequestFocus(false);
        setUndecorated(true);
        setDefaultLookAndFeelDecorated(true);
        setOpacity(0.05f);
        setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, (int) screen.getWidth(), (int) screen.getHeight());
        addMouseListener(new ScreenMouseListener());
        addMouseMotionListener(new ScreenMouseListener());
        setVisible(true);
    }
}
