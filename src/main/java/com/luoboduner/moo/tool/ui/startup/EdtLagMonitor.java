package com.luoboduner.moo.tool.ui.startup;

import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 开发期 EDT 延迟监控：队列中最多一个待执行 ping。
 */
@Slf4j
public final class EdtLagMonitor {

    private static final long THRESHOLD_MS = Long.getLong("mootool.edtLagThresholdMs", 50L);
    private static final AtomicBoolean QUEUED = new AtomicBoolean();
    private static volatile boolean running;

    private EdtLagMonitor() {
    }

    public static void startIfEnabled() {
        boolean enabled = Boolean.parseBoolean(System.getProperty("mootool.edtDiagnostics", "false"))
                || Boolean.getBoolean("mootool.dev");
        if (!enabled) {
            return;
        }
        running = true;
        schedulePing();
        log.info("EDT lag monitor enabled thresholdMs={}", THRESHOLD_MS);
    }

    public static void stop() {
        running = false;
    }

    private static void schedulePing() {
        if (!running || !QUEUED.compareAndSet(false, true)) {
            return;
        }
        long scheduledAt = System.nanoTime();
        SwingUtilities.invokeLater(() -> {
            QUEUED.set(false);
            long lagMs = (System.nanoTime() - scheduledAt) / 1_000_000L;
            if (lagMs >= THRESHOLD_MS) {
                log.warn("EDT lag={}ms phase={} stackSample={}",
                        lagMs,
                        StartupCoordinator.getInstance().currentPhase(),
                        sampleEdtHint());
            }
            if (running) {
                AppExecutors.deferred().schedule(EdtLagMonitor::schedulePing, 200, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        });
    }

    private static String sampleEdtHint() {
        return "AWT-EventQueue busy; check startup/tool init logs";
    }
}
