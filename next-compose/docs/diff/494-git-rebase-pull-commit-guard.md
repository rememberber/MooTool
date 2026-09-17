# DIFF-494：Rebase 冲突期间 pull / commit / continue / abort 守卫

## 背景

DIFF-135 已覆盖 rebase 冲突解决后 `continueOperation` 成功路径；DIFF-447～448、493 分别锁定 merge 中 `pull` 与未解决冲突禁止 `continue`。Electron `VaultGitService` 在 **rebase 进行中**（`status.merging`）同样拒绝 `pull`，在冲突未清前拒绝 `commit`/`continue-operation`，并可通过 `merge --abort` 或 `rebase --abort` 结束操作。merge 对称单测已有，rebase 分支需独立回归避免与大型集成用例耦合。

## 行为

- `GitRebaseGuardTest.pullBlockedWhileRebaseInProgress`：`rebase` 冲突未解决时 `pull` 失败，文案含 merge/rebase 语义且不含 remote 缺失提示（对齐 DIFF-448 顺序）。
- `GitRebaseGuardTest.commitBlockedWhileRebaseConflictsRemain`：冲突存在时 `commit` 失败。
- `GitRebaseGuardTest.continueRejectedWhileRebaseConflictsRemain`：未 `resolve-conflict` 时 `continueOperation` 失败且文案含 `conflict`。
- `GitRebaseGuardTest.abortEndsRebaseWithUnresolvedConflicts`：`abortMerge` 在 rebase 冲突态调用 `rebase --abort`，恢复 `operation=none`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（JDK 21）
