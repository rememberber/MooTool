# DIFF-273：代码运行编辑器 Cmd/Ctrl+Shift+F 格式化

## 对照 Electron

`RuntimeTool` 源码编辑器 `onKeyDown`：`meta/ctrl + shift + f` 调用 `formatSource()`。

## 行为

- `CodeRunScreen.formatSource()`：与工具栏「格式化」共用，运行中不执行。
- `EditorHost` 绑定 `EditorAppShortcuts(onFormat = …)`，走 RSTA `ACTION_FORMAT`（`Cmd/Ctrl+Shift+F`）。

## 验证

- 既有 `EditorBufferShortcutTest` 覆盖 Shift+F 绑定
- `./gradlew :composeApp:desktopTest --offline`
