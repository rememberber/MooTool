# DIFF-300：JSON 历史恢复与查找替换 copy 反馈对齐 Electron

## 背景

Electron `JsonTool`：

- `onApplyHistory` 仅 `update({ content, historyOpen: false })`，不写 `json.notice.restored`。
- `replaceCurrent` / `replaceAllMatches` 成功时 `update({ …, notice: '', copyState: 'idle' })`。

Compose 历史恢复多写「已从历史恢复」；「全部替换」经 `setText` 不触发 `onUserDocumentChange`，复制按钮可能仍显示「已复制」。

## 行为

- **F04**：`HistoryBrowser` 恢复 JSON 正文后只关历史对话框并 `clearJsonPathQueryResult()`，保留既有 `notice`（与 Electron 直接改 `content` 一致）。
- 查找条「替换」「全部替换」成功时显式 `copyState = idle`（单次替换与 Electron 一致；全部替换补齐 `setText` 路径）。

## 验证

- 对照 `next/src/features/json/JsonTool.tsx` `onApplyHistory` / `replaceCurrent`
- `./gradlew :composeApp:desktopTest --offline`
