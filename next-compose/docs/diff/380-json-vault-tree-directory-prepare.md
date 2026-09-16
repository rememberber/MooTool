# DIFF-380：JSON Vault 树目录单击走 `prepareJsonVaultContext`

## 背景

树目录单击原直接 `jsonVaultFlushDirtyOrNotice`；flush 失败时不回滚 `vaultSelectedPath`。若用户已单击高亮其它文件，失败后会仍指向该文件而非仍打开的 `currentFile`（DIFF-378 已在 prepare 内修复）。

## 行为

- 目录 `onSelect` 改为 `prepareJsonVaultContext`（干净文档直接选中目录；脏文档 flush 后选中；失败回滚）。
- 文件 `onSelect` 仍仅更新 `vaultSelectedPath`，由 `onOpen`/`openJsonVaultTreeFile` 加载。

## 测试

- `JsonVaultPrepareContextTest.prepareContextDirectoryFlushConflictRevertsHighlightToOpenFile`

## 验收

- F04 Vault；`desktopTest --offline`。
