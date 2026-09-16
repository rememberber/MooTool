# DIFF-215：JSON Vault 树右键前置保存与重命名预填

## 背景

Electron `JsonVaultPanel` 在 `beginRename` 预填去掉 `.json` 的文件名；对其它条目操作前若当前片段脏则需先保存（`openFile` 语义）。Compose 树右键未保存即开对话框，重命名预填含 `.json` 扩展名。

## 行为

- `prepareJsonVaultContext`：更新 `vaultSelectedPath`；当目标为其它文件且当前编辑器脏时先 `saveJsonVault`（无 toast）。
- `jsonVaultRenameDefault` / `jsonVaultRenameDefaultFromFileName`：与 Electron `leafName(...).replace(/\.json$/i, '')` 一致。
- JSON Vault 文本对话框主按钮在输入为空时禁用（移动除外）。

## 验证

- `JsonVaultRenameDefaultTest`
- `./gradlew :composeApp:desktopTest --offline`
