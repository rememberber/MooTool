# DIFF-352：JSON Vault 树拖放多文件导入前 flush

## 问题

[DIFF-341](341-json-vault-import-flush-dirty.md) 仅在 Vault 树**单文件**自动打开前 `flushJsonVaultEditorIfDirty`；一次拖入多个 `.json` 只入库不切换时，当前脏片段可能未写盘。随手记对等行为见 [DIFF-351](351-quicknote-vault-drop-save-multi-import.md)。

## 行为

- Vault 树拖放：有有效 `.json` 时，在导入循环前先 flush；失败则中止。
- 单文件打开路径不再重复 flush（已在循环前完成）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
