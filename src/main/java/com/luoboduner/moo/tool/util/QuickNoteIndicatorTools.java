package com.luoboduner.moo.tool.util;

import com.formdev.flatlaf.FlatLaf;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;

import javax.swing.*;
import java.awt.*;

/**
 * 随手记状态提示：使用短暂浮层 Toast，避免挤压工具栏布局。
 */
public final class QuickNoteIndicatorTools {

    private static Timer hideTimer;
    private static JWindow toastWindow;

    private QuickNoteIndicatorTools() {
    }

    public static void showTips(String tips, TipsLevel level) {
        Runnable show = () -> showToast(tips, level);
        if (SwingUtilities.isEventDispatchThread()) {
            show.run();
        } else {
            SwingUtilities.invokeLater(show);
        }
    }

    private static void showToast(String tips, TipsLevel level) {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        if (quickNoteForm == null) {
            return;
        }
        JPanel anchor = quickNoteForm.getQuickNotePanel();
        if (anchor == null || !anchor.isShowing()) {
            return;
        }

        dismissToast();

        JLabel label = new JLabel(tips);
        label.setOpaque(true);
        boolean dark = FlatLaf.isLafDark();
        label.setBackground(dark ? new Color(45, 45, 48, 235) : new Color(255, 255, 255, 240));
        label.setForeground(resolveForeground(level, dark));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(dark ? new Color(80, 80, 80) : new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        toastWindow = new JWindow(SwingUtilities.getWindowAncestor(anchor) instanceof Frame owner
                ? owner
                : null);
        toastWindow.setBackground(new Color(0, 0, 0, 0));
        toastWindow.getContentPane().add(label);
        toastWindow.pack();

        Point anchorLoc = anchor.getLocationOnScreen();
        Dimension anchorSize = anchor.getSize();
        Dimension toastSize = toastWindow.getSize();
        int x = anchorLoc.x + Math.max(8, anchorSize.width - toastSize.width - 16);
        int y = anchorLoc.y + Math.max(8, anchorSize.height - toastSize.height - 16);
        toastWindow.setLocation(x, y);
        toastWindow.setVisible(true);

        hideTimer = new Timer(2200, e -> dismissToast());
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    private static Color resolveForeground(TipsLevel level, boolean dark) {
        if (TipsLevel.SUCCESS.equals(level)) {
            return TipsLevel.successColor();
        }
        return dark ? new Color(220, 220, 220) : new Color(40, 40, 40);
    }

    private static void dismissToast() {
        if (hideTimer != null) {
            hideTimer.stop();
            hideTimer = null;
        }
        if (toastWindow != null) {
            toastWindow.setVisible(false);
            toastWindow.dispose();
            toastWindow = null;
        }
    }

    public enum TipsLevel {
        INFO,
        SUCCESS,
        WARN,
        ERROR;

        public static Color successColor() {
            return new Color(100, 197, 126);
        }
    }
}
