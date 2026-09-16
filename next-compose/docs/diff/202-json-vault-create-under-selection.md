# DIFF-202：JSON Vault 新建文件/文件夹落在当前选中目录

## 背景

Electron `JsonVaultPanel.beginCreateFile` / `beginCreateFolder` 使用 `selectedDirectory(selectedEntry)` 作为父路径。

Compose 此前「新建」始终在 Vault 根目录创建；「新文件夹」对话框默认名称为固定 `folder`，未带上父路径。

## 行为

- 新增 `VaultSelectionPath.parentDirectory` / `join`（对照 `selectedDirectory` / `parentPath`）。
- F04：「新建」片段与「新文件夹」默认路径基于 `vaultSelectedPath`（否则 `currentFile`）。
- i18n 增加 `json.vault.defaultFolder`。

## 验证

- `VaultSelectionPathTest`
- `./gradlew :composeApp:desktopTest --offline`
