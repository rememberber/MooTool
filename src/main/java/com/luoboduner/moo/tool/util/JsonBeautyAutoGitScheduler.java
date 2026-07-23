package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * JSON Vault 空闲 / 失焦后触发 Git 检查点。
 */
@Slf4j
public final class JsonBeautyAutoGitScheduler {

    private static final int TICK_MS = 5000;

    private static volatile long lastActivityAt = System.currentTimeMillis();
    private static volatile long windowDeactivatedAt;
    private static volatile long lastIdleCheckpointForActivity;
    private static volatile long lastInactiveCheckpointAt;
    private static Timer tickTimer;
    private static boolean started;
    private static volatile boolean checkpointInProgress;
    private static java.awt.event.AWTEventListener windowListener;

    private JsonBeautyAutoGitScheduler() {
    }

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        tickTimer = new Timer(TICK_MS, e -> evaluateCheckpoint());
        tickTimer.setRepeats(true);
        tickTimer.start();
        attachWindowListener();
    }

    public static void stop() {
        if (tickTimer != null) {
            tickTimer.stop();
            tickTimer = null;
        }
        detachWindowListener();
        checkpointInProgress = false;
        started = false;
    }

    public static void recordActivity() {
        lastActivityAt = System.currentTimeMillis();
    }

    private static void attachWindowListener() {
        if (windowListener != null) {
            return;
        }
        windowListener = event -> {
            if (event.getID() == WindowEvent.WINDOW_DEACTIVATED
                    && event instanceof WindowEvent windowEvent
                    && windowEvent.getWindow() == App.mainFrame) {
                windowDeactivatedAt = System.currentTimeMillis();
            } else if (event.getID() == WindowEvent.WINDOW_ACTIVATED
                    && event instanceof WindowEvent windowEvent
                    && windowEvent.getWindow() == App.mainFrame) {
                windowDeactivatedAt = 0;
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(windowListener, AWTEvent.WINDOW_EVENT_MASK);
    }

    private static void detachWindowListener() {
        if (windowListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(windowListener);
            windowListener = null;
        }
    }

    private static void evaluateCheckpoint() {
        if (!App.config.isJsonBeautyAutoGitCommit()) {
            return;
        }
        if (checkpointInProgress) {
            return;
        }
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        if (!QuickNoteGitUtil.isGitRepo(vaultDir)) {
            return;
        }
        if (JsonBeautyGitCheckpoint.isGitCheckpointBlocked(vaultDir)) {
            return;
        }
        if (JsonBeautyVaultRefreshCoordinator.hasUnsavedChanges()) {
            return;
        }
        if (!JsonBeautyGitCheckpoint.hasPendingCheckpointWork(vaultDir)) {
            return;
        }

        long now = System.currentTimeMillis();
        int idleSeconds = App.config.getJsonBeautyAutoGitIdleSeconds();
        int inactiveSeconds = App.config.getJsonBeautyAutoGitInactiveSeconds();

        boolean idleReady = now - lastActivityAt >= idleSeconds * 1000L
                && lastIdleCheckpointForActivity < lastActivityAt;
        boolean inactiveReady = windowDeactivatedAt > 0
                && now - windowDeactivatedAt >= inactiveSeconds * 1000L
                && lastInactiveCheckpointAt < windowDeactivatedAt;

        if (!idleReady && !inactiveReady) {
            return;
        }

        final boolean runForIdle = idleReady;
        final boolean runForInactive = inactiveReady;
        final long idleActivitySnapshot = lastActivityAt;
        final long inactiveSnapshot = windowDeactivatedAt;

        checkpointInProgress = true;
        new SwingWorker<QuickNoteGitUtil.GitCommandResult, Void>() {
            @Override
            protected QuickNoteGitUtil.GitCommandResult doInBackground() {
                return JsonBeautyGitCheckpoint.runAutomaticCheckpoint();
            }

            @Override
            protected void done() {
                try {
                    QuickNoteGitUtil.GitCommandResult result = get();
                    if (result.isSuccess()) {
                        if (runForIdle) {
                            lastIdleCheckpointForActivity = idleActivitySnapshot;
                        }
                        if (runForInactive) {
                            lastInactiveCheckpointAt = inactiveSnapshot;
                        }
                    } else if (StringUtils.isNotBlank(result.getMessage())) {
                        log.debug("JSON auto git checkpoint skipped: {}", result.getMessage());
                    }
                } catch (Exception ex) {
                    log.debug("JSON auto git checkpoint failed: {}", ex.getMessage());
                } finally {
                    checkpointInProgress = false;
                }
            }
        }.execute();
    }
}
