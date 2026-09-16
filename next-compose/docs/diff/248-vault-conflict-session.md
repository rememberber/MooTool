# DIFF-248：Vault 外部冲突对话框会话态

## 行为

- `JsonSession.vaultConflict`、`QuickNoteSession.vaultConflict` 持有 `VaultConflictState`，替代 Compose `remember`。
- 分离工具窗口时冲突对话框与状态栏横幅不随主窗占位销毁而丢失。
- 主窗切页不自动清空冲突（与 Electron 会话语义一致）；用户须在对话框内处理或关闭。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
