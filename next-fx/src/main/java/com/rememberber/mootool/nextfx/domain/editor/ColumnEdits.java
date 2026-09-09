package com.rememberber.mootool.nextfx.domain.editor;

import java.util.ArrayList;
import java.util.List;

/**
 * Rectangular / column edits use logical lines and visual columns (tabs expand to {@code tabSize}).
 */
public final class ColumnEdits {

    private ColumnEdits() {
    }

    public static String insert(String text, int startLine, int endLine, int column, String insertion, int tabSize) {
        List<String> lines = splitKeepLast(text);
        int from = Math.max(0, Math.min(startLine, endLine));
        int to = Math.min(lines.size() - 1, Math.max(startLine, endLine));
        String insert = insertion == null ? "" : insertion;
        for (int index = from; index <= to; index++) {
            lines.set(index, insertAtVisualColumn(lines.get(index), column, insert, tabSize));
        }
        return join(lines, text.endsWith("\n"));
    }

    public static String delete(String text, int startLine, int endLine, int startColumn, int endColumn, int tabSize) {
        List<String> lines = splitKeepLast(text);
        int from = Math.max(0, Math.min(startLine, endLine));
        int to = Math.min(lines.size() - 1, Math.max(startLine, endLine));
        int left = Math.min(startColumn, endColumn);
        int right = Math.max(startColumn, endColumn);
        for (int index = from; index <= to; index++) {
            lines.set(index, deleteVisualRange(lines.get(index), left, right, tabSize));
        }
        return join(lines, text.endsWith("\n"));
    }

    public static String paste(String text, int startLine, int startColumn, String clipboard, int tabSize) {
        String[] clipLines = clipboard == null ? new String[] {""} : clipboard.split("\n", -1);
        List<String> lines = splitKeepLast(text);
        while (lines.size() < startLine + clipLines.length) {
            lines.add("");
        }
        for (int offset = 0; offset < clipLines.length; offset++) {
            int index = startLine + offset;
            lines.set(index, insertAtVisualColumn(lines.get(index), startColumn, clipLines[offset], tabSize));
        }
        return join(lines, text.endsWith("\n") || clipboard.endsWith("\n"));
    }

    public static int visualColumn(String line, int charIndex, int tabSize) {
        int column = 0;
        int limit = Math.min(charIndex, line.length());
        for (int i = 0; i < limit; i++) {
            if (line.charAt(i) == '\t') {
                column += tabSize - (column % tabSize);
            } else {
                column += 1;
            }
        }
        return column;
    }

    static String insertAtVisualColumn(String line, int column, String insertion, int tabSize) {
        int index = indexOfVisualColumn(line, column, tabSize);
        if (index > line.length()) {
            return line + " ".repeat(index - line.length()) + insertion;
        }
        if (index == line.length() && visualColumn(line, line.length(), tabSize) < column) {
            return line + " ".repeat(column - visualColumn(line, line.length(), tabSize)) + insertion;
        }
        return line.substring(0, index) + insertion + line.substring(index);
    }

    private static String deleteVisualRange(String line, int startColumn, int endColumn, int tabSize) {
        int start = indexOfVisualColumn(line, startColumn, tabSize);
        int end = indexOfVisualColumn(line, endColumn, tabSize);
        start = Math.min(start, line.length());
        end = Math.min(end, line.length());
        if (start >= end) {
            return line;
        }
        return line.substring(0, start) + line.substring(end);
    }

    private static int indexOfVisualColumn(String line, int column, int tabSize) {
        int visual = 0;
        for (int i = 0; i < line.length(); i++) {
            if (visual >= column) {
                return i;
            }
            if (line.charAt(i) == '\t') {
                visual += tabSize - (visual % tabSize);
            } else {
                visual += 1;
            }
        }
        return line.length() + Math.max(0, column - visual);
    }

    private static List<String> splitKeepLast(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(text.substring(start, i));
                start = i + 1;
            }
        }
        if (start <= text.length()) {
            if (start < text.length() || !text.endsWith("\n")) {
                lines.add(text.substring(start));
            } else if (lines.isEmpty()) {
                lines.add("");
            }
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private static String join(List<String> lines, boolean trailingNewline) {
        String joined = String.join("\n", lines);
        if (trailingNewline && !joined.endsWith("\n")) {
            return joined + "\n";
        }
        return joined;
    }
}
