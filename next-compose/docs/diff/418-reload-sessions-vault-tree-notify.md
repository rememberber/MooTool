# DIFF-418：`reloadToolSessionsFromStore` 统一刷新 Vault 树

## 背景

[DIFF-404](404-cross-product-import-reload-sessions.md) 跨产品导入与 [DIFF-417](417-backup-restore-vault-tree-refresh.md) 备份恢复均调用 `reloadToolSessionsFromStore()`，但仅递增 `sessionGeneration`；导入写入 JSON/随手记 Vault 文件后，文档库树仍可能显示旧 `snapshot`，直至手动刷新或切页。

## 行为

- `AppContainer.reloadToolSessionsFromStore()` 在 `reloadAllToolSessionsFromStore()` 之后调用 `notifyJsonVaultTreeChanged()` / `notifyQuickNoteVaultTreeChanged()`（与 DIFF-415/416 共用 tick）。
- 监视器基线见 [DIFF-419](419-vault-monitor-rebaseline-on-reload.md)。
- `restoreBackup` 不再重复 notify（由上述入口统一承担）。

## 验证

- `VaultTreeRefreshNotifyTest.reloadToolSessionsFromStoreNotifiesVaultTrees`
- `./gradlew :composeApp:desktopTest --offline`
