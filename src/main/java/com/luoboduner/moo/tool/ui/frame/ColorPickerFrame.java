package com.luoboduner.moo.tool.ui.frame;

import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.form.func.ColorPickerForm;

import javax.swing.*;

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
        ScreenFrame.getInstance().setVisible(true);
        ColorPickerFrame.getInstance().setVisible(true);
    }

    private void init() {
        setUndecorated(true);
        setAutoRequestFocus(false);
        setAlwaysOnTop(true);

        setName(UiConsts.APP_NAME);
        setTitle(UiConsts.APP_NAME + "-ColorPicker");
        FrameUtil.setFrameIcon(this);

        setContentPane(ColorPickerForm.getInstance().getColorPickerPanel());
        pack();
        setVisible(true);

        setLocation(10, 10);
    }

}
