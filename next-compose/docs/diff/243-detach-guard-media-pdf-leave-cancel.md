# DIFF-243：图片/调色板分离守卫与 PDF 切页取消处理

## 背景

DIFF-237/236 已在图片/调色板切页取消协程与采屏 overlay。主窗分离工具时 `ImageScreen`/`ColorBoardScreen` 的 `onDispose` 仍会取消 `imageWorkJob`/`screenPickJob` 并清空会话遮罩，导致分离窗无法延续进行中的批处理或取色。

PDF 拆分/合并使用 `session.cancelled` 协作取消，但主窗切页未置位，后台协程可能继续写会话。

## 行为

- **图片**：`DismissModalOverlaysOnDispose` 仅在 `!isDetached(Image)` 时取消 `imageWorkJob` 与 `dismissModalOverlays`。
- **调色板**：同上，`!isDetached(ColorBoard)`。
- **PDF**：`!isDetached(Pdf)` 时切页置 `cancelled=true`（`busy` 时）并关闭模态；与工具栏「取消」一致。

## 验证

- `./gradlew :composeApp:desktopTest --offline`

续 [DIFF-242](242-tool-leave-cancel-coderun-regex-hardware.md) 分离守卫约定；全工具模态关闭见 [DIFF-244](244-dismiss-modals-unless-detached.md)。
