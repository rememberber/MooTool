# DIFF-256：Host 方案右键菜单会话态与首项焦点

## 对照 Electron

`HostTool` 方案列表 `onContextMenu` 打开 `role="menu"`，并在 `requestAnimationFrame` 后聚焦第一项（重命名/导出/删除）。菜单状态在工具会话中保持，分离窗重建不应丢失。

## 行为

- `HostSession.profileContextMenuId` 保存当前打开的右键菜单对应方案 id；`dismissModalOverlays()` 与主窗切页时清空。
- 菜单项与 Electron 一致：重命名、导出、删除（复制保留在底栏，不进菜单）。
- `MooMenuItem` 支持 `modifier`；Vault 树与 Host 方案菜单打开时首项 `FocusRequester`。
- i18n 增加 `common.rename`（中/英）。

## 验证

- `ToolModalOverlaysTest`
- `./gradlew :composeApp:desktopTest --offline`
