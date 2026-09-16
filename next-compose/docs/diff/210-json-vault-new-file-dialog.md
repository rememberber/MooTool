# DIFF-210：JSON Vault「新建」改为路径对话框

## 背景

Electron `JsonVaultPanel.beginCreateFile` 打开文本对话框，默认 `parent/snippet.json`；提交时 `saveJsonVaultFile`，正文为当前编辑器内容或 `{\n\n}`。

Compose「新建」曾一键生成带时间戳的文件名并立即创建，用户无法编辑路径，默认正文为 `{\n}\n`。

## 行为

- 「新建」与「新文件夹」对称：设置 `dialogInputMode = json-file`，默认 `VaultSelectionPath.join(parent, "snippet.json")`。
- 保存时 `createFile`、打开编辑器、更新 `currentFile`/`vaultSelectedPath`/`savedText` 与监视器；空编辑器用 `{\n\n}`。
- 移除时间戳一键创建。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
