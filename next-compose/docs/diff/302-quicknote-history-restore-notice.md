# DIFF-302：随手记历史恢复不写 restored notice

## 背景

[DIFF-300](300-json-history-replace-copy-state.md) 已对齐 Electron `JsonTool.onApplyHistory`（仅恢复正文并关对话框）。Compose Host 历史恢复本就只写 `session.content`（见 `HostScreen` `HistoryBrowser`）。随手记仍写入 `json.notice.restored`。

Electron `QuickNoteTool` 无通用历史对话框；本产品 F01 保留 `HistoryBrowser`，恢复行为应与 JSON/Host 一致：**不**额外写恢复成功 notice。

## 行为

- **F01**：`HistoryBrowser` 恢复后 `setText` + 关闭历史；保留既有 `notice`（`setText` 不触发 `onUserDocumentChange`）。

## 验证

- 对照 `next/src/features/json/JsonTool.tsx` `onApplyHistory` 与 Host `HistoryBrowser` 恢复
- `./gradlew :composeApp:desktopTest --offline`
