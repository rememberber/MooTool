package com.luoboduner.moo.tool.ui.listener;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.func.ColorBoardForm;
import com.luoboduner.moo.tool.ui.form.func.ColorPickerForm;
import com.luoboduner.moo.tool.ui.frame.ColorPickerFrame;
import com.luoboduner.moo.tool.ui.frame.ScreenFrame;
import com.luoboduner.moo.tool.util.ColorUtil;

import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * <pre>
 * ScreenMouseListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/19.
 */
public class ScreenMouseListener implements MouseInputListener {
    private static Robot robot;

    @Override
    public void mouseClicked(MouseEvent e) {
        robot = getRobot();
        Point point = e.getLocationOnScreen();
        ScreenFrame.getInstance().setVisible(false);
        Color color = robot.getPixelColor(point.x, point.y);

        ColorBoardForm.setSelectedColor(color);
        ColorPickerForm.getInstance().getCurrentColorPanel().setBackground(color);
        ColorPickerForm.getInstance().getCurrentColorLabel().setText(ColorUtil.toHex(color));
        ColorPickerFrame.getInstance().setVisible(false);
        ScreenFrame.getInstance().setVisible(false);
        App.mainFrame.setVisible(true);
    }

    @Override
    public void mousePressed(MouseEvent e) {
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
        robot = getRobot();
        Point point = e.getLocationOnScreen();
        int x = point.x;
        int y = point.y;

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (x < 220 || y < 230) {
            ColorPickerFrame.getInstance().setLocation((int) screen.getWidth() - 220, (int) screen.getHeight() - 300);
        } else if (x > (screen.getWidth() - 220) || y > (screen.getHeight() - 300)) {
            ColorPickerFrame.getInstance().setLocation(10, 10);
        }
        Color color = robot.getPixelColor(x, y);

        ColorPickerForm.getInstance().getZoomPanel().setBackground(color);
        ColorPickerForm.getInstance().getCurrentColorPanel().setBackground(color);
        ColorPickerForm.getInstance().getCurrentColorLabel().setText(ColorUtil.toHex(color));
    }

    private static Robot getRobot() {
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (AWTException eg) {
                eg.printStackTrace();
            }
        }
        return robot;
    }
}
