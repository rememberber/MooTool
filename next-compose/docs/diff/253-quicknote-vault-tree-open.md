# DIFF-253：随手记文档库列 `treeOpen` 对齐

## 对照 Electron

`QuickNoteTool` 的 `treeOpen` 可在宽布局下折叠左侧文档库；状态保存在会话中，并参与分栏 `storageKey`。

## 行为

- `QuickNoteSession.vaultTreeOpen`（默认 `true`），写入 `QuickNoteSessionSnapshot`。
- 工具栏首组切换按钮（文案对齐 `quickNote.openVault` / `quickNote.vault`）。
- `LayoutPolicy.showQuickNoteVault` 组合窄窗 `compactAux` 与 `vaultTreeOpen`。

## 验证

- `LayoutPolicyTest`、`QuickNoteSessionSnapshotTest`
- `./gradlew :composeApp:desktopTest --offline`
