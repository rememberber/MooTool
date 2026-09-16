# DIFF-236：图片工具切页取消区域截图

## 背景

F23 区域截图使用全屏 `ScreenRegionPicker`（与 F22 取色同属录屏 overlay）。采屏/框选过程中切走图片页时，overlay 与 `busy` 可能残留。

## 行为

- `ScreenRegionPicker.dismissActive()`：关闭当前区域选择层（等同 Esc 取消）。
- `ImageSession.dismissModalOverlays()` 增加 `busy = false` 并调用 `dismissActive()`。
- `ImageScreen` 在 `onDispose` 取消进行中的截图协程。

## 验证

- `ToolModalOverlaysTest.host_and_image_dismissModalOverlays_clears_flags`（含 `busy`）
- `./gradlew :composeApp:desktopTest --offline`
