package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.QuickNoteGitPullResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.io.File;

/**
 * 后台定时从远程拉取更新（对标 Tolaria useAutoSync）。
 */
@Slf4j
public final class QuickNoteAutoPullScheduler {

    private static final int TICK_MS = 30_000;

    private static volatile long lastPullAt;
    private static volatile boolean pullInProgress;
    private static Timer tickTimer;
    private static boolean started;

    private QuickNoteAutoPullScheduler() {
    }

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        lastPullAt = System.currentTimeMillis();
        tickTimer = new Timer(TICK_MS, e -> evaluatePull());
        tickTimer.setRepeats(true);
        tickTimer.start();
    }

    public static void stop() {
        if (tickTimer != null) {
            tickTimer.stop();
            tickTimer = null;
        }
        started = false;
        pullInProgress = false;
    }

    private static void evaluatePull() {
        int intervalMinutes = App.config.getQuickNoteAutoPullIntervalMinutes();
        if (intervalMinutes <= 0) {
            return;
        }
        File vaultDir = QuickNoteVaultUtil.getVaultDir();
        if (!QuickNoteGitUtil.isGitRepo(vaultDir) || !QuickNoteGitUtil.hasRemote(vaultDir)) {
            return;
        }
        if (QuickNoteGitCheckpoint.isGitCheckpointBlocked(vaultDir)) {
            return;
        }
        if (QuickNoteVaultRefreshCoordinator.hasUnsavedChanges()) {
            return;
        }
        if (pullInProgress) {
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMs = intervalMinutes * 60_000L;
        if (now - lastPullAt < intervalMs) {
            return;
        }

        pullInProgress = true;
        new SwingWorker<QuickNoteGitPullResult, Void>() {
            @Override
            protected QuickNoteGitPullResult doInBackground() {
                return QuickNoteGitUtil.pullWithResult(vaultDir);
            }

            @Override
            protected void done() {
                pullInProgress = false;
                lastPullAt = System.currentTimeMillis();
                try {
                    QuickNoteGitPullResult result = get();
                    QuickNoteVaultRefreshCoordinator.refreshAfterPull(result);
                    if (!result.isSuccess() && StringUtils.isNotBlank(result.getMessage())) {
                        log.debug("Auto git pull skipped: {}", result.getMessage());
                    }
                } catch (Exception ex) {
                    log.debug("Auto git pull failed: {}", ex.getMessage());
                }
            }
        }.execute();
    }
}
