# DIFF-252：JSONPath 选择器高亮会话态

## 行为

- `JsonSession.pathPickerSelection` 保存路径选择器内当前高亮路径；与已有 `pathPickerOpen` 一起在分离窗重建时保持打开中的选择状态。
- 主窗切页 `dismissModalOverlays()` 关闭选择器并清空 `pathPickerSelection`。

## 验证

- `ToolModalOverlaysTest`
- `./gradlew :composeApp:desktopTest --offline`
