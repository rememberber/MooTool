package com.luoboduner.moo.tool.ui.component;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.App;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JPopupMenuMouseAdapter extends MouseAdapter {

    private static final Log logger = LogFactory.get();

    private JPopupMenu popupMenu;

    public JPopupMenuMouseAdapter(JPopupMenu popupMenu) {
        this.popupMenu = popupMenu;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        switch (e.getButton()) {
            case MouseEvent.BUTTON1: {
                showMainFrame();
                break;
            }
            case MouseEvent.BUTTON2: {
                logger.debug("托盘图标中键事件");
                break;
            }
            case MouseEvent.BUTTON3: {
                logger.debug("托盘图标右键事件");
                break;
            }
            default: {
                break;
            }
        }
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            Dimension size = popupMenu.getPreferredSize();
            Point mousePoint = new Point(e.getXOnScreen(), e.getYOnScreen());
            Point popupLocation = calculatePopupLocation(mousePoint, size, getScreenBounds(mousePoint));
            popupMenu.setLocation(popupLocation);
            popupMenu.setInvoker(popupMenu);
            popupMenu.setVisible(true);
        }
    }

    static Point calculatePopupLocation(Point mousePoint, Dimension popupSize, Rectangle screenBounds) {
        int x = mousePoint.x - popupSize.width;
        int y = mousePoint.y - popupSize.height;

        int maxX = Math.max(screenBounds.x, screenBounds.x + screenBounds.width - popupSize.width);
        int maxY = Math.max(screenBounds.y, screenBounds.y + screenBounds.height - popupSize.height);

        x = Math.max(screenBounds.x, Math.min(x, maxX));
        y = Math.max(screenBounds.y, Math.min(y, maxY));

        return new Point(x, y);
    }

    private static Rectangle getScreenBounds(Point point) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice screenDevice : graphicsEnvironment.getScreenDevices()) {
            Rectangle bounds = screenDevice.getDefaultConfiguration().getBounds();
            if (bounds.contains(point)) {
                return bounds;
            }
        }
        return graphicsEnvironment.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    }

    public static void showMainFrame() {
        App.mainFrame.setVisible(true);
        if (App.mainFrame.getExtendedState() == Frame.ICONIFIED) {
            App.mainFrame.setExtendedState(Frame.NORMAL);
        } else if (App.mainFrame.getExtendedState() == 7) {
            App.mainFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        App.mainFrame.requestFocus();
    }
}
