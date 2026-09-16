# DIFF-349：随手记导出对齐 Electron 正文与默认文件名

## 问题

Electron `exportNote` 将 **纯笔记正文**（`state.content`）另存为 `标题.txt`（当前打开时用编辑器缓冲）。Compose 工具栏/Vault **导出** 曾 `Files.copy` 整份 vault 文件或写入 frontmatter 序列化，外部文件与 Electron 不一致。

## 行为

- `quickNoteExportBody`：当前打开路径用 `session.editor.text`，否则 `readNote(...).content`。
- `exportQuickNoteVaultEntryToPath` 始终 UTF-8 写正文（不再复制 vault 磁盘文件）。
- 另存对话框默认名：`quickNoteExportDefaultFileName`（有标题 → `标题.txt`，否则用路径 leaf）。

## 验证

- `QuickNoteVaultExportTest`
- `./gradlew :composeApp:desktopTest --offline`
