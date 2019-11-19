package com.luoboduner.moo.tool.ui.frame;

import com.apple.eawt.Application;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.form.func.ColorPickerForm;
import com.luoboduner.moo.tool.util.SystemUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

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
        ScreenFrame.getInstance().setVisible(true);
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
        FrameUtil.setFrameIcon(this);
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

        setLocation(10, 10);

        ScreenFrame.getInstance().setVisible(true);
    }

}
