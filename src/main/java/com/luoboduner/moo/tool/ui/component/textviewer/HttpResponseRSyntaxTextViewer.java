package com.luoboduner.moo.tool.ui.component.textviewer;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

/**
 * HTTP 响应 Body 编辑器（支持查找高亮）。
 */
public class HttpResponseRSyntaxTextViewer extends CommonRSyntaxTextViewer {
    public HttpResponseRSyntaxTextViewer() {
        setDoubleBuffered(true);
        updateTheme();
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        setCodeFoldingEnabled(false);
    }
}
