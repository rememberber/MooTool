# DIFF-400：恢复备份原地重载工具会话

## 背景

DIFF-399 清空 `SessionManager` 缓存后，分离窗 `remember { sessionManager.jsonSession() }` 仍持有旧 `JsonSession` 实例，界面不更新。

## 行为

- `reloadAllToolSessionsFromStore` 改为在**已缓存**会话上 `restore` SQLite 快照，不清空 map/缓存。
- JSON/随手记重载前 `resetVaultScopedOverlays()`（逻辑下沉到 `JsonSession`/`QuickNoteSession`，`VaultRootChange` 委托调用）。
- 新增 `sessionGeneration`；Host 正文 `TextFieldValue` 在 generation 变化时同步（Compose 局部状态示例）。
- `SessionManagerReloadTest` 断言重载后仍为同一 `JsonSession` 引用。

## 验收

- A03；`desktopTest --offline`。
