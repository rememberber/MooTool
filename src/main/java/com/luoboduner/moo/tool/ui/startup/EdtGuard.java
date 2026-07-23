package com.luoboduner.moo.tool.ui.startup;

import javax.swing.SwingUtilities;

/**
 * EDT 线程护栏：断言与安全调度。
 */
public final class EdtGuard {

    private static final boolean ASSERTIONS_ENABLED =
            Boolean.parseBoolean(System.getProperty("mootool.edtAssertions",
                    String.valueOf(Boolean.getBoolean("mootool.dev") || isDevClasspath())));

    private EdtGuard() {
    }

    public static void assertEdt() {
        if (!ASSERTIONS_ENABLED) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Swing access outside EDT: " + Thread.currentThread().getName());
        }
    }

    public static void assertNotEdt() {
        if (!ASSERTIONS_ENABLED) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Blocking work on EDT is not allowed");
        }
    }

    public static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private static boolean isDevClasspath() {
        String path = System.getProperty("java.class.path", "");
        return path.contains("target/classes") || path.contains("out/production");
    }
}
