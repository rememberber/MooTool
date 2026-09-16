# DIFF-359：JSON Vault 树打开文件前统一 flush

## 问题

[DIFF-356](356-json-toolbar-import-flush.md)～[DIFF-358](358-json-conversion-dialog-flush.md) 已在导入/历史/弹层写入前 flush。Vault 树 **打开**另一片段仍手写 `saveJsonVault`（成功时带保存 toast），与 Git 面板/移动等路径的 `flushJsonVaultEditorIfDirty`（`showToast = false`）不一致。

## 行为

- `VaultTreeList.onOpen`：非目录项打开前 `flushJsonVaultEditorIfDirty`；失败则提示并中止；成功则 `loadJsonVaultSnippet`。
- 无 `currentFile` 或已保存时 flush 为 no-op（与原先「仅脏时保存」一致）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
