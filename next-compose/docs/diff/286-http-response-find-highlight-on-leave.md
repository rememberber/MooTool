# DIFF-286：HTTP 响应区切页清除查找高亮

## 问题

与 [DIFF-284](284-editor-find-highlight-on-leave.md) 相同：`findOpen` 为会话态时，离开 HTTP 工具后 `responseEditor` 上 `markMatches` 高亮可能残留。

## 行为

- HTTP 主屏接入 `EditorFindHighlight.ClearOnDispose(session.responseEditor)`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
