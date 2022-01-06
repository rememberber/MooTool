package com.luoboduner.moo.tool.ui.component;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义普通纯文本视图管理器
 */
public class QuickNoteSyntaxTextViewerManager {

    private Map<String, QuickNoteSyntaxTextViewer> viewMap = new HashMap<>();

    /**
     * 创建一个新实例
     *
     * @return
     */
    public QuickNoteSyntaxTextViewer newPlainTextViewer() {
        return new QuickNoteSyntaxTextViewer();
    }

    /**
     * 创建一个新实例，按名称放入map
     *
     * @return
     */
    public QuickNoteSyntaxTextViewer newPlainTextViewer(String name) {
        QuickNoteSyntaxTextViewer plainTextViewer = new QuickNoteSyntaxTextViewer();
        viewMap.put(name, plainTextViewer);
        return plainTextViewer;
    }

    /**
     * 按名称获取一个实例，若不存在则新建
     *
     * @return
     */
    public QuickNoteSyntaxTextViewer getSyntaxTextViewer(String name) {
        QuickNoteSyntaxTextViewer plainTextViewer = viewMap.get(name);
        if (plainTextViewer == null) {
            plainTextViewer = new QuickNoteSyntaxTextViewer();
            viewMap.put(name, plainTextViewer);
        }
        return plainTextViewer;
    }

}
