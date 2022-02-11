package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.FlatLaf;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.func.TimeConvertForm;
import com.luoboduner.moo.tool.ui.listener.func.QuickNoteListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class QuickNoteSyntaxTextViewer extends RSyntaxTextArea {
    public QuickNoteSyntaxTextViewer() {

        try {
            Theme theme;
            if (FlatLaf.isLafDark()) {
                theme = Theme.load(JsonSyntaxTextViewer.class.getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
            } else {
                theme = Theme.load(JsonSyntaxTextViewer.class.getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
            }
            theme.apply(this);
        } catch (IOException ioe) { // Never happens
            ioe.printStackTrace();
        }

        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        setCodeFoldingEnabled(true);
//        setUseSelectedTextColor(true);
//        setSelectedTextColor(new Color(50, 50, 50));

        JTextArea timeHisTextArea = TimeConvertForm.getInstance().getTimeHisTextArea();
        setSelectionColor(timeHisTextArea.getSelectionColor());
        setCaretColor(UIManager.getColor("Editor.caretColor"));
        setCurrentLineHighlightColor(UIManager.getColor("Editor.currentLineHighlight"));
        setMarkAllHighlightColor(UIManager.getColor("Editor.markAllHighlightColor"));
        setMarkOccurrencesColor(UIManager.getColor("Editor.markOccurrencesColor"));
        setMatchedBracketBGColor(UIManager.getColor("Editor.matchedBracketBackground"));
        setMatchedBracketBorderColor(UIManager.getColor("Editor.matchedBracketBorderColor"));

        setPaintMatchedBracketPair(true);

        // 初始化背景色
//        Style.blackTextArea(this);
        setBackground(TimeConvertForm.getInstance().getTimeHisTextArea().getBackground());
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

        setHyperlinksEnabled(true);
        addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI(e.getURL().toString()));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });

        setDoubleBuffered(true);

        // 文本域键盘事件
        addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_S) {
                    QuickNoteListener.quickSave(true);
                } else if (evt.isControlDown() && evt.isShiftDown() && evt.getKeyCode() == KeyEvent.VK_F) {
                    QuickNoteListener.format();
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_F) {
                    QuickNoteListener.showFindPanel();
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_R) {
                    QuickNoteListener.showFindPanel();
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_N) {
                    QuickNoteListener.newNote();
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                QuickNoteListener.quickSave(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                QuickNoteListener.quickSave(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                QuickNoteListener.quickSave(true);
            }
        });
    }
}
