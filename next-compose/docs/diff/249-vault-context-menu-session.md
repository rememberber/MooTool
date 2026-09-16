# DIFF-249：Vault 树右键菜单会话态

## 对照 Electron

`JsonVaultPanel` / `QuickNoteTool` 将 `contextMenu` 保存在模块级会话；分离窗重建 UI 时不应仅因 Compose 占位销毁而丢失已打开的菜单状态。

## 行为

- `JsonSession.vaultContextMenuPath`、`QuickNoteSession.vaultContextMenuPath` 由 `VaultTreeList` 读写。
- 主窗切页时 `dismissModalOverlays()` 关闭菜单；分离占位销毁不执行（DIFF-244）。

## 验证

- `ToolModalOverlaysTest`
- `./gradlew :composeApp:desktopTest --offline`
