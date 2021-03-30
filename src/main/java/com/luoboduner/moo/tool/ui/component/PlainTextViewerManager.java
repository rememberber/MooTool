package com.luoboduner.moo.tool.ui.component;

import java.util.Map;

/**
 * 自定义普通纯文本视图管理器
 */
public class PlainTextViewerManager {

    private Map<String, PlainTextViewer> viewMap;

    /**
     * 创建一个新实例
     *
     * @return
     */
    public PlainTextViewer newPlainTextViewer() {
        return new PlainTextViewer();
    }

}
