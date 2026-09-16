# DIFF-364：`prepareJsonVaultContext` 提取与冲突失败单测

## 背景

[DIFF-363](363-vault-prepare-save-failure-feedback.md) 在 `prepareJsonVaultContext` 内写入 flush 失败 `notice`；逻辑原先为 `JsonScreen` 私有函数，不便回归。

## 变更

- 将 `prepareJsonVaultContext` 移至 `JsonVaultSession.kt`（与 `saveJsonVault` / `flushJsonVaultEditorIfDirty` 同模块）。
- 新增 `JsonVaultPrepareContextTest`：脏缓冲 + 磁盘冲突时 `prepare` 返回 `false`、设置 `notice` 与 `vaultConflict`，并更新 `vaultSelectedPath`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
