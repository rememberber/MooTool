# DIFF-224：JSON Vault 新建文件前保存守卫

## 背景

与 DIFF-223 随手记新建笔记、DIFF-218 复制片段同理：在 Vault 对话框 **新建 JSON 文件**并切换编辑器前，若当前片段脏却未落盘，旧文件修改会丢失。

## 行为

- `flushJsonVaultEditorIfDirty`：有 `currentFile` 且正文脏时静默 `saveJsonVault`。
- `json-file` 对话框提交前先 flush；失败则中止创建。
- `duplicateJsonVaultSnippet` 复用同一 flush 辅助函数。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
