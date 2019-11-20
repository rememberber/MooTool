package com.luoboduner.moo.tool.ui.listener;

import com.luoboduner.moo.tool.ui.Init;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.ColorBoardForm;
import com.luoboduner.moo.tool.ui.form.func.ColorPickerForm;
import com.luoboduner.moo.tool.ui.frame.ColorPickerFrame;
import com.luoboduner.moo.tool.ui.frame.ScreenFrame;
import com.luoboduner.moo.tool.util.ColorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
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
@Slf4j
public class ScreenMouseListener implements MouseInputListener {
    public static Robot robot;

    @Override
    public void mouseClicked(MouseEvent e) {
        Point point = e.getLocationOnScreen();
        ScreenFrame.exit();
        Color color = robot.getPixelColor(point.x, point.y);

        ColorBoardForm.setSelectedColor(color);
        MainWindow.getInstance().getTabbedPane().setSelectedIndex(10);
        Init.showMainFrame();
        ColorPickerFrame.exit();
        ScreenMouseListener.robot = null;
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
        Point point = e.getLocationOnScreen();
        int x = point.x;
        int y = point.y;

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (x < 250 && y < 300) {
            ColorPickerFrame.getInstance().setLocation((int) screen.getWidth() - 220, (int) screen.getHeight() - 300);
        } else if (x > (screen.getWidth() - 250) && y > (screen.getHeight() - 350)) {
            ColorPickerFrame.getInstance().setLocation(10, 10);
        }
//        ScreenFrame.getInstance().setOpacity(0);
        Color color = robot.getPixelColor(x, y);
        ScreenFrame.getInstance().setOpacity(0.01f);

        ColorPickerForm.getInstance().getZoomPanel().setBackground(color);
        ColorPickerForm.getInstance().getCurrentColorPanel().setBackground(color);
        ColorPickerForm.getInstance().getCurrentColorLabel().setText(ColorUtil.toHex(color));
    }

    public static Robot getRobot() {
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(ColorBoardForm.getInstance().getColorBoardPanel(), e.getMessage(), "系统异常", JOptionPane.ERROR_MESSAGE);
                log.error(ExceptionUtils.getStackTrace(e));
            }
        }
        return robot;
    }
}
