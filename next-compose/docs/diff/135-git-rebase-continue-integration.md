# DIFF-135：Git rebase 冲突后继续

对照 Electron `vaultGitService.integration.test.ts` 的 rebase 冲突解析 + `continue-operation` 流程。

## 范围

- 单测 `GitEngineTest.continuesRebaseAfterConflictResolved`：feature 分支 rebase 到 main 产生冲突 → `resolveConflict(theirs)` → `continueOperation` → `operation=none`、无冲突。
- 与 UI `VaultGitDialog` 中「继续」按钮（`git.continue`）对应的后端能力验收。

## 文件

- `GitEngineTest.kt`
