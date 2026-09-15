# DIFF-041：文档库/Host 右键菜单、HTTP 分栏与分隔条键盘

- 编号：DIFF-041
- 影响：F01/F04 文档库、F10 Host、F09 HTTP、ui-spec 右键与分隔条
- 日期：2026-09-15

## 原行为（Electron）

JSON Vault 与随手记树右键：重命名/移动/复制/删除/在文件管理器显示/Git。Host 方案列表右键：重命名/导出/删除。HTTP 请求与响应可调高度。分隔条可用键盘微调。

## 本产品行为

- 文档库右键走已有 Vault CRUD：重命名、移动、复制（仅文件）、导出（仅文件）、删除、在文件管理器显示、Git。操作目标是被右键的节点，不要求先选中当前编辑文件。
- Host 方案右键：重命名（真实写入 `profiles.json`）、复制、导出、删除。
- HTTP 请求区与响应区用水平分隔条，高度写入 `layout.paneSizes.http[1]`。
- 分隔条获焦点后方向键每次 16dp；双击仍恢复默认。

## 理由

目标要求对照 Electron 补齐布局与工作流。此前这些动作只在选中项的底栏按钮上，树/列表没有右键；HTTP 响应高度不可调。

## 证据

`VaultTreeTest.contextMenuHidesFileOnlyActionsOnDirectories`、`VaultMoveTest` 路径重定向、`PaneHandleTest`。`desktopTest` **212/212**，见 `docs/evidence/2026-09-15-context-menu-panes/`。窗口手势、IME 手工、三平台安装仍未测。

## 受影响范围

- 在文件管理器显示依赖 `Desktop.browseFileDirectory`，不支持时回退打开父目录。
- 仍非 Electron 六套 CSS 逐选择器移植。
