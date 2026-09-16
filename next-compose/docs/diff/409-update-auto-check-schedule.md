# DIFF-409：自动检查更新调度对齐 Electron

## 背景

Electron `configureUpdateChecks` 在 `autoCheckUpdates` 开启时：启动 **2.5s** 后首次 `checkForUpdatesAndBroadcast`，之后每 **60 分钟** 重复；关闭设置时清除定时器。Compose 此前仅在 `startBackgroundTasks` **立即**检查一次，无周期调度，关闭「启动后检查更新」后也无法停止已发起的检查任务链。

## 行为

- `UpdateAutoCheckScheduler`：`STARTUP_DELAY_MS = 2500`、`INTERVAL_MS = 3600000`。
- `AppContainer.configureAutomaticUpdateChecks()` 在启动与 `autoCheckUpdates` 变更时 `reconfigure`；`close()` 时 `stop()`。
- 每次检查仍走 `UpdateCoordinator.check(autoDownload = …, automatic = true)`（UI 静默策略见 [DIFF-410](410-update-automatic-check-silent.md)）。

## 验证

- `UpdateAutoCheckSchedulerTest`
- `./gradlew :composeApp:desktopTest --offline`
