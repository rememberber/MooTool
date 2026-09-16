# DIFF-204：随手记新建空笔记、JSON 建夹选中路径、代理字段禁用态

## 背景

- Electron `createQuickNote` 只创建空笔记并选中，不把当前编辑区正文写入新文件。
- Electron `JsonVaultPanel.submitTextAction` 建文件夹后 `setSelectedEntry({ path, kind: 'directory' })`。
- Electron 设置页在 `proxyEnabled === false` 时禁用 host/port/用户名/密码字段。

## 行为

- F01：新建笔记后 `openFile` 使用 `createNote` 返回的空文档，不再把当前编辑器内容写入新笔记。
- F04：JSON Vault 对话框创建文件夹成功后 `vaultSelectedPath` 指向新目录（对齐树选中目录节点）。
- A01：`SettingTextField` / `MooTextField` 支持 `enabled`；网络设置代理关闭时四个文本字段不可编辑且 0.42 透明度。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
