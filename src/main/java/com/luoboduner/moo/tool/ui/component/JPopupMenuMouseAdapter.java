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
            popupMenu.setLocation(e.getX() - size.width, e.getY() - size.height);
            popupMenu.setInvoker(popupMenu);
            popupMenu.setVisible(true);
        }
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