package com.luoboduner.moo.tool.ui.component.textviewer;

import org.fife.ui.rtextarea.ExpandedFoldRenderStrategy;
import org.fife.ui.rtextarea.Gutter;

public class JsonRTextScrollPane extends CommonRTextScrollPane {

    public JsonRTextScrollPane(JsonRSyntaxTextViewer textArea) {
        super(textArea);
        updateTheme();
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        Gutter gutter = getGutter();
        // 保持默认的 MODERN 折叠图标样式，仅改为始终显示（默认 ON_HOVER 时悬停才出现）
        gutter.setExpandedFoldRenderStrategy(ExpandedFoldRenderStrategy.ALWAYS);
    }
}
