# DIFF-200：随手记文档库树选中与状态栏路径

## 背景

Electron `QuickNoteTool.selectNode`：单击目录会先静默保存脏笔记，再清空 `note/content` 并设置 `selectedPath`；单击文件则打开笔记。状态栏展示 `note?.relativePath ?? selectedPath`。

Compose 此前树选中仅跟 `currentFile`，点目录只折叠不更新选中/清空编辑区。

## 行为

- `QuickNoteSession.vaultSelectedPath` 写入快照。
- `VaultTreeList` 使用 `onSelect`、树行脏标记（`activeFilePath`/`activeFileDirty`）。
- 选中目录：必要时 `saveCurrent(showToast=false)`，清空 `currentFile` 与编辑器；选中文件仍走 `openFile`。
- 状态栏路径：`currentFile` 优先，否则 `vaultSelectedPath`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（含 `QuickNoteSessionSnapshotTest`）
- 对照 `next/src/features/quickNote/QuickNoteTool.tsx` `selectNode` 与状态栏
