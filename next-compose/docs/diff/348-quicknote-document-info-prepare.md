# DIFF-348：随手记文档信息前 `prepareQuickNoteVaultContext`

## 问题

Vault 右键 **文档信息** 自行 `readNote` + `saveIfNeeded` + 条件 `openFile`，与导出/复制/重命名共用的 `prepareQuickNoteVaultContext` 不一致，也不对齐 Electron `showDocumentInfo` → `selectNode`。

## 行为

- 右键 **文档信息**：先 `prepareQuickNoteVaultContext`（必要时保存并打开目标笔记），再设置 `documentInfoPath` 打开 overlay。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
