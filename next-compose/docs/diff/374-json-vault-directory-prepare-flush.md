# DIFF-374：JSON Vault 目录上下文/树选中前 flush 脏片段

## 背景

随手记选中目录前会 `saveIfNeeded`（[DIFF-360](360-quicknote-directory-select-save.md)）。JSON `prepareJsonVaultContext` 曾将 `entry.directory` 与「已是当前文件」一并 early-return，**目录**右键/底栏在编辑器脏时不写盘。

树 **单击目录** 仅改 `vaultSelectedPath`，脏片段可能一直未落盘直至 idle 保存。

## 行为

- `prepareJsonVaultContext`：目标为**目录**时，若已打开片段且脏则静默 `saveJsonVault`；失败写 `notice` 并返回 `false`（不清空编辑器，对齐 JSON 目录选中语义）。
- Vault 树 `onSelect`：单击**目录**前 `jsonVaultFlushDirtyOrNotice`，失败不改选中。

## 测试

- `JsonVaultPrepareContextTest.prepareContextDirectoryEntryFlushesDirtyOpenFile`

## 验收

- F04 Vault；`desktopTest --offline`。
