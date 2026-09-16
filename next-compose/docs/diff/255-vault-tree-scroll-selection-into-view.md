# DIFF-255：Vault 树选中滚入视口

## 对照 Electron

`JsonVaultPanel` / `QuickNoteTool` 在 `selectedPath` 或树节点变化后调用 `scrollSelectedIntoView`（`scrollIntoView({ block: 'nearest' })`），保证新建/选中深层文件时行仍可见。

## 行为

- `VaultTreeList` 对当前 `selectedPath` 行挂 `BringIntoViewRequester`，在选中路径、条目集合或展开状态变化后 `bringIntoView()`。
- 与已有 `vaultTreeScrollOffset` 会话态互补：用户手动滚动仍写回 offset；程序性选中会滚到最近可见区。
- `jsonEditorAutoFocusEnabled` / `quickNoteEditorAutoFocusEnabled` 在 `vaultContextMenuPath` 非空时不抢焦点（避免窗口激活关闭右键菜单）。

## 验证

- `JsonEditorFocusPolicyTest`、`QuickNoteEditorFocusPolicyTest`
- `./gradlew :composeApp:desktopTest --offline`
