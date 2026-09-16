# DIFF-378：Vault prepare 失败回滚优先当前打开文件 + 随手记树统一 prepare

## 背景

树单击已高亮目标文件、双击打开时若保存冲突，`prepare*VaultContext` 用 `vaultSelectedPath` 作回滚基准会留在未打开的文件上，与仍打开的 `currentFile` 不一致。

随手记 Vault 树 `onSelect`/`onOpen` 与 `prepareQuickNoteVaultContext` 逻辑重复。

## 行为

- JSON/随手记 `prepare*VaultContext`：flush 失败时 `vaultSelectedPath = currentFile.ifBlank { vaultSelectedPath }`。
- 随手记树：目录 `onSelect` 与文件 `onOpen` 走 `prepareQuickNoteVaultContext`。

## 测试

- `QuickNoteVaultPrepareContextTest.prepareContextSaveConflictRevertsTreeToOpenFileNotPendingHighlight`
- 既有 prepare 冲突单测仍通过

## 验收

- F01/F04 Vault；`desktopTest --offline`。
