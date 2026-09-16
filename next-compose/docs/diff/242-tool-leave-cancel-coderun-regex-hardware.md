# DIFF-242：代码运行 / 正则 / 系统信息切页取消在途任务

## 背景

DIFF-241 已覆盖 HTTP/翻译主窗切页取消网络请求。同类在途任务：代码运行子进程、正则 worker 匹配、系统信息采集协程。

分离工具时主窗占位销毁不应取消仍在分离窗执行的任务（与 `NetScreen` / DIFF-241 相同 `!isDetached` 守卫）。

## 行为

- **F05 代码运行**：`onDispose` 且 `!isDetached(Java)` 时 `CodeRunEngine.cancel(requestId)`（与工具栏「停止」一致）。
- **F15 正则**：`onDispose` 且 `!isDetached(Regex)` 时递增 `matchGeneration`、`regexWorker.cancel()`、清除 `running`。
- **F25 系统信息**：`onDispose` 且 `!isDetached(Hardware)` 时取消采集 `Job` 并清除 `loading`（分离窗继续采集）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`

续见 [DIFF-243](243-detach-guard-media-pdf-leave-cancel.md)（图片/调色板分离守卫、PDF 切页 `cancelled`）。
