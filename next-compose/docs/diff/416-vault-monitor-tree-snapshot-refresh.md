# DIFF-416：Vault 磁盘监视触发文档库树 snapshot 刷新

## 背景

Electron `configureJsonVaultWatcher` / Quick Note 监视在防抖 180ms 后 `broadcast('*-vault:changed')`，Vault 面板 `load()` 刷新树。Compose `VaultRevisionMonitor` 轮询已能检测外部写入并走 `handle*VaultChange` 重载干净编辑器，但 **未** 重抓 `snapshot`，外部新建/删除文件在树中不可见直至手动刷新或切页（DIFF-415 才补上按钮/Git 路径）。

## 行为

- JSON / 随手记 `VaultRevisionMonitor` 回调在 `handle*VaultChange` 前调用 `notifyJsonVaultTreeChanged()` / `notifyQuickNoteVaultTreeChanged()`，与 [DIFF-415](415-vault-git-manual-refresh-snapshot.md) 共用 tick → `LaunchedEffect` 重载 `snapshot` + `persistFilter`。

## 验证

- 既有 `VaultConflictEngineTest.snapshotDiffAndPollingSeeExternalWrites`
- `./gradlew :composeApp:desktopTest --offline`
