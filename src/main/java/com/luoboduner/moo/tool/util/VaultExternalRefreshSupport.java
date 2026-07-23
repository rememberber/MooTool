package com.luoboduner.moo.tool.util;

import javax.swing.SwingUtilities;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Vault 外部变更刷新：防抖 + UI 合并 + 后台扫盘。
 */
final class VaultExternalRefreshSupport {

    private final AtomicReference<ScheduledFuture<?>> pending = new AtomicReference<>();
    private final AtomicBoolean inFlight = new AtomicBoolean();
    private final AtomicBoolean rerunRequested = new AtomicBoolean();
    private final BooleanSupplier shouldSuppress;
    private final Consumer<Boolean> asyncRefresh;

    VaultExternalRefreshSupport(BooleanSupplier shouldSuppress, Consumer<Boolean> asyncRefresh) {
        this.shouldSuppress = shouldSuppress;
        this.asyncRefresh = asyncRefresh;
    }

    void requestDebounced() {
        if (shouldSuppress.getAsBoolean()) {
            return;
        }
        ScheduledFuture<?> previous = pending.getAndSet(
                com.luoboduner.moo.tool.ui.startup.AppExecutors.deferred().schedule(() -> {
                    if (shouldSuppress.getAsBoolean()) {
                        return;
                    }
                    runAsync(false);
                }, 600, TimeUnit.MILLISECONDS));
        if (previous != null) {
            previous.cancel(false);
        }
    }

    void requestImmediate(boolean force) {
        cancelPending();
        runAsync(force);
    }

    void cancelPending() {
        ScheduledFuture<?> previous = pending.getAndSet(null);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    private void runAsync(boolean force) {
        if (!inFlight.compareAndSet(false, true)) {
            rerunRequested.set(true);
            return;
        }
        asyncRefresh.accept(force);
    }

    void markFinished() {
        inFlight.set(false);
        if (rerunRequested.compareAndSet(true, false)) {
            runAsync(false);
        }
    }

    static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
}
