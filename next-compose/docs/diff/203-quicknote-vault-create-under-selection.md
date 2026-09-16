# DIFF-203：随手记 Vault 新建笔记/文件夹落在当前选中目录

## 背景

Electron `QuickNoteTool.runActionDialog`：`createNote` 使用 `selectedDirectory(selectedPath, selectedKind)` 作为 `parentPath`；`createFolder` 将 `actionValue`（文件夹名）与父目录拼接后 `createQuickNoteFolder`。

Compose 此前新建笔记未传 `parentPath`（总在根目录）；新建文件夹对话框使用整段路径作为 `createDirectory` 参数，且默认值为 `folder`，与 Electron（空默认值 + 仅输入文件夹名）不一致。

## 行为

- 复用 `VaultSelectionPath.parentDirectory` / `join`（与 DIFF-202 JSON Vault 同一语义）。
- 「新建笔记」：对话框默认空；保存时 `createNote(title, parentPath = parent)`。
- 「新建文件夹」：对话框默认空；保存时 `createDirectory(join(parent, name))`，选中路径更新为新目录并清空编辑区（对齐选中目录节点）。
- 父目录基于 `vaultSelectedPath`（否则 `currentFile`）与当前 Vault 树条目。

## 验证

- 既有 `NoteVaultTest`（`parentPath`）、`VaultSelectionPathTest`
- `./gradlew :composeApp:desktopTest --offline`
