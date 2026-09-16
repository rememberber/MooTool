# DIFF-362：随手记 Vault 打开/拖移保存失败可见错误

## 问题

[DIFF-360](360-quicknote-directory-select-save.md) / [DIFF-357](357-history-restore-flush-vault.md) 在 `saveIfNeeded` 失败时写 `session.error`。Vault 树 **双击打开** 与 **拖移**（影响当前打开文件时）失败仍静默中止，用户看不到原因（冲突对话框除外）。

## 行为

- `onOpen`：`saveIfNeeded` 失败时 `session.error` 回退为 `quickNote.saveFailed`（保留已有更具体错误）。
- `onMove`：拖移前保存守卫失败时同样写 `error`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
