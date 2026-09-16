# DIFF-345：随手记复制前 `prepareQuickNoteVaultContext`

## 问题

Electron `duplicateNote` 先 `selectNode`（保存当前笔记并打开目标）。Compose 树右键/底栏 **复制** 直接 `duplicateQuickNoteEntry`，仅 `saveIfNeeded` 当前打开文件，未在复制其它笔记前打开目标，与导出/重命名/移动（`prepareQuickNoteVaultContext`）不一致。

## 行为

- Vault 右键 **复制**、底栏 **复制**：先 `prepareQuickNoteVaultContext`（必要时保存并打开目标笔记），再 `duplicateQuickNoteEntry`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
