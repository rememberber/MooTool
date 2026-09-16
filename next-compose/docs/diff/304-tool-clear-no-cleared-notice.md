# DIFF-304：多工具「清空」不写 `json.notice.cleared`

## 背景

[DIFF-299](299-json-toolbar-clear-notice.md) 已对齐 JSON 工具栏清空与 Electron `update({ content: '' })`。格式化、编码、配置转换、UA 解析等工具清空仍写入 `json.notice.cleared`。

Electron 对应清空仅重置字段，例如 `ReformatTool` 文本/文件 Tab `setText('')`、`UaParseTool` `setSource(''); setResult(null)`，无状态栏 cleared 文案。

## 行为

- **F03 格式化**：`clearOutput` 仍 `clearTab` + 清 `error`，不写 `notice`。
- **F13 编码**：工具栏/溢出「清空」仅 `clearCurrent()`。
- **F06 配置**：溢出清空仅清各 Tab 字段与 `error`。
- **F12 UA**：清空仅清 `source`/`result`/`error`。

## 验证

- 对照 `next/src/features/reformat/ReformatTool.tsx`、`encode/EncodeTool.tsx`、`ua/UaParseTool.tsx`
- `./gradlew :composeApp:desktopTest --offline`
