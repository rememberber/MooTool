package com.luoboduner.moo.tool.util;

import com.formdev.flatlaf.util.FontUtils;

import javax.swing.*;
import java.awt.*;

/**
 * 编辑器字体工具：为仅含拉丁字形的字体（如 JetBrains Mono）提供中文回退。
 */
public final class EditorFontUtil {

    private EditorFontUtil() {
    }

    public static Font getEditorFont(String fontName, int style, int size) {
        return FontUtils.getCompositeFont(fontName, style, size);
    }

    public static Font getCjkFallbackFont(Font reference) {
        Font uiFont = UIManager.getFont("Label.font");
        if (uiFont == null) {
            uiFont = new Font(Font.SANS_SERIF, Font.PLAIN, reference.getSize());
        }
        return FontUtils.getCompositeFont(uiFont.getFamily(), reference.getStyle(), reference.getSize());
    }

    public static boolean usePrimaryFont(Font primary, char c) {
        return c == '\t' || primary.canDisplay(c);
    }

    public static int charsWidth(FontMetrics primaryFm, FontMetrics fallbackFm,
                                 Font primary, Font fallback,
                                 char[] text, int start, int len) {
        int width = 0;
        for (int i = start; i < start + len; i++) {
            char c = text[i];
            width += usePrimaryFont(primary, c) ? primaryFm.charWidth(c) : fallbackFm.charWidth(c);
        }
        return width;
    }

    public static void drawCharsWithFallback(Graphics2D g, char[] text, int start, int len,
                                             int x, int y, Font primary, Font fallback) {
        if (len <= 0) {
            return;
        }
        FontMetrics primaryFm = g.getFontMetrics(primary);
        FontMetrics fallbackFm = g.getFontMetrics(fallback);
        int cx = x;
        int i = start;
        while (i < start + len) {
            boolean primaryRun = usePrimaryFont(primary, text[i]);
            int runStart = i;
            while (i < start + len && usePrimaryFont(primary, text[i]) == primaryRun) {
                i++;
            }
            int runLen = i - runStart;
            g.setFont(primaryRun ? primary : fallback);
            g.drawChars(text, runStart, runLen, cx, y);
            cx += primaryRun
                    ? primaryFm.charsWidth(text, runStart, runLen)
                    : fallbackFm.charsWidth(text, runStart, runLen);
        }
    }
}
