package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 自定义普通纯文本视图
 */
public abstract class PlainTextViewer extends JTextArea {
    public PlainTextViewer() {
        // 初始化背景色
        if (UIUtil.isDarkLaf()) {
            Color bgColor = new Color(30, 30, 30);
            setBackground(bgColor);
            Color foreColor = new Color(187, 187, 187);
            setForeground(foreColor);
        }
        // 初始化边距
        setMargin(new Insets(10, 10, 10, 10));

        // 初始化字体
        String fontName = App.config.getQuickNoteFontName();
        int fontSize = App.config.getQuickNoteFontSize();
        if (fontSize == 0) {
            fontSize = getFont().getSize() + 2;
        }
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        setFont(font);
    }
}
