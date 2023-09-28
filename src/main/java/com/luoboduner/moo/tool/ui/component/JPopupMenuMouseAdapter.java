package com.luoboduner.moo.tool.ui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JPopupMenuMouseAdapter extends MouseAdapter {

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

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            Dimension size = popupMenu.getPreferredSize();
            popupMenu.setLocation(e.getX() - size.width, e.getY() - size.height);
            popupMenu.setInvoker(popupMenu);
            popupMenu.setVisible(true);
        }
    }
}