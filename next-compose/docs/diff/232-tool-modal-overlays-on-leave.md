# DIFF-232：离开 JSON/随手记页时关闭模态遮罩

## 背景

JSONPath 选择器、结果对话框、Vault 输入对话框、历史浏览器等以会话字段 + `MooOverlay` 实现。主窗切换到其它工具时 `JsonScreen`/`QuickNoteScreen` 会卸载，但 `pathPickerOpen` 等仍为 `true`，返回工具时会意外重新弹出遮罩。

## 行为

- `JsonSession.dismissModalOverlays` / `QuickNoteSession.dismissModalOverlays` 清除上述瞬时 UI 状态（保留查找条等可持久字段）。
- 两工具页 `DisposableEffect` 在 `onDispose` 时调用。

## 验证

- `ToolModalOverlaysTest`
- `./gradlew :composeApp:desktopTest --offline`
