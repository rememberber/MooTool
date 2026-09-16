# DIFF-284：切页清除 RSTA 查找高亮

## 问题

JSON/随手记 `findOpen` 为会话态；切到其他工具时 Compose 卸载，但 `EditorBuffer` 上 `markMatches` 高亮仍可能留在 RSTA，直到再次进入并 `sync`。

## 行为

- `EditorFindHighlight.ClearOnDispose`：工具页 `onDispose` 在 EDT 调用 `clearMatches`。
- JSON、随手记主屏接入。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
