# DIFF-339：Vault 导出当前打开文件的编辑器内容

## 问题

Electron 导出随手记时使用 `state.content`（当前打开则取编辑器缓冲区）。Compose Vault 右键/工具栏 **导出** 一律 `Files.copy` 磁盘文件；当前片段/笔记有未保存修改时，导出内容落后于编辑器。

JSON 工具栏导出已写 `session.editor.text`；**Vault 树导出**仍读盘。

## 行为

- `vaultExportText` / `vaultExportUsesEditorBuffer`：导出路径等于 `currentFile` 且 `editorText != savedText` 时用缓冲区。
- JSON：`exportJsonVaultEntryToPath` 供 Vault 上下文导出。
- 随手记：`exportQuickNoteVaultEntryToPath`（含 metadata 脏）用于工具栏与 Vault 导出；`quickNoteDirty` 迁至 `QuickNoteVaultExport.kt`。

## 验证

- `VaultExportContentTest`
- `./gradlew :composeApp:desktopTest --offline`
