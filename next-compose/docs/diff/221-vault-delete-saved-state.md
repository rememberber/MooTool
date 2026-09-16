# DIFF-221：Vault 删除后打开文件脏状态

## 背景

Electron 删除当前 JSON 片段时清空 `selectedPath` 与 `savedContent`（`''`），不再把磁盘状态与已删文件绑定。

Compose DIFF-217 在清空 `currentFile` 时仍设 `savedText = editor.text`，导致无打开文件却显示「未脏」，且与 Electron 不一致。

## 行为

- `vaultPathsAfterDelete`：在 DIFF-217 路径清理基础上，若打开文件被删则 `clearedOpenFile=true`。
- JSON：清空 `savedText`（编辑器正文保留，用户可另存为新片段）。
- 随手记：清空编辑器、`savedText` 与 metadata（与选中目录时一致）。

## 验证

- `VaultPathsAfterDeleteTest`
- `./gradlew :composeApp:desktopTest --offline`
