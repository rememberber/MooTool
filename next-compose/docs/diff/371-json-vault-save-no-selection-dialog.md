# DIFF-371：JSON Vault 无选中时保存打开新建对话框

## 背景

Electron `JsonVaultPanel.saveSelected` 在 `!selectedPath` 时调用 `beginCreateFile()`，不静默写入磁盘。

Compose `saveJsonVault` 在 `currentFile` 为空时曾写入根目录 `draft.json`，与 Electron 及 [DIFF-210](210-json-vault-new-file-dialog.md) 新建流程不一致。

## 行为

- `openJsonVaultNewFileDialog`：与「新建」按钮共用默认 `parent/snippet.json`。
- `jsonVaultSaveFromUserAction`：无 `currentFile` 时打开 `json-file` 对话框；否则走原保存与 `notice`。
- Vault 工具栏「保存」、编辑器 `Cmd/Ctrl+S` 改用上述逻辑。
- `saveJsonVault`：`currentFile` 为空时失败（`json.vault.saveNoSelection`），idle/Git 等路径仍仅在已打开片段时写盘。

## 测试

- `JsonVaultSaveSelectionTest`

## 验收

- F04 Vault 保存；`desktopTest --offline`。
