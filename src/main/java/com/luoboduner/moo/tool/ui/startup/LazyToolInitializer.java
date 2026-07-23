package com.luoboduner.moo.tool.ui.startup;

import javax.swing.JComponent;

/**
 * 工具页懒加载初始化契约：后台加载数据，EDT 创建与绑定 UI。
 *
 * @param <M> 后台加载得到的不可变/普通数据模型
 */
public interface LazyToolInitializer<M> {

    /**
     * 后台线程：只返回普通数据，不得触碰 Swing。
     */
    M loadData() throws Exception;

    /**
     * EDT：创建 Swing 页面（可含轻量布局）。
     */
    JComponent createView();

    /**
     * EDT：绑定完整数据。
     */
    void bindData(JComponent view, M data);

    /**
     * 页面 Ready 后启动页面级 watcher/scheduler。
     */
    default void startServices() {
    }

    /**
     * 页面或应用释放时停止服务。
     */
    default void dispose() {
    }

    /**
     * Loading 文案。
     */
    default String loadingMessage() {
        return "Loading……";
    }
}
