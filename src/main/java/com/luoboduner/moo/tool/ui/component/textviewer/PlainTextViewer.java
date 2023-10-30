package com.luoboduner.moo.tool.ui.component.textviewer;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.Style;

import javax.swing.*;
import java.awt.*;

/**
 * 自定义普通纯文本视图
 */
public abstract class PlainTextViewer extends JTextArea {
    public PlainTextViewer() {
        // 初始化背景色
        Style.blackTextArea(this);
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
