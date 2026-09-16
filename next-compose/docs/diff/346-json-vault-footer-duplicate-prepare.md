# DIFF-346：JSON Vault 底栏复制前 `prepareJsonVaultContext`

## 问题

[DIFF-344](344-json-vault-more-selection-duplicate.md) 已为 Vault **更多**菜单复制补 `prepareJsonVaultContext`。底栏 **复制**仍直接 `duplicateJsonVaultSnippet`，与底栏重命名/移动/删除及树右键（先 prepare 再动作）不一致。

## 行为

- JSON Vault 底栏 **复制**：先 `prepareJsonVaultContext`，再 `duplicateJsonVaultSnippet`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
