# DIFF-241：HTTP / 翻译切页取消在途请求

## 背景

离开工具页（主窗切到其他导航）时应取消未完成的网络请求，避免回调在会话已不可见时仍写入状态。分离窗口继续工作时主窗只显示占位，不应取消；分离窗关闭会先 `reattach` 再销毁，此时应取消。

对齐网络工具（`NetScreen` 在 `!isDetached` 时 `onDispose` 取消命令）与 Electron 离开页面不再等待结果的行为。

## 行为

- **翻译**：`TranslationScreen` 在 `onDispose` 且 `!isDetached(Translation)` 时递增 `sequence` 并调用既有 `cancelActive()`（`TranslationEngine.cancel`）。
- **HTTP**：`onDispose` 且 `!isDetached(Http)` 时 `HttpEngine.cancel(requestId)` 并清除 `sending`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`

续见 [DIFF-242](242-tool-leave-cancel-coderun-regex-hardware.md)（代码运行 / 正则 / 系统信息）。
