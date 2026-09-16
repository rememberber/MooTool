# DIFF-261：JSON/随手记切页保留模态会话

## 对照 Electron

- `JsonTool.tsx` 的 `jsonSessionState` 保留 `historyOpen`、`pathPickerOpen`、`inputConversion`、`outputDialog` 等。
- `JsonVaultPanel.tsx` 的 `jsonVaultSessionState` 保留 `gitDialogOpen`、`textAction`、`moveOpen`、`contextMenu` 等。
- `QuickNoteTool.tsx` 的 `quickNoteSessionState` 保留 `gitOpen`、`infoOpen`、`actionMode`、查找条等。

切到其它工具时 React 卸载 UI，但模块级会话不变；回到 JSON/随手记后遮罩与对话框恢复。

## 行为（next-compose）

- `JsonSession.dismissModalOverlays` / `QuickNoteSession.dismissModalOverlays` 改为 no-op（不再在 `DismissModalOverlaysOnDispose` 中清空 Git、历史、JSONPath 选择器、Vault 对话框、右键菜单路径等）。
- 遮罩仍只在对应 `JsonScreen` / `QuickNoteScreen` 组合时绘制，不会挡住其它工具页。
- 其它工具（HTTP、Host、图片等）仍按 DIFF-232～234 在切页时关闭模态。

## 验证

- `ToolModalOverlaysTest`（JSON / 随手记保留会话字段）
- `./gradlew :composeApp:desktopTest --offline`

## 关联

- 修正 DIFF-232 与 DIFF-246 中「主窗切页关闭 Git 面板」的表述；分离窗守卫仍见 DIFF-244。
