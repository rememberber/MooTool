# DIFF-440：JSON Vault 冲突「另存副本」与外部删除会话流

## 背景

DIFF-436～439 已覆盖冲突对话框 Tab 帧、`jsonVaultApplyExternalChange` 脏编辑冲突、监视器 wiring 与 Vault 层 `write(copy)` 单测。`JsonScreen.onSaveCopy` 在写副本后还会把编辑器重载为磁盘版本并更新 `savedText`/`currentFile`，且外部删除时 `applyExternalChange` 应产出 `deleted=true` 冲突态——此前未在会话层单测锁定。

## 行为

- `JsonVaultConflictSaveCopyFlowTest.saveCopyWritesSiblingAndReloadsDiskIntoEditor`：对齐 `JsonScreen` `onSaveCopy` 非删除分支（副本旁路 + `read(relativePath)` 回灌编辑器）。
- `JsonVaultConflictSaveCopyFlowTest.externalDeleteWhileDirtyYieldsDeletedConflict`：`jsonVault.delete` 后脏打开文件经 `jsonVaultApplyExternalChange` 得到 `VaultConflictState.deleted`。
- `QuickNoteVaultConflictSaveCopyFlowTest`：随手记对称覆盖 `quickNoteOpenVaultFile` 回灌与 `quickNoteApplyExternalChange` 删除冲突。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（563/563，含本 DIFF 新增 4 例）
