# DIFF-405：备份/导入重载后 Compose 正文字段同步

## 背景

DIFF-400 在 `sessionGeneration` 递增时原地 `restore` 工具会话。Host 方案正文、文本对比左右栏使用 `remember { TextFieldValue(...) }` 局部状态，仅依赖 `session.content` / `session.left` 变化时偶发未与 SQLite 快照对齐（例如重载后字段值与局部缓存一致性的边界）。

## 行为

- Host：`contentField` 同步 `LaunchedEffect` 增加 `sessionGeneration` 依赖。
- 文本对比：左右 `TextFieldValue` 同步 `LaunchedEffect` 增加 `sessionGeneration` 依赖。

## 验收

- F02/F10、A03（与 DIFF-404 重载链路配合）；`./gradlew :composeApp:desktopTest --offline`。
