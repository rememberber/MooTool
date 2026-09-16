# DIFF-216：Vault 重命名/移动后同步选中路径

## 背景

Electron `JsonVaultPanel.updateSelectionAfterPathChange` 在重命名/移动后同时更新 `selectedEntry`、`selectedPath`，并对子路径做前缀替换。

Compose 此前只 `retargetAfterMove` 更新 `currentFile`；`vaultSelectedPath` 在 JSON 侧常不更新，随手记移动对话框还曾把选中路径直接设为 `next`（目录移动时子路径选中会错）。

## 行为

- `VaultMove.retargetVaultPaths`：对 `currentFile` 与 `vaultSelectedPath` 各调用 `retargetAfterMove`。
- JSON/随手记：树拖放移动、对话框重命名/移动均走该辅助函数。

## 验证

- `VaultMoveTest.retargetVaultPathsUpdatesSelectionAndOpenFile`
- `./gradlew :composeApp:desktopTest --offline`
