# DIFF-358：JSON 转换结果/弹层写入编辑器前 flush

## 问题

[DIFF-357](357-history-restore-flush-vault.md) 已覆盖历史恢复。检查器 **结果弹层「使用输出」**（`jsonToXml`/`jsonToBean` 等）与 **XML/Bean 转换弹层**成功写入主编辑器时仍直接 `setText`，未保存的 Vault 片段可能丢失。

## 行为

- `ResultDialog`「使用输出」：先 `flushJsonVaultEditorIfDirty`，失败则提示并保持弹层。
- `InputDialog`「运行」成功路径：同样在 `setText` 前 flush。
- 工具栏 `transform`（格式化/压缩等）仍只改当前缓冲，不额外 flush（输入即 `editor.text`）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
