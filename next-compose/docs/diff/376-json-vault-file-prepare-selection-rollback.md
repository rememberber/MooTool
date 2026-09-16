# DIFF-376：JSON 切换片段 prepare flush 失败回滚树选中

## 背景

Electron `openFile` 在脏文档保存失败时不改 `selectedPath`。[DIFF-375](375-json-vault-directory-prepare-selection-rollback.md) 已修正目录条目；**文件**条目在 `prepareJsonVaultContext` 仍先写 `vaultSelectedPath` 再 flush，失败时底栏指向目标片段而编辑器仍打开原片段。

## 行为

- 非目录条目：仅在 flush 成功或无需 flush 时更新 `vaultSelectedPath`；切换前 flush 失败回滚至 flush 前选中（空则 `currentFile`）。

## 测试

- 更新 `JsonVaultPrepareContextTest.prepareContextSaveConflictWritesNoticeAndReturnsFalse` 期望 `vaultSelectedPath == open.json`

## 验收

- F04 Vault；`desktopTest --offline`。
