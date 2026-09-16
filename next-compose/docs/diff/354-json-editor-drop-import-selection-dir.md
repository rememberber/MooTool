# DIFF-354：JSON 编辑器拖入 `.json` 按树选中目录入库

## 问题

[DIFF-207](207-vault-drop-selection-footer.md) 规定 Vault **树**拖入使用 `vaultSelectedPath.ifBlank { currentFile }` 解析父目录。[DIFF-116](116-json-editor-file-drop.md) 编辑器拖入 `.json` 仍 `importFile` 到 vault 根，子目录选中时与树拖放不一致。

## 行为

- `jsonVaultImportTargetDirectory` / `importJsonVaultFile`：与随手记 [DIFF-350](350-quicknote-vault-import-selection-path.md) 对称。
- 编辑器拖入 `.json`：flush 后写入选中目录并打开 portable 相对路径。
- Vault 树多文件拖放复用 `importJsonVaultFile`（逻辑不变）。

## 验证

- `JsonVaultImportTest`
- `./gradlew :composeApp:desktopTest --offline`
