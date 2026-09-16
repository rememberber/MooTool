# DIFF-366：随手记 Vault Git flush 跳过干净文档

## 问题

JSON Vault Git 已在 [DIFF-332](332-json-git-flush-skip-clean.md) 在 `editor.text == savedText` 时跳过 `onFlush` 写盘。随手记 `onFlush` 每次仍调用 `quickNoteVaultSaveIfNeeded`（虽因 `quickNoteDirty` 很快返回），未显式跳过；且 metadata 与正文一致时应对齐 `isVaultEditorDirty()` / Electron `dirty`（含 metadata）。

## 行为

- 提取 `quickNoteGitFlushBeforeAction`：`currentFile` 为空时沿用未命名守卫；已命名且 `!quickNoteDirty(session)` 时返回 `null`（不写盘）；脏时 `quickNoteVaultSaveIfNeeded`，失败返回 `error` 或 `quickNote.saveFailed`。
- `QuickNoteScreen` Git 对话框 `onFlush` 委托上述函数。

## 验证

- `QuickNoteGitFlushTest`
- `./gradlew :composeApp:desktopTest --offline`
