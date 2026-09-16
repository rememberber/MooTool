# DIFF-415：手动 Git / 刷新 Vault 重载文档库 snapshot

## 背景

Electron `onJsonVaultChange` 在 `json-vault:changed`（含手动/自动 pull、文件监视等）时调用 `load()` 刷新树，并在编辑器干净时 `reloadSelectedFromDisk()`。Compose 自动 pull 已在 DIFF-408 用 tick 重抓 `snapshot` + `persistFilter`；Git 面板 pull/丢弃/继续合并等仅 `persistFilter()`，未重载 `snapshot`，远程新增文件不会出现在树中。「刷新 Vault」按钮同样只 `reloadOpenFileIfClean` + `filterRev`。

## 行为

- `AppContainer.notifyJsonVaultTreeChanged()` / `notifyQuickNoteVaultTreeChanged()`：对齐 `broadcast('*-vault:changed')`，递增与自动 pull 共用的 tick。
- JSON/随手记 Git `onVaultRefresh`、Vault「刷新」按钮与 JSON 更多菜单「刷新」改为调用 notify；`LaunchedEffect` 继续负责 IO `snapshot` + `persistFilter`。
- 磁盘监视见 [DIFF-416](416-vault-monitor-tree-snapshot-refresh.md)。

## 验证

- `VaultTreeRefreshNotifyTest`
- `./gradlew :composeApp:desktopTest --offline`
