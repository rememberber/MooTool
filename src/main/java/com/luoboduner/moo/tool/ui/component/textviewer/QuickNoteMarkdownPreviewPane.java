package com.luoboduner.moo.tool.ui.component.textviewer;

import com.luoboduner.moo.tool.util.QuickNoteMarkdownUtil;
import com.luoboduner.moo.tool.util.ScrollUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 随手记 Markdown 预览面板
 */
public class QuickNoteMarkdownPreviewPane extends JPanel {

    private final JEditorPane editorPane;
    private final JScrollPane scrollPane;
    private String lastMarkdown = "";

    public QuickNoteMarkdownPreviewPane() {
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html; charset=utf-8");
        editorPane.setEditorKit(new HTMLEditorKit());
        editorPane.setMargin(new Insets(0, 0, 0, 0));
        syncEditorBackground();
        editorPane.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI(e.getURL().toString()));
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
        });

        scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.getViewport().setBackground(editorPane.getBackground());
        ScrollUtil.smoothPane(scrollPane);
        ScrollUtil.gentleWheelScroll(scrollPane, editorPane);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void syncEditorBackground() {
        Color background = UIManager.getColor("Editor.background");
        if (background == null) {
            background = UIManager.getColor("Panel.background");
        }
        if (background != null) {
            editorPane.setBackground(background);
            if (scrollPane != null) {
                scrollPane.getViewport().setBackground(background);
            }
        }
    }

    public void updateContent(String markdown) {
        if (markdown == null) {
            markdown = "";
        }
        if (markdown.equals(lastMarkdown)) {
            return;
        }
        lastMarkdown = markdown;
        int caretPosition = editorPane.getCaretPosition();
        editorPane.setText(QuickNoteMarkdownUtil.toHtmlDocument(markdown));
        editorPane.setCaretPosition(Math.min(caretPosition, editorPane.getDocument().getLength()));
    }

    public void refreshTheme(String markdown) {
        syncEditorBackground();
        lastMarkdown = "";
        updateContent(markdown);
    }

    public void reset() {
        lastMarkdown = "";
        editorPane.setText(QuickNoteMarkdownUtil.toHtmlDocument(""));
    }
}
