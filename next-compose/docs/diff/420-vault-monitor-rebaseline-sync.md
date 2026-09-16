# DIFF-420：Vault 监视器 rebaseline 同步与加锁

## 背景

[DIFF-419](419-vault-monitor-rebaseline-on-reload.md) 用 `LaunchedEffect` rebaseline，仍可能与后台轮询线程竞态：导入写入磁盘后、rebaseline 前的一帧内轮询可把整库当作外部变更。监视器 `previous` 与 worker 更新亦未互斥。

## 行为

- `VaultRevisionMonitor`：`stateLock` 保护 `previous` 更新与 `rebaseline()`；轮询在锁内 diff 并提交 `previous = next`。
- `RebBaselineVaultMonitorOnSessionReload`：`SideEffect` 在 `sessionGeneration` 或监视器实例变化时立即 rebaseline（JSON/随手记共用）。

## 验证

- 复用 `VaultConflictEngineTest.rebaselineAfterBulkImportAvoidsReplayDiff`
- `./gradlew :composeApp:desktopTest --offline`
