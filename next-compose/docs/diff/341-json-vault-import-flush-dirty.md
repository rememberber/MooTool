# DIFF-341：JSON 拖入 Vault/编辑器打开前 flush 脏片段

## 问题

拖入 `.json` 到编辑器或 Vault 树会 `importFile` 并切换 `currentFile`，此前未对当前打开且未保存的片段写盘，可能丢失编辑（`onOpen` 切换语义应对齐 `saveSelected(false)`）。

## 行为

- 编辑器拖入 `.json` 入库并打开：先 `flushJsonVaultEditorIfDirty`，失败则中止并提示。
- Vault 树拖入且**单文件**自动打开：同样在切换前 flush。
- 多文件拖放导入前 flush 见 [DIFF-352](352-json-vault-drop-flush-multi-import.md)。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
