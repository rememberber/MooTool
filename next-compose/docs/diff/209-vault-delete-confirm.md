# DIFF-209：JSON/随手记 Vault 删除确认

## 背景

Electron `JsonVaultPanel` 与随手记删除前使用 `desktopDialog.confirm`（`json.vault.confirmDelete` / `quickNote.confirmDelete`）。

Compose 右键与当前文件工具栏删除为立即执行，无确认。

## 行为

- 共享 `VaultDeleteConfirmOverlay`（应用内 overlay、危险删除钮）。
- F04：JSON Vault 右键与底栏「删除」先确认再 `jsonVault.delete`。
- F01：随手记 Vault 右键与底栏「删除」先确认再 `noteVault.delete`（非空目录仍由存储层报错）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
