# DIFF-399：恢复备份后重载工具会话

## 背景

zip 恢复会覆盖 SQLite 中的 `tool_sessions` 与磁盘 `data/`。`SessionManager` 在首次访问时缓存各工具会话；若不丢弃缓存，用户仍看到恢复前的内存草稿，保存时可能再次写坏已恢复的数据。提示「建议退出重开」不足以闭环。

## 行为

- `SessionManager.reloadAllToolSessionsFromStore()`：取消在途任务并在缓存会话上重载 SQLite（DIFF-400 起不再清空缓存）。
- `AppContainer.restoreBackup` 在设置重载与 DB `rebind` 之后调用上述方法。

## 测试

- `SessionManagerReloadTest`

## 验收

- A03；`desktopTest --offline`。
