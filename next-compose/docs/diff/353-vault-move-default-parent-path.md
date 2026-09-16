# DIFF-353：Vault 移动对话框默认目标目录

## 问题

Electron JSON `beginMove` / 随手记 `openTreeAction(move)` 使用 `parentPath`（顶层条目默认 `""`，嵌套条目为父路径）。Compose 移动对话框误用 `VaultSelectionPath.parentDirectory(selected, entries)`：当选中**目录**时返回目录自身，与 Electron 不一致；根级 `Work` 也会预填 `Work` 而非 `""`。

## 行为

- JSON / 随手记：打开移动对话框时，目标目录默认值为 `VaultMove.parentDirectory(被移动路径)`（对齐 Electron `parentPath`）。

## 验证

- `VaultMoveTest` 增补根目录/嵌套目录用例
- `./gradlew :composeApp:desktopTest --offline`
