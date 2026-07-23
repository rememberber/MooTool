package com.luoboduner.moo.tool.ui.startup;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 启动阶段耗时埋点。
 */
@Slf4j
public final class StartupMetrics {

    private static final long APP_START_NANOS = System.nanoTime();
    private static final Map<String, Long> PHASE_ELAPSED_MS = new ConcurrentHashMap<>();
    private static final AtomicLong LAST_PHASE_NANOS = new AtomicLong(APP_START_NANOS);

    private StartupMetrics() {
    }

    public static void mark(String phase) {
        mark(phase, null);
    }

    public static void mark(String phase, String detail) {
        long now = System.nanoTime();
        long fromStartMs = (now - APP_START_NANOS) / 1_000_000L;
        long sinceLastMs = (now - LAST_PHASE_NANOS.getAndSet(now)) / 1_000_000L;
        PHASE_ELAPSED_MS.put(phase, fromStartMs);
        String thread = Thread.currentThread().getName();
        if (detail == null || detail.isBlank()) {
            log.info("startup phase={} elapsedMs={} sinceLastMs={} thread={}",
                    phase, fromStartMs, sinceLastMs, thread);
        } else {
            log.info("startup phase={} elapsedMs={} sinceLastMs={} detail={} thread={}",
                    phase, fromStartMs, sinceLastMs, detail, thread);
        }
    }

    public static void markTool(String phase, String toolId, long costMs) {
        log.info("startup phase={} elapsedMs={} tool={} thread={}",
                phase, costMs, toolId, Thread.currentThread().getName());
        PHASE_ELAPSED_MS.put(phase + "." + toolId, costMs);
    }

    public static long millisSinceStart() {
        return (System.nanoTime() - APP_START_NANOS) / 1_000_000L;
    }

    public static Long phaseElapsedMs(String phase) {
        return PHASE_ELAPSED_MS.get(phase);
    }
}
