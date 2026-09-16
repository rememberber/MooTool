# DIFF-112：JSON Vault 打开目录与刷新

对照 `JsonVaultPanel.tsx` 更多菜单中的 `json.vault.openFolder` / `json.vault.refresh`。

## 行为

- **打开 Vault**：`container.openDirectory(jsonVault.root())`，与随手记 `quickNote.openVault` 一致。
- **刷新 Vault**：触发 `onFilter` + `onChanged`，重新拉取树快照（等同 Electron `load()`）。

## 文件

- `JsonScreen.kt`（`VaultPane`）
- `Translator.kt`：`json.vault.openFolder`、`json.vault.refresh`
