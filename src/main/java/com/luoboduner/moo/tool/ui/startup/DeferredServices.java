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

    /** 对齐 Next：窗口就绪后再稍等片刻，避免与首屏争抢网络/CPU。 */
    private static final long INITIAL_DELAY_SECONDS = 3;
    private static final long PERIOD_SECONDS = 60 * 60;

    private static final AtomicReference<ScheduledFuture<?>> UPDATE_CHECK = new AtomicReference<>();

    private DeferredServices() {
    }

    public static void start() {
        stop();
        if (!App.config.isAutoCheckUpdate()) {
            log.info("Auto update check disabled by setting");
            return;
        }
        // StartupCoordinator 已延后约 1.5s 调用本方法；此处再等几秒后做首次检查，之后每小时一次
        ScheduledFuture<?> future = AppExecutors.deferred().scheduleAtFixedRate(
                () -> {
                    try {
                        log.info("Running auto update check");
                        UpgradeUtil.checkUpdate(true);
                    } catch (Exception e) {
                        log.debug("Auto update check failed: {}", e.toString());
                    }
                },
                INITIAL_DELAY_SECONDS,
                PERIOD_SECONDS,
                TimeUnit.SECONDS);
        UPDATE_CHECK.set(future);
        log.info("Scheduled auto update check: first in {}s, then every {}s",
                INITIAL_DELAY_SECONDS, PERIOD_SECONDS);
    }

    public static void stop() {
        ScheduledFuture<?> future = UPDATE_CHECK.getAndSet(null);
        if (future != null) {
            future.cancel(false);
        }
    }
}
