# DIFF-222：Vault 底栏操作针对树选中路径

## 背景

Electron 随手记 Vault 删除等操作针对 `selectedPath`（可为目录且无打开文件）。Compose 底栏重命名/移动/删除/复制仅绑定 `currentFile`，树里选中目录时无法操作或误操作打开文件。

## 行为

- JSON / 随手记：底栏动作目标为 `vaultSelectedPath.ifBlank { currentFile }`（与 `VaultSelectionFooter` 一致）。
- 重命名/移动/删除前走 `prepareJsonVaultContext` / `prepareQuickNoteVaultContext`。
- **复制**仅在目标为文件时显示（目录无 duplicate）。
- JSON 底栏补充 **移动**（与随手记底栏动作集一致；Electron JSON 仅在更多/右键提供移动）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
