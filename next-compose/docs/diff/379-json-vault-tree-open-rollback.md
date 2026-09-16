# DIFF-379：JSON Vault 树打开文件 flush 失败回滚选中

## 背景

树单击会先 `onSelect` 更新 `vaultSelectedPath`，再 `onOpen` flush 并加载。flush 失败时选中仍指向目标文件，与 `currentFile` 不一致（随手记/Electron `openFile` 失败语义见 DIFF-378）。

## 行为

- 提取 `openJsonVaultTreeFile`：flush 失败时 `vaultSelectedPath = currentFile.ifBlank { vaultSelectedPath }`；成功则 `loadJsonVaultSnippet`。
- `JsonScreen` Vault 树 `onOpen` 使用该辅助函数。

## 测试

- `JsonVaultTreeOpenTest.openTreeFileSaveConflictRevertsSelectionToOpenFile`

## 验收

- F04 Vault；`desktopTest --offline`。
