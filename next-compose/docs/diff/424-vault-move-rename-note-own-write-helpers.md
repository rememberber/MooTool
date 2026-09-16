# DIFF-424：Vault 移动/重命名/保存与 `noteOwnWrite` 辅助函数

## 背景

[DIFF-422](422-vault-import-note-own-write.md)～[DIFF-423](423-vault-duplicate-note-own-write.md) 为导入/复制登记监视器哈希。树 **拖移**、对话框 **重命名/移动/新建**、**保存** 仍可能触发 `VaultRevisionMonitor` 误报外部变更。

## 行为

- 新增 `VaultMonitorOwnWrite.kt`：`noteOwnWriteJsonVaultFile` / `noteOwnWriteQuickNoteFile`（从 Vault 读盘正文再登记）。
- JSON：树 `onMove`、Vault 对话框成功（非 `json-folder`）、`saveJsonVault`、编辑器/树导入与拖放改用辅助函数。
- 随手记：树 `onMove`、对话框成功（非 `folder`）、`saveQuickNote`、导入/复制/拖放改用辅助函数。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（531/531）
