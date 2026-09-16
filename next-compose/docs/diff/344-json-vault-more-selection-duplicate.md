# DIFF-344：JSON Vault「更多」菜单对齐树选中路径

## 问题

[DIFF-222](222-vault-footer-actions-selection.md) 已将底栏动作绑定 `vaultSelectedPath.ifBlank { currentFile }`，但 Vault **更多**菜单仍只用 `session.currentFile`（与 Electron `selectedEntry` 不一致）。树里选中某片段而编辑器仍打开另一文件时，更多 → 重命名/移动/复制会误作用于打开文件。复制也未在目录选中时禁用，且未走 `prepareJsonVaultContext`。

## 行为

- 更多菜单目标路径：`vaultSelectedPath.ifBlank { currentFile }`。
- **复制**：仅当选中项为文件时启用；执行前 `prepareJsonVaultContext`。
- 重命名/移动：沿用 `prepareJsonVaultContext`，条目来自树 `items`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
