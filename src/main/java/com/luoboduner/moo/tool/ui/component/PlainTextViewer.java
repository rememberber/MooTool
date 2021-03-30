package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.util.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 自定义普通纯文本视图
 */
public class PlainTextViewer extends JTextArea {
    public PlainTextViewer() {
        if (UIUtil.isDarkLaf()) {
            Color bgColor = new Color(30, 30, 30);
            setBackground(bgColor);
            Color foreColor = new Color(187, 187, 187);
            setForeground(foreColor);
        }
    }
}
