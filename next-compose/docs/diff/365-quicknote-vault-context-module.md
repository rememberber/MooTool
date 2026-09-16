# DIFF-365：随手记 Vault 上下文模块与 prepare 单测

## 背景

JSON Vault 已在 [DIFF-364](364-json-vault-prepare-context-extract-test.md) 将 `prepareJsonVaultContext` 提取到 `JsonVaultSession.kt` 并加冲突单测。随手记 `prepareQuickNoteVaultContext` 与 `saveIfNeeded` / `saveCurrent` / `openFile` 仍堆在 `QuickNoteScreen.kt`，不便回归且与 JSON 不对称。

## 变更

- 新增 `QuickNoteVaultContext.kt`：`prepareQuickNoteVaultContext`、`quickNoteVaultSaveIfNeeded`、`quickNoteSaveCurrent`、`quickNoteOpenVaultFile`、`quickNoteOnEdt`。
- `prepare` 开头同步 `vaultSelectedPath`（对齐 JSON `prepareJsonVaultContext`）。
- `QuickNoteScreen.kt` 改为调用上述 internal API，删除重复实现。
- 新增 `QuickNoteVaultPrepareContextTest`：脏笔记 + 磁盘正文冲突时 prepare 失败、`error`/`vaultConflict` 非空。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
