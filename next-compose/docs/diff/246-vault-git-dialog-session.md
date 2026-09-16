# DIFF-246：Vault Git 面板会话状态

## 对照 Electron

Electron `JsonVaultPanel` / `QuickNoteTool` 将 `gitDialogOpen` / `gitOpen` 保存在模块级会话状态，分离窗口重建 UI 时仍保持 Git 面板打开。

## 行为

- `JsonSession.gitDialogOpen`、`QuickNoteSession.gitDialogOpen` 替代 Compose `remember` 局部状态。
- 主窗切页时 Git 面板会话字段保留（DIFF-261，对齐 Electron）；主窗因分离销毁占位时不执行切页清理（DIFF-244）。

## 验证

- `ToolModalOverlaysTest`（JSON / 随手记 `gitDialogOpen`）
- `./gradlew :composeApp:desktopTest --offline`
