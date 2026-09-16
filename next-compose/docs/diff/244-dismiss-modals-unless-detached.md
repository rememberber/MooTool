# DIFF-244：切页关模态统一 `!isDetached` 守卫

## 背景

DIFF-234/243 在部分工具上单独加了 `isDetached` 判断。JSON/随手记/HTTP 等仍在主窗分离占位销毁时调用 `dismissModalOverlays()`，会关掉分离窗里仍应显示的 JSONPath 选择器、Vault 对话框、留言板展示等共享会话状态。

## 行为

- `DismissModalOverlaysOnDispose(container, toolId) { … }` 仅在 `!sessionManager.isDetached(toolId)` 时执行回调。
- 全工具 `*Screen` 与 JSON/随手记/HTTP 统一接入；图片/调色板/PDF 的额外清理（协程取消、`cancelled`）纳入同一回调。

## 验证

- `ToolModalLifecycleTest`
- `./gradlew :composeApp:desktopTest --offline`

续：`OnToolLeaveUnlessDetached` / `shouldRunToolLeaveCleanup` 供 DIFF-241～243 在途任务取消复用（见 `ToolModalLifecycle.kt`）。
