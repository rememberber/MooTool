package com.luoboduner.moo.tool.util;

import javax.swing.*;

public class AlertUtil {
    public static void buttonInfo(JButton button, String textBefore, String text, int millions) {
        Runnable show = () -> {
            Icon iconBefore = button.getIcon();
            boolean restoreIcon = iconBefore != null && textBefore.isEmpty();
            if (restoreIcon) {
                button.setIcon(null);
            }
            button.putClientProperty("AlertUtil.iconBefore", restoreIcon ? iconBefore : null);
            button.setText(text);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            show.run();
        } else {
            SwingUtilities.invokeLater(show);
        }
        new Thread(() -> {
            try {
                Thread.sleep(millions);
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
            }
            SwingUtilities.invokeLater(() -> {
                Icon iconBefore = (Icon) button.getClientProperty("AlertUtil.iconBefore");
                button.setText(textBefore);
                if (iconBefore != null) {
                    button.setIcon(iconBefore);
                    button.putClientProperty("AlertUtil.iconBefore", null);
                }
            });
        }, "AlertUtil-buttonInfo").start();
    }
}
