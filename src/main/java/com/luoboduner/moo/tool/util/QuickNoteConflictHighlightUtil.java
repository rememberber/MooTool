package com.luoboduner.moo.tool.util;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 高亮 Git 合并冲突标记行。
 */
public final class QuickNoteConflictHighlightUtil {

    private static final Pattern CONFLICT_LINE = Pattern.compile("^(<<<<<<<|=======|>>>>>>>).*$", Pattern.MULTILINE);
    private static final Color CONFLICT_COLOR = new Color(255, 96, 96, 96);

    private QuickNoteConflictHighlightUtil() {
    }

    public static void refresh(RSyntaxTextArea textArea) {
        if (textArea == null) {
            return;
        }
        clear(textArea);
        String text = textArea.getText();
        if (text == null || !text.contains("<<<<<<<")) {
            return;
        }
        Highlighter highlighter = textArea.getHighlighter();
        Matcher matcher = CONFLICT_LINE.matcher(text);
        List<Object> tags = tagsFor(textArea);
        while (matcher.find()) {
            try {
                Object tag = highlighter.addHighlight(
                        matcher.start(),
                        matcher.end(),
                        new DefaultHighlighter.DefaultHighlightPainter(CONFLICT_COLOR));
                tags.add(tag);
            } catch (BadLocationException ignored) {
                // document changed while highlighting
            }
        }
    }

    public static void clear(RSyntaxTextArea textArea) {
        if (textArea == null) {
            return;
        }
        Highlighter highlighter = textArea.getHighlighter();
        for (Object tag : tagsFor(textArea)) {
            highlighter.removeHighlight(tag);
        }
        tagsFor(textArea).clear();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> tagsFor(RSyntaxTextArea textArea) {
        Object key = QuickNoteConflictHighlightUtil.class;
        Object value = textArea.getClientProperty(key);
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        List<Object> tags = new ArrayList<>();
        textArea.putClientProperty(key, tags);
        return tags;
    }
}
