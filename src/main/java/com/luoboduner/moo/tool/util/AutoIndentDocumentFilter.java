package com.luoboduner.moo.tool.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.util.function.Supplier;

/**
 * 纯文本组件的换行符自动缩进。
 * 仅当用户插入单个“\n”（例如，按 Enter）时适用，
 * 并保留上一行的缩进。对于 JSON，增加
 * 如果前一个修剪行以“{”或“[”结尾，则缩进 4 个空格。
 *
 * @author Cassian Florin
 * @email flowercard591@gmail.com
 * @date 2023/10/09 15:01
 */
public class AutoIndentDocumentFilter extends DocumentFilter {

    private final Supplier<String> bodyTypeSupplier;

    public AutoIndentDocumentFilter(Supplier<String> bodyTypeSupplier) {
        this.bodyTypeSupplier = bodyTypeSupplier;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if ("\n".equals(string)) {
            string = string + indentFor(fb.getDocument(), offset);
        }
        super.insertString(fb, offset, string, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if ("\n".equals(text)) {
            text = text + indentFor(fb.getDocument(), offset);
        }
        super.replace(fb, offset, length, text, attrs);
    }

    private String indentFor(Document doc, int offset) throws BadLocationException {
        String upToOffset = doc.getText(0, offset);
        int lineStart = upToOffset.lastIndexOf('\n') + 1;
        if (lineStart < 0) lineStart = 0;
        String currentLine = upToOffset.substring(lineStart);

        String baseIndent = leadingWhitespace(currentLine);
        String add = "";
        String bodyType = bodyTypeSupplier != null ? bodyTypeSupplier.get() : null;
        String trimmed = currentLine.trim();

        // 检测下一个非空白字符（直到换行符）以处理右大括号对齐
        char nextNonWs = nextNonWhitespaceChar(doc, offset);
        boolean closingAhead = (nextNonWs == '}' || nextNonWs == ']');

        if (bodyType != null && "application/json".equalsIgnoreCase(bodyType)) {
            if (!closingAhead && (trimmed.endsWith("{") || trimmed.endsWith("["))) {
                add = "    "; // 4 spaces
            }
        }
        return baseIndent + add;
    }

    private char nextNonWhitespaceChar(Document doc, int offset) throws BadLocationException {
        int len = doc.getLength();
        if (offset >= len) return 0;
        int max = len - offset;
        String tail = doc.getText(offset, max);
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (c == '\n') break; // stop at newline
            if (!Character.isWhitespace(c)) return c;
        }
        return 0;
    }

    private String leadingWhitespace(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t') {
                i++;
            } else {
                break;
            }
        }
        return i == 0 ? "" : s.substring(0, i);
    }
}
