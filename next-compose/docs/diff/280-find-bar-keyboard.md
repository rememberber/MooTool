# DIFF-280：查找条键盘（对齐 Electron `FindReplaceBar`）

## 对照 Electron

`FindReplaceBar`：查找框 **Enter** → 查找下一处；条内 **↑/↓** → 上/下一处；**Escape** → 关闭；打开时自动聚焦查找框。

## 行为

- `FindBarKeyHandling`：`previewFindBarRowKey` / `previewFindQueryEnter` + `onFindBarRowKeys` / `onFindQueryEnterKey`。
- **Host** / **JSON** / **HTTP** 响应查找条：↑/↓、Esc（条内）、Enter（查找框）；打开条时 `FocusRequester` 聚焦查找框。
- **Host** / **JSON** / **随手记** 工具页：`Escape` 在查找打开时关闭条（HTTP 原有壳层 Esc 保留）。

## 验证

- `FindBarKeyHandlingTest`
- `./gradlew :composeApp:desktopTest --offline`
