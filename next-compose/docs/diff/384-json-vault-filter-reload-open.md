# DIFF-384：JSON Vault 筛选/搜索刷新时重载打开文件

## 背景

随手记 [DIFF-383](383-quicknote-vault-refresh-reload-open.md) 在 `persistFilter` 调用 `quickNoteReloadOpenFileIfClean`。Electron JSON `load()` 在树依赖（keyword/sort 等）变化且干净时 `reloadSelectedFromDisk()`。Compose JSON 仅在工具栏「刷新 Vault」重载，搜索/排序变更不重读磁盘。

## 行为

- `JsonScreen.persistFilter` 在递增 `filterRev` 前调用 `jsonVaultReloadOpenFileIfClean`（脏文档跳过）。

## 测试

- 复用 `JsonVaultReloadOpenFileTest`；行为与 DIFF-382 同一辅助函数。

## 验收

- F04 Vault；`desktopTest --offline`。
