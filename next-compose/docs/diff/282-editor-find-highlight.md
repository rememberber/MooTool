# DIFF-282：JSON / 随手记 RSTA 查找命中高亮

## 对照 Electron

`TextCodeEditor` 在 `findOpen` 时传入 `searchQuery` + `searchOptions`，由 `codeEditorSearchHighlight` 标出全部命中（HTTP 响应区已在 Compose 用 `EditorBuffer.markMatches` 实现）。

## 行为

- `EditorFindHighlight`：`spans` + `sync`，当前命中由编辑器选区/`findMatchAtSelection` 判定。
- JSON、随手记：查找条打开且查询非空时，在 `LaunchedEffect` 中于 EDT 刷新 RSTA 高亮；关闭或空查询时 `clearMatches`。

## 验证

- `EditorFindHighlightTest`
- `./gradlew :composeApp:desktopTest --offline`
