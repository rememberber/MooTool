package com.luoboduner.moo.tool.ui.startup;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 合并高频 UI 刷新请求，同一时刻最多排队一次 EDT 任务。
 */
public final class UiTaskCoalescer {

    private final AtomicBoolean queued = new AtomicBoolean();
    private final Runnable refresh;

    public UiTaskCoalescer(Runnable refresh) {
        this.refresh = refresh;
    }

    public void request() {
        if (!queued.compareAndSet(false, true)) {
            return;
        }
        EdtGuard.runOnEdt(() -> {
            // 在刷新开始前清除标记，刷新期间的新请求可再排一次。
            queued.set(false);
            refresh.run();
        });
    }

    public boolean isQueued() {
        return queued.get();
    }
}
