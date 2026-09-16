# DIFF-445：Electron Git init 全流程单测 + 冲突叠层禁编辑器自动聚焦

## 背景

`vaultGitService.integration.test.ts` 首条用例（init → commit → 工作区/提交 diff）此前仅分散在多个 `GitEngineTest` 中。外部冲突叠层打开时，JSON/随手记应禁止窗口激活抢编辑器焦点（`jsonEditorAutoFocusEnabled` / `quickNoteEditorAutoFocusEnabled`），缺回归。

## 行为

- `GitEngineTest.mirrorsElectronVaultGitInitCommitDiffHistory`：对齐 Electron 首条 integration 用例（含多文件第二次提交 diff）。
- `JsonEditorFocusPolicyTest` / `QuickNoteEditorFocusPolicyTest`：`vaultConflict != null` 时 `autoFocus` 为 false。
- `VaultConflictActionsTest`：直接测 `applyJsonVaultConflictKeep` / `applyJsonVaultConflictSaveCopy`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（584/584）
