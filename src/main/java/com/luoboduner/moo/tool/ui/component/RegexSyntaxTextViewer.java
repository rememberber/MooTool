package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.FlatLaf;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.Style;
import com.luoboduner.moo.tool.ui.form.func.TimeConvertForm;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;

import java.awt.*;
import java.io.IOException;

public class RegexSyntaxTextViewer extends RSyntaxTextArea {
    public RegexSyntaxTextViewer() {

        try {
            Theme theme;
            if (FlatLaf.isLafDark()) {
                theme = Theme.load(RegexSyntaxTextViewer.class.getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
            } else {
                theme = Theme.load(RegexSyntaxTextViewer.class.getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
            }
            theme.apply(this);
        } catch (IOException ioe) { // Never happens
            ioe.printStackTrace();
        }

        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        setCodeFoldingEnabled(true);
//        setCurrentLineHighlightColor(new Color(52, 52, 52));
//        setUseSelectedTextColor(true);
//        setSelectedTextColor(new Color(50, 50, 50));

        // 初始化背景色
//        Style.blackTextArea(this);
        setBackground(TimeConvertForm.getInstance().getTimeHisTextArea().getBackground());
        // 初始化边距
        setMargin(new Insets(10, 10, 10, 10));

        // 初始化字体
        String fontName = App.config.getJsonBeautyFontName();
        int fontSize = App.config.getJsonBeautyFontSize();
        if (fontSize == 0) {
            fontSize = getFont().getSize() + 2;
        }
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        setFont(font);

        setHyperlinksEnabled(false);

        Style.blackTextArea(this);

        setDoubleBuffered(true);
    }
}
