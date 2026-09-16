# DIFF-350：随手记 Vault 工具栏导入目录与相对路径

## 问题

[DIFF-118](118-vault-drop-toast-quicknote.md) 规定 Vault 树拖放入当前选中目录并用 `jsonVaultEntryRelativePath` 打开。工具栏/「更多」导入仍 `importFile` 到 vault 根且用 `Path.fileName` 打开，在子目录选中时路径错误，与树拖放不一致。

## 行为

- `quickNoteVaultImportTargetDirectory`：与树拖放相同，按 `vaultSelectedPath.ifBlank { currentFile }` 解析父目录。
- `importQuickNoteVaultFile`：写入目标目录并返回 portable 相对路径。
- 工具栏/「更多」导入复用 `runQuickNoteVaultImport`（含 `saveIfNeeded`、导出目录持久化、toast）。

## 验证

- `QuickNoteVaultImportTest`、`QuickNoteVaultExportTest.exportBodyUsesEditorWhenCurrentFile`
- `./gradlew :composeApp:desktopTest --offline`
