package com.luoboduner.moo.tool.ui.startup;

import cn.hutool.core.io.FileUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.FuncTabCatalog;
import com.luoboduner.moo.tool.ui.Init;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import com.luoboduner.moo.tool.util.UpgradeUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 启动协调器：窗口可见后加载当前页，并延后非关键服务。
 */
@Slf4j
public final class StartupCoordinator {

    private static final StartupCoordinator INSTANCE = new StartupCoordinator();

    @Getter
    private final AtomicReference<StartupPhase> phase = new AtomicReference<>(StartupPhase.NEW);

    private volatile boolean started;
    private volatile CompletableFuture<Void> bootstrapFuture =
            CompletableFuture.completedFuture(null);

    private StartupCoordinator() {
    }

    public static StartupCoordinator getInstance() {
        return INSTANCE;
    }

    public StartupPhase currentPhase() {
        return phase.get();
    }

    /**
     * 数据库/临时目录等最小后台引导是否已完成。
     */
    public CompletableFuture<Void> bootstrapReady() {
        return bootstrapFuture;
    }

    /**
     * 必须在主窗口 setVisible 之后、尽快从 EDT 调用。
     * 只提交后台/后续 EDT 任务，自身不做阻塞初始化。
     */
    public void startAfterWindowVisible() {
        EdtGuard.assertEdt();
        if (started) {
            return;
        }
        started = true;
        phase.set(StartupPhase.SHELL_VISIBLE);
        StartupMetrics.mark("mainFrame.visible");

        phase.set(StartupPhase.CRITICAL_LOADING);
        StartupMetrics.mark("critical.loading");

        int recentIndex = App.config.getRecentTabIndex();
        String initialToolId = resolveInitialToolId(recentIndex);
        StartupMetrics.mark("initialTool.loading", initialToolId);

        MainWindow.getInstance().selectTabWithoutInit(recentIndex);

        bootstrapFuture = CompletableFuture.runAsync(this::prepareBackgroundBootstrap, AppExecutors.io());
        bootstrapFuture.whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
            if (error != null) {
                log.error("Background bootstrap failed", error);
            }
            onCriticalReady();
            LazyToolManager.getInstance().ensureInitialized(initialToolId);
        }));

        AppExecutors.deferred().schedule(this::startDeferredServices, 1500, TimeUnit.MILLISECONDS);
    }

    /**
     * Tab 切换时调用：等待 bootstrap 完成后 single-flight 初始化。
     */
    public void ensureToolWhenReady(String toolId) {
        bootstrapFuture.whenComplete((ignored, error) ->
                SwingUtilities.invokeLater(() -> LazyToolManager.getInstance().ensureInitialized(toolId)));
    }

    private void prepareBackgroundBootstrap() {
        StartupMetrics.mark("bootstrap.db.begin");
        App.sqlSession = MybatisUtil.getSqlSession();
        UpgradeUtil.smoothUpgrade();
        StartupMetrics.mark("bootstrap.db.ready");
        prepareTempDir();
        StartupMetrics.mark("bootstrap.tempDir.ready");
    }

    private void prepareTempDir() {
        File tempDir;
        if (SystemUtil.isLinuxOs()) {
            tempDir = new File(SystemUtil.CONFIG_HOME + File.separator + "temp");
        } else {
            tempDir = new File(FileUtil.getTmpDirPath() + "MooTool");
        }
        if (!tempDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tempDir.mkdirs();
        }
        FileUtil.clean(tempDir);
        App.tempDir = tempDir;
    }

    private void onCriticalReady() {
        EdtGuard.assertEdt();
        if (phase.get().ordinal() < StartupPhase.INTERACTIVE.ordinal()) {
            phase.set(StartupPhase.INTERACTIVE);
            StartupMetrics.mark("app.interactive");
        }
        AppExecutors.deferred().schedule(() -> SwingUtilities.invokeLater(() -> {
            try {
                Init.languageGuide();
                Init.fontSizeGuide();
            } catch (Exception e) {
                log.error("Startup guide failed", e);
            }
        }), 800, TimeUnit.MILLISECONDS);
    }

    private void startDeferredServices() {
        phase.set(StartupPhase.DEFERRED_RUNNING);
        StartupMetrics.mark("deferredServices.started");
        try {
            Init.startDeferredServices();
        } catch (Exception e) {
            log.error("Deferred services failed", e);
        } finally {
            phase.set(StartupPhase.READY);
            StartupMetrics.mark("app.ready");
        }
    }

    public static String resolveInitialToolId(int recentIndex) {
        return FuncTabCatalog.byIndex(recentIndex)
                .map(FuncTabCatalog.FuncTab::id)
                .orElse("mootool");
    }

    public void shutdown() {
        LazyToolManager.getInstance().disposeAll();
        AppExecutors.shutdown();
        EdtLagMonitor.stop();
    }
}
