# DIFF-269：HTTP 响应查找预填选区

## 对照 Electron

`HttpTool.openFind` 读取响应 `TextCodeEditor` 当前选区，打开查找条时写入 `find` 字段。快捷键为 `Cmd/Ctrl+F`（无 `R`），且忽略请求区 `.http-request-pane` 内按键。

## 行为

- `openHttpResponseFind` 通过 `openFindBarSeedingSelection(session.responseEditor)` 预填 `findQuery`
- 壳层 `Cmd/Ctrl+F`（请求 Body 编辑器聚焦时不触发，见 [DIFF-270](270-http-find-ignore-request-body-focus.md)）、响应区「查找」按钮与响应 `EditorHost` 查找快捷键均走该入口

## 验证

- `./gradlew :composeApp:desktopTest --offline`
