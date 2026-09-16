# DIFF-413：修改自动 pull 间隔时重置计时

## 背景

Electron `configureJsonVaultAutoPull` / `configureQuickNoteAutoPull` 在设置变更时 `clearInterval` 并新建定时器，间隔修改后立即按新周期计时。Compose `VaultGitPullScheduler` 用 `lastPullAt` 节流，修改 `autoPullMinutes` 后可能仍须等待旧间隔剩余时间。

## 行为

- `VaultGitPullScheduler.resetIntervalClock()` 清零 `lastPullAt`。
- `AppContainer.updateSettings` 在 `vault.autoPullMinutes`、`data.directory` 或 Vault 自定义路径变化时对 JSON/随手记 pull 调度器调用（对齐 `applySettings` 重建 watcher/定时器）。

## 验证

- `VaultGitPullSchedulerTest`
- `./gradlew :composeApp:desktopTest --offline`
