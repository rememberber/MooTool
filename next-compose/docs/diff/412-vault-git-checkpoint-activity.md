# DIFF-412：Vault Git 自动检查点活动记录对齐 Electron

## 背景

Electron 在 JSON/随手记 Vault 的 save、建夹、重命名、移动、复制、删除与随手记附件导入后调用 `*CheckpointScheduler.recordActivity`，用于空闲/失焦自动提交的计时与提交说明。Compose 此前仅在保存正文时记录 `Update JSON snippet` / `Update Quick Note`。

## 行为

- `VaultGitCheckpointMessages` 集中 Electron 文案。
- JSON：对话框建夹/移动/重命名、删除确认、复制片段、**树拖放移动**、**树多文件拖放导入**成功后 `recordVaultActivity(..., json = true)`（批量见 [DIFF-421](421-vault-batch-import-checkpoint-activity.md)）。
- 随手记：对应对话框模式、删除、复制、**树拖放移动**、**导入打开单笔记**、**树多文件拖放导入**、附件（文件/剪贴板）记录活动（导入见 [DIFF-414](414-quicknote-vault-import-checkpoint.md)、[DIFF-421](421-vault-batch-import-checkpoint-activity.md)）。

## 验证

- `VaultGitCheckpointMessagesTest`
- `./gradlew :composeApp:desktopTest --offline`
