package com.luoboduner.moo.tool.ui.startup;

import lombok.extern.slf4j.Slf4j;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 单个工具槽：single-flight 初始化与状态管理。
 *
 * @param <M> 数据模型
 */
@Slf4j
final class ToolSlot<M> {

    private final String toolId;
    private final LazyToolInitializer<M> initializer;
    private final ToolContentHost host;
    private final AtomicReference<ToolLoadState> state = new AtomicReference<>(ToolLoadState.NEW);
    private final AtomicInteger generation = new AtomicInteger();
    private volatile CompletableFuture<Void> inflight;

    ToolSlot(String toolId, LazyToolInitializer<M> initializer, ToolContentHost host) {
        this.toolId = Objects.requireNonNull(toolId, "toolId");
        this.initializer = Objects.requireNonNull(initializer, "initializer");
        this.host = Objects.requireNonNull(host, "host");
    }

    String toolId() {
        return toolId;
    }

    ToolLoadState state() {
        return state.get();
    }

    ToolContentHost host() {
        return host;
    }

    boolean beginLoading() {
        while (true) {
            ToolLoadState current = state.get();
            if (current == ToolLoadState.LOADING || current == ToolLoadState.READY) {
                return false;
            }
            if (state.compareAndSet(current, ToolLoadState.LOADING)) {
                return true;
            }
        }
    }

    void ensureInitialized() {
        if (!beginLoading()) {
            return;
        }
        int gen = generation.incrementAndGet();
        showLoadingOnEdt();

        long startNs = System.nanoTime();
        CompletableFuture<Void> future = CompletableFuture
                .supplyAsync(() -> {
                    EdtGuard.assertNotEdt();
                    try {
                        long loadStart = System.nanoTime();
                        M data = initializer.loadData();
                        StartupMetrics.markTool("tool.loadData", toolId,
                                (System.nanoTime() - loadStart) / 1_000_000L);
                        return data;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, AppExecutors.io())
                .handle((data, error) -> {
                    EdtGuard.runOnEdt(() -> completeOnEdt(gen, data, error, startNs));
                    return null;
                });
        inflight = future;
    }

    void retry() {
        ToolLoadState current = state.get();
        if (current == ToolLoadState.LOADING) {
            return;
        }
        state.set(ToolLoadState.FAILED);
        ensureInitialized();
    }

    private void showLoadingOnEdt() {
        EdtGuard.runOnEdt(() -> host.showLoading(initializer.loadingMessage()));
    }

    private void completeOnEdt(int gen, M data, Throwable error, long startNs) {
        if (gen != generation.get()) {
            log.info("Discard stale tool load result tool={} gen={}", toolId, gen);
            return;
        }
        long totalMs = (System.nanoTime() - startNs) / 1_000_000L;
        if (error != null) {
            Throwable root = unwrap(error);
            log.error("Tool load failed tool=" + toolId, root);
            state.set(ToolLoadState.FAILED);
            host.showError("加载失败：" + root.getMessage(), root, ignored -> retry());
            StartupMetrics.markTool("tool.failed", toolId, totalMs);
            return;
        }
        try {
            long uiStart = System.nanoTime();
            JComponent view = initializer.createView();
            EdtGuard.assertEdt();
            initializer.bindData(view, data);
            host.showContent(view);
            initializer.startServices();
            state.set(ToolLoadState.READY);
            StartupMetrics.markTool("tool.bindUi", toolId, (System.nanoTime() - uiStart) / 1_000_000L);
            StartupMetrics.markTool("tool.ready", toolId, totalMs);
        } catch (Exception e) {
            log.error("Tool UI bind failed tool=" + toolId, e);
            state.set(ToolLoadState.FAILED);
            host.showError("界面初始化失败：" + e.getMessage(), e, ignored -> retry());
            StartupMetrics.markTool("tool.failed", toolId, totalMs);
        }
    }

    private static Throwable unwrap(Throwable error) {
        Throwable t = error;
        while (t.getCause() != null && (t instanceof RuntimeException || t instanceof java.util.concurrent.CompletionException)) {
            t = t.getCause();
        }
        return t;
    }

    void dispose() {
        generation.incrementAndGet();
        CompletableFuture<Void> f = inflight;
        if (f != null) {
            f.cancel(true);
        }
        try {
            initializer.dispose();
        } catch (Exception e) {
            log.warn("Dispose tool failed tool={}: {}", toolId, e.toString());
        }
        state.set(ToolLoadState.NEW);
        SwingUtilities.invokeLater(() -> host.showLoading(initializer.loadingMessage()));
    }
}
