# DIFF-217：Vault 删除后清除树选中路径

## 背景

Electron 删除 JSON Vault 条目后会清空与已删路径相关的 `selectedPath`；Compose 此前只处理 `currentFile` 前缀匹配，**`vaultSelectedPath`** 在删除目录或选中子路径时仍指向已不存在的路径。

## 行为

- `VaultMove.clearPathIfDeleted` / `clearVaultPathsAfterDelete`：路径等于被删项或在其子树下则置空。
- JSON `VaultDeleteConfirmOverlay` 与随手记删除对话框成功后同步更新 `vaultSelectedPath`。

## 验证

- `VaultMoveTest.clearPathsAfterDeleteRemovesSelectionUnderFolder`
- `./gradlew :composeApp:desktopTest --offline`
