package org.fife.ui.rsyntaxtextarea;

import com.luoboduner.moo.tool.util.EditorFontUtil;

import javax.swing.text.TabExpander;
import java.awt.*;

/**
 * 在 JetBrains Mono 等缺字字体下，为中文等字符回退到界面字体，避免显示为方块。
 */
public class CjkFallbackTokenPainter extends DefaultTokenPainter {

    @Override
    public float nextX(Token token, int charCount, float x, RSyntaxTextArea host, TabExpander e) {
        int textOffs = token.getTextOffset();
        char[] text = token.getTextArray();
        int end = textOffs + charCount;
        int flushLen = 0;
        int flushIndex = textOffs;
        Font primary = host.getFontForTokenType(token.getType());
        Font fallback = EditorFontUtil.getCjkFallbackFont(primary);
        FontMetrics primaryFm = host.getFontMetrics(primary);
        FontMetrics fallbackFm = host.getFontMetrics(fallback);

        for (int i = textOffs; i < end; i++) {
            if (text[i] == '\t') {
                x = e.nextTabStop(
                        x + EditorFontUtil.charsWidth(primaryFm, fallbackFm, primary, fallback, text, flushIndex, flushLen),
                        0);
                flushLen = 0;
                flushIndex = i + 1;
            } else {
                flushLen++;
            }
        }

        return x + EditorFontUtil.charsWidth(primaryFm, fallbackFm, primary, fallback, text, flushIndex, flushLen);
    }

    @Override
    protected float paintImpl(Token token, Graphics2D g, float x, float y,
                              RSyntaxTextArea host, TabExpander e, float clipStart,
                              boolean selected, boolean useSTC) {
        int origX = (int) x;
        int textOffs = token.getTextOffset();
        char[] text = token.getTextArray();
        int end = textOffs + token.length();
        float nextX = x;
        int flushLen = 0;
        int flushIndex = textOffs;
        Color fg = useSTC ? host.getSelectedTextColor() : host.getForegroundForToken(token);
        Color bg = selected ? null : host.getBackgroundForToken(token);
        Font primary = host.getFontForToken(token);
        Font fallback = EditorFontUtil.getCjkFallbackFont(primary);
        FontMetrics primaryFm = host.getFontMetrics(primary);
        FontMetrics fallbackFm = host.getFontMetrics(fallback);

        for (int i = textOffs; i < end; i++) {
            switch (text[i]) {
                case '\t':
                    nextX = e.nextTabStop(
                            x + EditorFontUtil.charsWidth(primaryFm, fallbackFm, primary, fallback, text, flushIndex, flushLen),
                            0);
                    if (bg != null) {
                        paintBackground(x, y, nextX - x, primaryFm.getHeight(), g, primaryFm.getAscent(), host, bg);
                    }
                    if (flushLen > 0) {
                        g.setColor(fg);
                        EditorFontUtil.drawCharsWithFallback(g, text, flushIndex, flushLen, (int) x, (int) y, primary, fallback);
                        flushLen = 0;
                    }
                    flushIndex = i + 1;
                    x = nextX;
                    break;
                default:
                    flushLen += 1;
                    break;
            }
        }

        nextX = x + EditorFontUtil.charsWidth(primaryFm, fallbackFm, primary, fallback, text, flushIndex, flushLen);
        Rectangle r = host.getMatchRectangle();

        if (flushLen > 0 && nextX >= clipStart) {
            if (bg != null) {
                paintBackground(x, y, nextX - x, primaryFm.getHeight(), g, primaryFm.getAscent(), host, bg);
                if (token.length() == 1 && r != null && r.x == x) {
                    ((RSyntaxTextAreaUI) host.getUI()).paintMatchedBracketImpl(g, host, r);
                }
            }
            g.setColor(fg);
            EditorFontUtil.drawCharsWithFallback(g, text, flushIndex, flushLen, (int) x, (int) y, primary, fallback);
        }

        if (host.getUnderlineForToken(token)) {
            g.setColor(fg);
            int y2 = (int) (y + 1);
            g.drawLine(origX, y2, (int) nextX, y2);
        }

        if (host.getPaintTabLines() && origX == host.getMargin().left) {
            paintTabLines(token, origX, (int) y, (int) nextX, g, e, host);
        }

        return nextX;
    }
}
