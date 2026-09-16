# DIFF-235：调色板切页取消屏幕取色

## 背景

F22 屏幕取色使用全屏 `ScreenColorPicker` overlay。用户在采屏过程中切走工具页时，overlay 与 `picking` 状态可能残留，返回后取色按钮仍显示「处理中」。

## 行为

- `ScreenColorPicker.dismissActive()`：关闭当前取色层（等同 Esc 取消）。
- `ColorSession.dismissModalOverlays()` 增加 `picking = false` 并调用 `dismissActive()`。
- `ColorBoardScreen` 在 `onDispose` 取消进行中的采屏协程。

## 验证

- `ToolModalOverlaysTest.color_dismissModalOverlays_clears_picking_and_dialogs`
- `./gradlew :composeApp:desktopTest --offline`
