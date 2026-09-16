# DIFF-234：全工具切页关闭模态遮罩

## 背景

[DIFF-232](232-tool-modal-overlays-on-leave.md)～[DIFF-233](233-http-modal-overlays-on-leave.md) 仅覆盖 JSON、随手记、HTTP。其余带 `HistoryBrowser` / `MooOverlay` 的工具在切页后同样会残留 `historyOpen`、收藏、确认框等状态。

## 行为

- 为各 `*Session` 增加 `dismissModalOverlays()`（见 `ToolModalOverlays.kt`）。
- 抽取 `DismissModalOverlaysOnDispose`（`ToolModalLifecycle.kt`），在 F02–F25 相关工具页 `onDispose` 调用。
- 留言板离开页时结束 `presenting` 沉浸展示。

## 验证

- `ToolModalOverlaysTest`（含 Host/Image）
- `./gradlew :composeApp:desktopTest --offline`
