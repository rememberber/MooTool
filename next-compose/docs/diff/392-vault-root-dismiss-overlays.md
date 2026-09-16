# DIFF-392：Vault 根切换时关闭 Vault 相关 overlay

## 背景

DIFF-391 在 Vault 有效根变化时重置树 UI 并重载打开文件。若 Git 面板、外部冲突、删除确认等仍打开，会针对**旧仓库**路径操作。

## 行为

- `JsonSession.dismissVaultScopedOverlays` / `QuickNoteSession.dismissVaultScopedOverlays`：关闭 Git 对话框、冲突、删除确认、右键菜单、JSONPath 选择器、Vault CRUD 输入对话框等。
- `OnVaultEffectiveRootChanged` 回调中在重置树 UI 之前调用。

## 测试

- `VaultRootChangeTest`

## 验收

- F01/F04 Vault + Git；`desktopTest --offline`。
