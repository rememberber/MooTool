package com.luoboduner.moo.tool.ui.component;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义普通纯文本视图管理器
 */
public class QuickNotePlainTextViewerManager {

    private Map<String, QuickNotePlainTextViewer> viewMap = new HashMap<>();

    /**
     * 创建一个新实例
     *
     * @return
     */
    public QuickNotePlainTextViewer newPlainTextViewer() {
        return new QuickNotePlainTextViewer();
    }

    /**
     * 创建一个新实例，按名称放入map
     *
     * @return
     */
    public QuickNotePlainTextViewer newPlainTextViewer(String name) {
        QuickNotePlainTextViewer plainTextViewer = new QuickNotePlainTextViewer();
        viewMap.put(name, plainTextViewer);
        return plainTextViewer;
    }

    /**
     * 按名称获取一个实例，若不存在则新建
     *
     * @return
     */
    public QuickNotePlainTextViewer getPlainTextViewer(String name) {
        QuickNotePlainTextViewer plainTextViewer = viewMap.get(name);
        if (plainTextViewer == null) {
            plainTextViewer = new QuickNotePlainTextViewer();
            viewMap.put(name, plainTextViewer);
        }
        return plainTextViewer;
    }

}
