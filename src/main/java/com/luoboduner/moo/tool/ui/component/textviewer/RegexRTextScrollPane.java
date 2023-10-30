package com.luoboduner.moo.tool.ui.component.textviewer;

import com.formdev.flatlaf.FlatLaf;
import com.luoboduner.moo.tool.App;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;

public class RegexRTextScrollPane extends RTextScrollPane {
    // constructor
    public RegexRTextScrollPane(RegexRSyntaxTextViewer textArea) {
        super(textArea);

        updateTheme();
    }

    public void updateTheme() {
        setMaximumSize(new Dimension(-1, -1));
        setMinimumSize(new Dimension(-1, -1));

        Color defaultBackground = App.mainFrame.getBackground();

        Gutter gutter = getGutter();
        if (FlatLaf.isLafDark()) {
            gutter.setBorderColor(gutter.getLineNumberColor().darker());
        } else {
            gutter.setBorderColor(gutter.getLineNumberColor().brighter());
        }
        gutter.setBackground(defaultBackground);
        Font font2 = new Font(App.config.getFont(), Font.PLAIN, App.config.getFontSize());
        gutter.setLineNumberFont(font2);
        gutter.setBackground(UIManager.getColor("Editor.gutter.background"));
        gutter.setBorderColor(UIManager.getColor("Editor.gutter.borderColor"));
        gutter.setLineNumberColor(UIManager.getColor("Editor.gutter.lineNumberColor"));
    }
}
