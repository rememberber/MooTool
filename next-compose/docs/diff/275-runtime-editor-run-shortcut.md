# DIFF-275：代码运行编辑器 Cmd/Ctrl+Enter 运行

## 对照 Electron

`RuntimeTool` 全局 `keydown`：`meta/ctrl + Enter` 在未运行时调用 `runCode()`。

## 行为

- 壳层 `onPreviewKeyEvent` 已有相同逻辑。
- `EditorHost` 增加 `EditorAppShortcuts(onSend)`，RSTA 聚焦时走 `ACTION_SEND`（与 HTTP Body 一致）。

## 验证

- `EditorBufferShortcutTest` 覆盖 Enter 菜单快捷绑定
- `./gradlew :composeApp:desktopTest --offline`
