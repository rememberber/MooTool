# DIFF-218：JSON Vault 复制片段选中与脏保存

## 背景

Electron 复制 JSON 片段后会在编辑器打开副本并更新选中条目。Compose 此前只更新 `currentFile`，**`vaultSelectedPath`** 仍指向源文件；且 `duplicate` 读盘，当前文件有未保存修改时副本可能基于旧内容。

## 行为

- `loadJsonVaultSnippet`：打开片段时同步 `currentFile`、`vaultSelectedPath`、`savedText` 与编辑器正文。
- `duplicateJsonVaultSnippet`：复制前若当前片段脏则静默 `saveJsonVault`；成功后 `loadJsonVaultSnippet`。
- `saveJsonVault` 抽到 `JsonVaultSession.kt` 供保存/复制共用。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
