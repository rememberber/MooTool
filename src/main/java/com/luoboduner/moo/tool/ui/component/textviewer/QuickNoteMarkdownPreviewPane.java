package com.luoboduner.moo.tool.ui.component.textviewer;

import com.luoboduner.moo.tool.util.QuickNoteMarkdownUtil;
import com.luoboduner.moo.tool.util.ScrollUtil;

import javax.swing.*;
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
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html; charset=utf-8");
        editorPane.setEditorKit(new HTMLEditorKit());
        editorPane.setMargin(new Insets(0, 0, 0, 0));
        editorPane.setBackground(UIManager.getColor("Editor.background"));
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
        ScrollUtil.smoothPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
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
        lastMarkdown = "";
        updateContent(markdown);
    }

    public void reset() {
        lastMarkdown = "";
        editorPane.setText(QuickNoteMarkdownUtil.toHtmlDocument(""));
    }
}
