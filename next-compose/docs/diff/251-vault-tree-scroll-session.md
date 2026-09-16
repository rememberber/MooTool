# DIFF-251：Vault 树滚动位置会话态

## 对照 Electron

`jsonVaultTreeScrollTop` / `quickNoteTreeScrollTop` 为模块级变量，分离窗重建文档库树时恢复 `scrollTop`。

## 行为

- `JsonSession.vaultTreeScrollOffset`、`QuickNoteSession.vaultTreeScrollOffset`。
- `VaultTreeList` 通过 `treeScrollOffset` / `onTreeScrollOffsetChange` 与 `ScrollState` 同步。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
