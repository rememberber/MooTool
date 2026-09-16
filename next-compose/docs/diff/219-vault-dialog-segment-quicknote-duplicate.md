# DIFF-219：Vault 对话框单段路径与随手记复制前保存

## 背景

- JSON Vault 新建文件/文件夹对话框若用户只输入 `snippet.json` 或文件夹名（无 `/`），应落在当前选中目录下（与默认全路径预填、DIFF-202 行为一致）。
- 随手记复制与 JSON DIFF-218 同理：`duplicate` 读盘，当前笔记脏时需先保存。

## 行为

- `VaultSelectionPath.resolveEntryPath`：无 `/` 的输入拼到 `parentDirectory(vaultSelectedPath|currentFile)` 下。
- JSON `json-file` / `json-folder` 提交使用该解析。
- `duplicateQuickNoteEntry`：`saveIfNeeded` 后 `duplicate` 并 `openFile`。

## 验证

- `VaultSelectionPathTest.resolveSingleSegmentUnderSelectedDirectory`
- `./gradlew :composeApp:desktopTest --offline`
