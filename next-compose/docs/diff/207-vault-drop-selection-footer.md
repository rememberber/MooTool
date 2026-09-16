# DIFF-207：Vault 树拖入目录按选中路径 + 随手记底栏选中路径

## 背景

Electron Vault 导入/拖放落在 `selectedDirectory(selectedEntry)`。Compose JSON/随手记树区域拖入此前只用 `currentFile` 的父目录，树里选中目录或另一文件时目标目录错误。

JSON Vault 已有 `VaultSelectionFooter`（DIFF-196）；随手记仅有状态栏路径，侧栏缺少与 JSON 一致的 31dp 底栏。

## 行为

- JSON / 随手记树拖入：`VaultSelectionPath.parentDirectory(vaultSelectedPath ?: currentFile, items)`。
- F01：随手记 Vault 列增加 `VaultSelectionFooter`（路径 + 当前文件未保存 `•`）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
