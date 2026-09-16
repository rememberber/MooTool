# DIFF-247：Vault overlay 会话态（随手记文档信息 + JSON 删除确认）

## 对照 Electron

`QuickNoteTool` 的 `infoOpen` 保存在模块级会话状态，分离窗口重建 UI 时不应丢失文档信息 overlay。

## 行为

- `QuickNoteSession.documentInfoPath`：非空表示文档信息 overlay 打开。
- `JsonSession.vaultDeleteConfirmPath`：非空表示 Vault 删除确认 overlay。
- 主窗切页时 `dismissModalOverlays()` 清空；分离占位销毁不执行（DIFF-244）。

## 验证

- `ToolModalOverlaysTest`（随手记 `documentInfoPath`、JSON `vaultDeleteConfirmPath`）
- `./gradlew :composeApp:desktopTest --offline`
