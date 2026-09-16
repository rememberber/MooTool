# DIFF-250：Vault 树展开状态会话态

## 对照 Electron

`JsonVaultPanel` / `QuickNoteTool` 将目录 `expanded` 保存在模块级会话；仅在首次加载或用户切换展开模式时按设置重置，分离窗重建 UI 时保留手动折叠/展开。

## 行为

- `JsonSession.vaultTreeExpanded`、`QuickNoteSession.vaultTreeExpanded` 为 `Map<String, Boolean>`。
- `VaultTreeList` 通过 `treeExpanded` / `onTreeExpandedChange` 读写；`vaultTreeExpandForMode` 在空图或展开模式变更时初始化。

## 验证

- `VaultTreeTest.vaultTreeExpandForMode_smart_expandsRootOnly`
- `./gradlew :composeApp:desktopTest --offline`
