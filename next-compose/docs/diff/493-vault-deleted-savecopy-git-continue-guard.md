# DIFF-493：外部删除冲突另存副本 + Git 继续合并守卫

## 背景

DIFF-440 已锁定 JSON/随手记「磁盘仍在」时的 `onSaveCopy` 会话流，以及 `deleted=true` 冲突态的产生。`applyJsonVaultConflictSaveCopy` / `applyQuickNoteVaultConflictSaveCopy` 在 **外部删除** 分支会把 `currentFile` 切到 `*.local-{epoch}.*` 并保留编辑器正文，此前未单测。Git `continueOperation` 在 Electron 于 `conflicts > 0` 时返回失败（`Resolve all conflicts before continuing`），需独立回归避免与大量集成用例耦合。

## 行为

- `JsonVaultConflictSaveCopyFlowTest.saveCopyWhenExternallyDeletedOpensCopyAsCurrentFile`：对齐 `VaultConflictActions` 删除分支（副本落盘、会话指向副本、清除 `vaultConflict`）。
- `QuickNoteVaultConflictSaveCopyFlowTest.saveCopyWhenExternallyDeletedOpensCopyAsCurrentFile`：随手记对称（frontmatter 解析正文）。
- `GitContinueGuardTest.continueRejectedWhileConflictsRemain`：merge 冲突未 `resolve-conflict` 时 `continueOperation` 失败且文案含 `conflict`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（JDK 21）
