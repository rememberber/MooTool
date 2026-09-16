# DIFF-375：JSON 目录 prepare flush 失败回滚树选中

## 背景

[DIFF-374](374-json-vault-directory-prepare-flush.md) 在目录 `prepareJsonVaultContext` 前写 `vaultSelectedPath`，flush 失败时选中仍落在目录上，与树单击目录 flush 失败不改选中不一致，也与随手记 [DIFF-360](360-quicknote-directory-select-save.md) 语义不符。

## 行为

- 目录条目：仅在 flush 成功（或无需 flush）后更新 `vaultSelectedPath`；失败恢复为 flush 前的 `vaultSelectedPath`（空则回退 `currentFile`）。

## 测试

- `JsonVaultPrepareContextTest.prepareContextDirectoryFlushConflictKeepsTreeSelection`

## 验收

- F04 Vault；`desktopTest --offline`。
