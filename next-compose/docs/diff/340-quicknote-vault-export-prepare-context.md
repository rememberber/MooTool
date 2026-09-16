# DIFF-340：随手记 Vault 导出前 `prepareQuickNoteVaultContext`

## 问题

[DIFF-339](339-vault-export-editor-buffer.md) 修正导出内容来源，但随手记 Vault 右键 **导出** 未像重命名/移动/删除一样调用 `prepareQuickNoteVaultContext`。导出其它笔记时若当前文档脏，可能未先保存；与 JSON Vault（上下文动作统一 `prepareJsonVaultContext`）及 Electron `exportNote` → `selectNode` 不一致。

## 行为

- 随手记 Vault 上下文 **Export**：先 `prepareQuickNoteVaultContext`（必要时 `saveIfNeeded` 并打开目标笔记），再弹出另存对话框并 `exportQuickNoteVaultEntryToPath`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
