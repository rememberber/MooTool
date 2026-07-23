package com.luoboduner.moo.tool.ui.startup;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 启动与页面加载使用的有界线程池。
 */
@Slf4j
public final class AppExecutors {

    private static final AtomicInteger IO_SEQ = new AtomicInteger();
    private static final AtomicInteger COMPUTE_SEQ = new AtomicInteger();
    private static final AtomicInteger DEFERRED_SEQ = new AtomicInteger();

    private static volatile ExecutorService ioExecutor;
    private static volatile ExecutorService computeExecutor;
    private static volatile ScheduledExecutorService deferredExecutor;

    private AppExecutors() {
    }

    public static ExecutorService io() {
        ExecutorService local = ioExecutor;
        if (local == null) {
            synchronized (AppExecutors.class) {
                local = ioExecutor;
                if (local == null) {
                    ioExecutor = local = Executors.newFixedThreadPool(3, namedDaemonFactory("mootool-io-", IO_SEQ));
                }
            }
        }
        return local;
    }

    public static ExecutorService compute() {
        ExecutorService local = computeExecutor;
        if (local == null) {
            synchronized (AppExecutors.class) {
                local = computeExecutor;
                if (local == null) {
                    int n = Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() - 1));
                    computeExecutor = local = Executors.newFixedThreadPool(n, namedDaemonFactory("mootool-compute-", COMPUTE_SEQ));
                }
            }
        }
        return local;
    }

    public static ScheduledExecutorService deferred() {
        ScheduledExecutorService local = deferredExecutor;
        if (local == null) {
            synchronized (AppExecutors.class) {
                local = deferredExecutor;
                if (local == null) {
                    deferredExecutor = local = Executors.newSingleThreadScheduledExecutor(
                            namedDaemonFactory("mootool-deferred-", DEFERRED_SEQ));
                }
            }
        }
        return local;
    }

    public static void shutdown() {
        shutdownQuietly(ioExecutor);
        shutdownQuietly(computeExecutor);
        shutdownQuietly(deferredExecutor);
        ioExecutor = null;
        computeExecutor = null;
        deferredExecutor = null;
    }

    private static void shutdownQuietly(ExecutorService executor) {
        if (executor == null) {
            return;
        }
        try {
            executor.shutdownNow();
        } catch (Exception e) {
            log.warn("Failed to shutdown executor: {}", e.toString());
        }
    }

    private static ThreadFactory namedDaemonFactory(String prefix, AtomicInteger seq) {
        return r -> {
            Thread t = new Thread(r, prefix + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }
}
