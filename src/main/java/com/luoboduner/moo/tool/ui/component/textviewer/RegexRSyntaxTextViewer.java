package com.luoboduner.moo.tool.ui.component.textviewer;

import com.formdev.flatlaf.FlatLaf;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.func.TimeConvertForm;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class RegexRSyntaxTextViewer extends RSyntaxTextArea {
    public RegexRSyntaxTextViewer() {

        setDoubleBuffered(true);

        updateTheme();
    }

    public void updateTheme() {
        try {
            Theme theme;
            if (FlatLaf.isLafDark()) {
                theme = Theme.load(App.class.getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
            } else {
                theme = Theme.load(App.class.getResourceAsStream(
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


        setBackground(UIManager.getColor("Editor.background"));
        setCaretColor(UIManager.getColor("Editor.caretColor"));
        setSelectionColor(UIManager.getColor("Editor.selectionBackground"));
        setCurrentLineHighlightColor(UIManager.getColor("Editor.currentLineHighlight"));
        setMarkAllHighlightColor(UIManager.getColor("Editor.markAllHighlightColor"));
        setMarkOccurrencesColor(UIManager.getColor("Editor.markOccurrencesColor"));
        setMatchedBracketBGColor(UIManager.getColor("Editor.matchedBracketBackground"));
        setMatchedBracketBorderColor(UIManager.getColor("Editor.matchedBracketBorderColor"));
        setPaintMatchedBracketPair(true);
        setAnimateBracketMatching(false);
    }
}
