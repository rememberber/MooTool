# DIFF-441：Vault 冲突「重新加载 / 保留编辑」会话流 + 删除态 UI 帧

## 背景

DIFF-440 锁定「另存副本」与 `deleted` 冲突检测。`JsonScreen` / `QuickNoteScreen` 的 `onReload`、`onKeep` 分支（含删除态无「载入磁盘」钮）仍缺会话级单测与删除态 Compose Tab 证据。

## 行为

- `JsonVaultConflictReloadKeepFlowTest` / `QuickNoteVaultConflictReloadKeepFlowTest`：镜像 `VaultConflictDialog` `onReload`（含 `clearJsonPathQueryResult`）与 `onKeep`。
- `VaultConflictCaptureTest`：`146-compose-vault-conflict-deleted-savecopy-tab-focus.png`（`deleted=true` 时仅「另存副本」+「保留编辑」）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（570/570）
