package org.fife.ui.rsyntaxtextarea;

/**
 * 为 RSyntaxTextArea 提供支持中文回退的 TokenPainter。
 */
public class CjkFallbackTokenPainterFactory implements TokenPainterFactory {

    @Override
    public TokenPainter getTokenPainter(RSyntaxTextArea textArea) {
        if (textArea.isWhitespaceVisible()) {
            return new VisibleWhitespaceTokenPainter();
        }
        return new CjkFallbackTokenPainter();
    }
}
