# DIFF-419：会话重载后 Vault 监视器 rebaseline

## 背景

[DIFF-418](418-reload-sessions-vault-tree-notify.md) 在跨产品导入/备份恢复后刷新 Vault 树，但 `VaultRevisionMonitor` 仍保留导入前的 `previous` 快照。下一轮轮询会把**整库新文件**当作外部变更，触发多余的 `notify*VaultTreeChanged` 与 `handle*VaultChange`（路径列表可能极大）。

## 行为

- `VaultRevisionMonitor.rebaseline()`：以当前磁盘快照重置 `previous` 并清空 `expectedHashes`。
- JSON/随手记页在 `sessionGeneration` 或监视器实例变化时 `monitor?.rebaseline()`（`RebBaselineVaultMonitorOnSessionReload` 用 `SideEffect` 同步执行，与 `reloadToolSessionsFromStore` 递增代次对齐；轮询线程与 rebaseline 共用 `stateLock`，见 [DIFF-420](420-vault-monitor-rebaseline-sync.md)）。

## 验证

- `VaultConflictEngineTest.rebaselineAfterBulkImportAvoidsReplayDiff`
- `./gradlew :composeApp:desktopTest --offline`
