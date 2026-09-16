# DIFF-237：图片工具切页取消批处理任务

## 背景

DIFF-236 已处理区域截图 overlay。压缩/水印/SVG 矢量化仍在后台协程中写库，切页后可能继续执行并弹出 toast/历史。

## 行为

- `ImageScreen` 用统一 `imageWorkJob` 托管截图、批处理与矢量化协程；`onDispose` 取消 Job。
- `processImages` / `vectorize` 返回 `Job`，循环内 `ensureActive()`；`CancellationException` 静默收尾，不写错误态。
- 工具栏「取消」仍走既有 `session.cancelled` + `ImageException("cancelled")` 用户可见提示。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
