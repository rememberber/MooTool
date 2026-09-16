# DIFF-382：JSON Vault 刷新重载当前打开文件

## 背景

Electron `JsonVaultPanel` 在 `load()`/刷新且编辑器干净时调用 `reloadSelectedFromDisk()`，从磁盘重读 `selectedPath`；文件已删则清空编辑器。Compose「刷新 Vault」仅重建树索引，不重载已打开片段。

## 行为

- `jsonVaultReloadOpenFileIfClean`：干净时读盘并 `loadJsonVaultSnippet`；缺失则 `clearJsonVaultOpenSession`。
- 工具栏/更多菜单「刷新 Vault」、切回 JSON 且干净时复用该逻辑（切回仍写 `vault.conflict.reloaded` notice）。

## 测试

- `JsonVaultReloadOpenFileTest`

## 验收

- F04 Vault；`desktopTest --offline`。
