# DIFF-357：通用历史恢复前保存脏 Vault 片段

## 问题

[DIFF-356](356-json-toolbar-import-flush.md) 已在 JSON 工具栏导入前 flush。`HistoryBrowser` 恢复仍直接 `setText` 覆盖编辑器：JSON/随手记当前打开 Vault 条目且未保存时，恢复会丢编辑或让磁盘与缓冲不一致。

## 行为

- **JSON**：`onRestore` 前先 `flushJsonVaultEditorIfDirty`；失败则提示并保持历史对话框打开。
- **随手记**：`onRestore` 前先 `saveIfNeeded`；失败则写 `error` 并保持历史对话框打开。
- 仍不写 `json.notice.restored`（见 [DIFF-303](303-history-restore-no-restored-notice.md)）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
