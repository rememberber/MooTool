# DIFF-268：RSTA 编辑器内 Cmd/Ctrl+R 查找

## 对照 Electron

编辑器焦点在 CodeMirror/RSTA 时，窗口级 `keydown` 可能到不了 Compose；Electron 在编辑器组件上同样监听 `f`/`r` 快捷键。

## 行为

- `EditorBuffer.bindAppShortcuts` 将 `VK_R` + menu 修饰键绑定到与 `VK_F` 相同的 `ACTION_FIND` 回调（JSON/随手记/HTTP 等 `EditorHost`）。

## 验证

- `EditorBufferShortcutTest`
- `./gradlew :composeApp:desktopTest --offline`

## 关联

- 壳层快捷键见 [DIFF-267](267-find-replace-shortcut-ctrl-r.md)
