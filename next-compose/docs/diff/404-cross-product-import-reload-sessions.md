# DIFF-404：跨产品导入后重载工具会话

## 背景

DIFF-399/400 在**恢复备份**后调用 `reloadAllToolSessionsFromStore`，使 JSON/随手记/HTTP 等监听 `sessionGeneration` 的 UI 与磁盘一致。设置 **跨产品导入**（`CrossProductImporter.apply`）会写入 Vault、SQLite 历史/集合/Host 等，但此前未重载内存会话，与 Electron 导入后刷新工作区不一致。

## 行为

- 导入成功后在主线程调用 `AppContainer.reloadToolSessionsFromStore()`（与 `restoreBackup` 共用入口；Vault 树 notify 见 [DIFF-418](418-reload-sessions-vault-tree-notify.md)）。
- 迁移面板 `sessionGeneration` 变化时仅清空扫描预览与确认框，**保留** `importNotice`/`importError`，避免重载后立刻擦掉导入成功提示（DIFF-403 备份场景仍清空预览）。

## 验收

- A01/A03；`./gradlew :composeApp:desktopTest --offline`。
