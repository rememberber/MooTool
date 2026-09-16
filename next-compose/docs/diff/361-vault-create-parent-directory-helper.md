# DIFF-361：Vault 新建默认父路径复用 import 目标目录辅助

## 问题

[DIFF-202](202-json-vault-create-under-selection.md) / [DIFF-203](203-quicknote-vault-create-under-selection.md) 已按选中路径解析父目录。[DIFF-350](350-quicknote-vault-import-selection-path.md) / [DIFF-354](354-json-editor-drop-import-selection-dir.md) 将拖放/导入目标提取为 `*VaultImportTargetDirectory`（`vaultSelectedPath.ifBlank { currentFile }` → `parentDirectory`）。JSON「新建」按钮与随手记「新建笔记」对话框仍手写相同逻辑，易漂移。

## 行为

- JSON Vault「新建片段/文件夹」默认路径：`jsonVaultImportTargetDirectory` + `VaultSelectionPath.join`。
- 随手记「新建笔记」`createNote(parentPath = …)`：`quickNoteVaultImportTargetDirectory`。
- `resolveEntryPath` / 树拖放等既有路径不变。

## 验证

- `JsonVaultImportTest`、`QuickNoteVaultImportTest`
- `./gradlew :composeApp:desktopTest --offline`
