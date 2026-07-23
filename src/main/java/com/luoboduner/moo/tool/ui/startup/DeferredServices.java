package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.UpgradeUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 延后非关键服务的统一启停。
 */
@Slf4j
public final class DeferredServices {

    private static final AtomicReference<ScheduledFuture<?>> UPDATE_CHECK = new AtomicReference<>();

    private DeferredServices() {
    }

    public static void start() {
        stop();
        if (!App.config.isAutoCheckUpdate()) {
            return;
        }
        // 首次延迟，避免与首屏争抢网络/CPU
        ScheduledFuture<?> future = AppExecutors.deferred().scheduleAtFixedRate(
                () -> {
                    try {
                        UpgradeUtil.checkUpdate(true);
                    } catch (Exception e) {
                        log.debug("Auto update check failed: {}", e.toString());
                    }
                },
                5,
                60,
                TimeUnit.MINUTES);
        UPDATE_CHECK.set(future);
    }

    public static void stop() {
        ScheduledFuture<?> future = UPDATE_CHECK.getAndSet(null);
        if (future != null) {
            future.cancel(false);
        }
    }
}
