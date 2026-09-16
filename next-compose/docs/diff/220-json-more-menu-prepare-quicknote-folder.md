# DIFF-220：JSON 更多菜单前置保存与随手记建夹路径解析

## 背景

- JSON Vault 树右键在重命名/移动前会 `prepareJsonVaultContext`（DIFF-215），**更多**菜单中的重命名/移动此前未走同一流程，且重命名预填未用 `jsonVaultRenameDefault`。
- 随手记新建文件夹对话框应对单段/多段路径与 JSON DIFF-219 一致，使用 `resolveEntryPath`。

## 行为

- JSON **更多 → 重命名/移动**：`prepareJsonVaultContext`；移动默认目标为 `VaultSelectionPath.parentDirectory`；重命名预填 `jsonVaultRenameDefault(entry)`。
- 随手记 **新建文件夹**：`VaultSelectionPath.resolveEntryPath`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
