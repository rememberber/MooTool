package com.luoboduner.moo.tool.ui.frame;

import com.apple.eawt.Application;
import com.google.common.collect.Lists;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.form.func.ColorBoardForm;
import com.luoboduner.moo.tool.ui.form.func.ColorPickerForm;
import com.luoboduner.moo.tool.util.ColorUtil;
import com.luoboduner.moo.tool.util.SystemUtil;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;

/**
 * <pre>
 * ColorPickerFrame
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/19.
 */
public class ColorPickerFrame extends JFrame {

    private static ColorPickerFrame colorPickerFrame;

    private ColorPickerFrame() {
    }

    public static ColorPickerFrame getInstance() {
        if (colorPickerFrame == null) {
            colorPickerFrame = new ColorPickerFrame();
            colorPickerFrame.init();
        }
        return colorPickerFrame;
    }

    public static void showPicker() {
        ColorPickerFrame.getInstance().setVisible(true);
        ColorPickerFrame.ScreenFrame.getInstance().setVisible(true);
    }

    private void init() {
        setDefaultLookAndFeelDecorated(true);
        setUndecorated(true);
        setAutoRequestFocus(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new GridBagLayout());
        setAlwaysOnTop(true);

        setName(UiConsts.APP_NAME);
        setTitle(UiConsts.APP_NAME);
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
        setIconImages(images);
        // Mac系统Dock图标
        if (SystemUtil.isMacOs()) {
            Application application = Application.getApplication();
            application.setDockIconImage(UiConsts.IMAGE_LOGO_512);
            application.setEnabledAboutMenu(false);
            application.setEnabledPreferencesMenu(false);
        }

        addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                getInstance().setVisible(false);
                ScreenFrame.getInstance().setVisible(false);
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });

        setContentPane(ColorPickerForm.getInstance().getColorPickerPanel());
        pack();
        setVisible(true);

        ScreenFrame.getInstance().setVisible(true);
    }

    private static class ScreenMouseListener implements MouseInputListener {
        private Robot robot;

        @Override
        public void mouseClicked(MouseEvent e) {
            try {
                robot = new Robot();
            } catch (AWTException eg) {
                eg.printStackTrace();
            }
            Point point = e.getLocationOnScreen();
            int x = point.x;
            int y = point.y;
            ScreenFrame.getInstance().setVisible(false);
            Color color = robot.getPixelColor(x, y);

            ColorBoardForm.setSelectedColor(color);
            ColorPickerForm.getInstance().getCurrentColorPanel().setBackground(color);
            ColorPickerForm.getInstance().getCurrentColorLabel().setText(ColorUtil.toHex(color));
            ColorPickerFrame.getInstance().setVisible(false);
            ScreenFrame.getInstance().setVisible(false);
            App.mainFrame.setVisible(true);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            try {
                robot = new Robot();
            } catch (AWTException eg) {
                eg.printStackTrace();
            }
            Point point = e.getLocationOnScreen();
            int x = point.x;
            int y = point.y;
            ScreenFrame.getInstance().setVisible(false);
            Color color = robot.getPixelColor(x, y);

            ColorPickerForm.getInstance().getCurrentColorPanel().setBackground(color);
            ColorPickerForm.getInstance().getCurrentColorLabel().setText(ColorUtil.toHex(color));
            ScreenFrame.getInstance().setVisible(false);
        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mouseDragged(MouseEvent e) {

        }

        @Override
        public void mouseMoved(MouseEvent e) {
            try {
                robot = new Robot();
            } catch (AWTException eg) {
                eg.printStackTrace();
            }
            Point point = e.getLocationOnScreen();
            int x = point.x;
            int y = point.y;
            Color color = robot.getPixelColor(x, y);

            ColorPickerForm.getInstance().getZoomPanel().setBackground(color);
            ColorPickerForm.getInstance().getCurrentColorPanel().setBackground(color);
            ColorPickerForm.getInstance().getCurrentColorLabel().setText(ColorUtil.toHex(color));
        }
    }

    /**
     * 屏幕透明框架
     */
    public static class ScreenFrame extends JFrame {
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
}
