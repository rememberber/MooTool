package com.rememberber.mootool.nextfx.support;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class FxSupport {

    private static final CountDownLatch STARTED = new CountDownLatch(1);

    static {
        try {
            Platform.startup(STARTED::countDown);
        } catch (IllegalStateException alreadyStarted) {
            STARTED.countDown();
        }
    }

    private FxSupport() {
    }

    public static void run(ThrowingRunnable action) {
        try {
            if (!STARTED.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX toolkit did not start");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(60, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for JavaFX work");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
