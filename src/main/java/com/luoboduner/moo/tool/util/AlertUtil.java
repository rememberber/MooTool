package com.luoboduner.moo.tool.util;

import javax.swing.*;

public class AlertUtil {
    public static void buttonInfo(JButton button, String textBefore, String text, int millions) {
        // button显示text的内容，millions后恢复
        button.setText(text);
        new Thread(() -> {
            try {
                Thread.sleep(millions);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            button.setText(textBefore);
        }).start();
    }
}
