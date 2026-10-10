package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.QuickNoteGitPullResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.SwingWorker;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JSON Vault 后台定时从远程拉取更新。
 */
@Slf4j
public final class JsonBeautyAutoPullScheduler {

    private static final int TICK_MS = 30_000;

    private static volatile long lastPullAt;
    private static volatile boolean pullInProgress;
    private static boolean started;
    private static ScheduledExecutorService scheduler;

    private JsonBeautyAutoPullScheduler() {
    }

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        lastPullAt = System.currentTimeMillis();
        scheduler= Executors.newSingleThreadScheduledExecutor(r->{
            Thread thread = new Thread(r, "JsonBeautyAutoPullScheduler");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(()->{
            try {
                evaluatePull();
            }catch (Throwable ex){
                log.debug("JSON auto git pull failed: {}", ex.getMessage());
            }
        },TICK_MS,TICK_MS, TimeUnit.MILLISECONDS);
    }

    public static void stop() {
        if(scheduler !=null){
            scheduler.shutdown();
            try {
                if(!scheduler.awaitTermination(TICK_MS,TimeUnit.MILLISECONDS)){
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }finally {
                scheduler=null;
            }
        }
        started = false;
        pullInProgress = false;
    }

    public static void onSettingsChanged() {
        lastPullAt = System.currentTimeMillis();
    }

    private static void evaluatePull() {
        int intervalMinutes = App.config.getJsonBeautyAutoPullIntervalMinutes();
        if (intervalMinutes <= 0) {
            return;
        }
        File vaultDir = JsonBeautyVaultUtil.getVaultDir();
        if (!QuickNoteGitUtil.isGitRepo(vaultDir) || !QuickNoteGitUtil.hasRemote(vaultDir)) {
            return;
        }
        if (JsonBeautyGitCheckpoint.isGitCheckpointBlocked(vaultDir)) {
            return;
        }
        if (JsonBeautyVaultRefreshCoordinator.hasUnsavedChanges()) {
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
                    JsonBeautyVaultRefreshCoordinator.refreshAfterPull(result);
                    if (!result.isSuccess() && StringUtils.isNotBlank(result.getMessage())) {
                        log.debug("JSON auto git pull skipped: {}", result.getMessage());
                    }
                } catch (Exception ex) {
                    log.debug("JSON auto git pull failed: {}", ex.getMessage());
                }
            }
        }.execute();
    }
}
