# DIFF-233：离开 HTTP 页时关闭模态遮罩

## 背景

[DIFF-232](232-tool-modal-overlays-on-leave.md) 覆盖 JSON/随手记。HTTP 工具同样用会话字段驱动历史、cURL 导入、保存集合、删除确认等 `MooOverlay`，切页后返回会误弹。

## 行为

- `HttpSession.dismissModalOverlays` 清除 `historyOpen` / `curlOpen` / `saveOpen` / `deleteConfirm`（保留响应查找条等编辑状态）。
- `HttpScreen` 在 `onDispose` 时调用。

## 验证

- `ToolModalOverlaysTest`（HTTP 用例）
- `./gradlew :composeApp:desktopTest --offline`
