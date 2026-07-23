package com.luoboduner.moo.tool.ui.startup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LazyToolManagerTest {

    private LazyToolManager manager;
    private JPanel container;

    @Before
    public void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            manager = LazyToolManager.getInstance();
            manager.disposeAll();
            container = new JPanel();
        });
    }

    @After
    public void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> manager.disposeAll());
        AppExecutors.shutdown();
    }

    @Test
    public void concurrentEnsureInitializedLoadsOnce() throws Exception {
        AtomicInteger loadCount = new AtomicInteger();
        AtomicInteger createCount = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> manager.register("demo", new LazyToolInitializer<String>() {
            @Override
            public String loadData() {
                loadCount.incrementAndGet();
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "ok";
            }

            @Override
            public JComponent createView() {
                createCount.incrementAndGet();
                return new JLabel("demo");
            }

            @Override
            public void bindData(JComponent view, String data) {
                ready.countDown();
            }
        }, container));

        Thread t1 = new Thread(() -> manager.ensureInitialized("demo"));
        Thread t2 = new Thread(() -> manager.ensureInitialized("demo"));
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        assertEquals(ToolLoadState.READY, manager.stateOf("demo"));
        assertEquals(1, loadCount.get());
        assertEquals(1, createCount.get());
    }

    @Test
    public void failureThenRetrySucceeds() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> manager.register("flaky", new LazyToolInitializer<String>() {
            @Override
            public String loadData() throws Exception {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("boom");
                }
                return "ok";
            }

            @Override
            public JComponent createView() {
                return new JLabel("flaky");
            }

            @Override
            public void bindData(JComponent view, String data) {
                ready.countDown();
            }
        }, container));

        manager.ensureInitialized("flaky");
        awaitState(ToolLoadState.FAILED, 5);
        manager.retry("flaky");
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        assertEquals(ToolLoadState.READY, manager.stateOf("flaky"));
        assertEquals(2, attempts.get());
    }

    @Test
    public void loadDataNotOnEdtAndBindOnEdt() throws Exception {
        AtomicBoolean loadOnEdt = new AtomicBoolean(true);
        AtomicBoolean bindOnEdt = new AtomicBoolean(false);
        CountDownLatch ready = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> manager.register("thread-check", new LazyToolInitializer<Void>() {
            @Override
            public Void loadData() {
                loadOnEdt.set(SwingUtilities.isEventDispatchThread());
                return null;
            }

            @Override
            public JComponent createView() {
                return new JLabel("x");
            }

            @Override
            public void bindData(JComponent view, Void data) {
                bindOnEdt.set(SwingUtilities.isEventDispatchThread());
                ready.countDown();
            }
        }, container));

        manager.ensureInitialized("thread-check");
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        assertFalse(loadOnEdt.get());
        assertTrue(bindOnEdt.get());
    }

    private void awaitState(ToolLoadState expected, int seconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (manager.stateOf("flaky") == expected) {
                return;
            }
            Thread.sleep(20);
        }
        assertEquals(expected, manager.stateOf("flaky"));
    }
}
