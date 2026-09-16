# DIFF-377：随手记 prepare 保存失败回滚树选中

## 背景

JSON [DIFF-375](375-json-vault-directory-prepare-selection-rollback.md)～[DIFF-376](376-json-vault-file-prepare-selection-rollback.md) 在 `prepareJsonVaultContext` flush 失败时回滚 `vaultSelectedPath`。

随手记 `prepareQuickNoteVaultContext` 在开头写 `vaultSelectedPath`，`saveIfNeeded` 失败时仍指向目录/目标笔记，与 [DIFF-360](360-quicknote-directory-select-save.md) 树单击目录语义不一致。

## 行为

- 目录/切换笔记：先 `saveIfNeeded`；成功后再更新 `vaultSelectedPath` 并清空或 `open`；失败恢复 flush 前选中（空则 `currentFile`），**不**清空编辑器。

## 测试

- 更新 `QuickNoteVaultPrepareContextTest.prepareContextSaveConflictWritesErrorAndReturnsFalse`
- 新增 `prepareContextDirectorySaveConflictKeepsTreeSelection`

## 验收

- F01 Vault；`desktopTest --offline`。
