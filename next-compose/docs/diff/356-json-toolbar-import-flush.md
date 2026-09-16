# DIFF-356：JSON 工具栏导入前 flush 脏 Vault 片段

## 问题

[DIFF-355](355-json-drop-nonjson-flush-vault.md) 已在编辑器**拖入**非 JSON 前 `flushJsonVaultEditorIfDirty`。工具栏/「更多」**导入**仍直接 `setText` 覆盖缓冲区：当前仍绑定 Vault 文件且未保存时，磁盘片段落后于即将显示的导入内容，后续保存/冲突行为混乱。

## 行为

- 工具栏与溢出菜单 **导入**：选文件后先 flush（失败则提示并中止），再读入编辑器并 toast。
- 仍为「仅改编辑器缓冲」语义（不入库），与 Electron `importFile` 一致；flush 只保证已打开片段先落盘。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
