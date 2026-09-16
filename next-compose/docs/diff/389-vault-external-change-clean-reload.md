# DIFF-389：Vault 磁盘变更干净时总重载当前打开文件

## 背景

Electron `onJsonVaultChange` 在 `!dirty` 时 `reloadSelectedFromDisk()`，不依赖变更路径列表。Compose `handle*VaultChange` 仅在 `currentFile in paths` 时处理，其它文件变更导致当前片段磁盘更新时不会重载。

## 行为

- 提取 `jsonVaultApplyExternalChange` / `quickNoteApplyExternalChange`：干净时走 `*ReloadOpenFileIfClean`；脏时仍要求 `currentFile in paths` 再 `VaultConflictEngine.decide`。
- 脏文件重载统一 `loadJsonVaultSnippet` / `quickNoteOpenVaultFile`。

## 测试

- `JsonVaultExternalChangeTest.cleanOpenFileReloadsWhenChangeNotInPathsList`

## 验收

- F01/F04 Vault；`desktopTest --offline`。
