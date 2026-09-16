# DIFF-446：Git 提交守卫 + 模态禁焦 + `VaultConflictActions` reload 单测

## 背景

Electron `VaultGitService.commit` 在 merge/rebase 进行中拒绝新提交，并截断说明至 300 字符。JSON/随手记在 Git 面板等模态打开时不应自动聚焦编辑器。`applyJsonVaultConflictReload` 需直接单测覆盖 `clearJsonPathQueryResult`。

## 行为

- `GitEngineTest.rejectsCommitWhileMergeInProgress`、`truncatesCommitMessageToThreeHundredCharacters`。
- `JsonEditorFocusPolicyTest`：Git 面板、Vault 删除确认叠层禁自动聚焦；`QuickNoteEditorFocusPolicyTest`：Git 面板。
- `VaultConflictActionsTest.jsonReloadUsesProductionHelper`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（589/589）
