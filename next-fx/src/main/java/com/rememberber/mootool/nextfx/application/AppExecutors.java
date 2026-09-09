package com.rememberber.mootool.nextfx.application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class AppExecutors implements AutoCloseable {

    private final ExecutorService cpu;
    private final ExecutorService io;

    public AppExecutors() {
        int cores = Runtime.getRuntime().availableProcessors();
        int cpuBound = Math.max(1, Math.min(4, cores - 1));
        this.cpu = Executors.newFixedThreadPool(cpuBound, named("mootool-fx-cpu"));
        this.io = Executors.newThreadPerTaskExecutor(named("mootool-fx-io"));
    }

    public ExecutorService cpu() {
        return cpu;
    }

    public ExecutorService io() {
        return io;
    }

    @Override
    public void close() {
        cpu.shutdownNow();
        io.shutdownNow();
    }

    private static ThreadFactory named(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = Thread.ofPlatform().unstarted(runnable);
            thread.setName(prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
