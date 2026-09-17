# Electron `vaultGitService.integration.test.ts` 对照登记

| caseId | sourceProduct | sourceFile | Compose 验证 |
| --- | --- | --- | --- |
| init-commit-diff-history | MooTool Next Electron | `next/src/shared/vaultGitService.integration.test.ts` | `GitEngineTest.mirrorsElectronVaultGitInitCommitDiffHistory`（[DIFF-445](../diff/445-git-init-parity-focus-conflict-overlay.md)）；`.DS_Store` 见 DIFF-444 |
| pull-merge-conflict-resolve | 同上 | 同上（分叉 pull） | `GitEngineTest.pullLeavesMergeConflictWhenHistoriesDiverge`（[DIFF-137](../diff/137-git-pull-merge-conflict.md)） |
| rebase-continue | Electron 行为 | `VaultGitService` + main | `GitEngineTest.continuesRebaseAfterConflictResolved`（DIFF-135） |
| merge-abort/continue | 同上 | 同上 | `GitEngineTest.resolvesAndAbortsMergeConflicts` / `continuesMergeAfterConflictResolved`（DIFF-136） |
| rejects-unsafe-paths | 同上 | `diff({ path })` | `GitEngineTest.fileDiffsMatchesElectronPathRules`（[DIFF-442](../diff/442-git-filediffs-electron-vaultgit-parity.md)） |
| parent-repo-not-adopted | 同上 | 嵌套 Vault `init` | `GitEngineTest.rejectsParentRepositoryUntilVaultRootIsInitialized`（DIFF-442 加强） |
| automatic-checkpoint-init/push | 同上 | `automaticCheckpoint` | `GitEngineTest.automaticCheckpointInitializesRepositoryOnFirstUse` / `automaticCheckpointPushesWhenRemoteConfigured` |
| unicode-paths | 同上 | status/diff/discard/rename | `GitEngineTest.unicodePathsWorkForStatusDiffDiscardAndRename` |
| stale-index-lock | 同上 | 陈旧 `index.lock` 隔离后提交 | `GitEngineTest.repairsStaleIndexLockAndRetriesCommit`（[DIFF-443](../diff/443-git-index-lock-recovery-concurrency.md)） |
| recent-index-lock | 同上 | 新锁阻塞提交 | `GitEngineTest.recentIndexLockBlocksCommit` |
| held-index-lock | 同上 | 陈旧锁被占用不隔离 | `GitEngineTest.doesNotQuarantineIndexLockHeldOpenByThisProcess`（DIFF-444） |
| commit-during-merge | 同上 | merge 中禁止 commit | `GitEngineTest.rejectsCommitWhileMergeInProgress`（[DIFF-446](../diff/446-git-commit-guard-focus-vault-actions-reload.md)） |
| commit-message-300 | 同上 | 说明截断 300 | `GitEngineTest.truncatesCommitMessageToThreeHundredCharacters` |
| pull-during-merge | 同上 | merge 中禁止 pull | `GitEngineTest.pullBlockedWhileMergeInProgress`（[DIFF-447](../diff/447-git-pull-merge-order-conflict-dialog-click.md)）；`GitPullGuardTest.mergeInProgressRejectedBeforeMissingRemoteMessage`（[DIFF-448](../diff/448-vault-conflict-reload-savecopy-git-pull-guard.md)） |
| continue-unresolved-conflicts | 同上 | 冲突未解决禁止 continue | `GitContinueGuardTest.continueRejectedWhileConflictsRemain`（merge，[DIFF-493](../diff/493-vault-deleted-savecopy-git-continue-guard.md)）；`GitRebaseGuardTest.continueRejectedWhileRebaseConflictsRemain`（rebase，[DIFF-494](../diff/494-git-rebase-pull-commit-guard.md)） |
| pull-during-rebase | 同上 | rebase 中禁止 pull | `GitRebaseGuardTest.pullBlockedWhileRebaseInProgress`（[DIFF-494](../diff/494-git-rebase-pull-commit-guard.md)） |
| git-ui-abort-copy | Vault Git 面板 | merge/rebase 中止/确认文案、continue/counts/resolve/pull 显隐与 busy | `GitOperationPresentationTest`（[DIFF-520](../diff/520-vault-numeric-git-http-curl.md)、[DIFF-539](../diff/539-vault-git-presentation-pull-slice.md)） |
| merge-conflict-ui-hint | Vault Git 面板 | merge/rebase 未解决冲突说明 + 变更行冲突 `<em>` | `GitMergeConflictPresentationTest` + `VaultGitDialog`（[DIFF-545](../diff/545-js-ts-format-git-conflict-nav-slice.md)） |
| commit-during-rebase-conflicts | 同上 | rebase 冲突未清禁止 commit | `GitRebaseGuardTest.commitBlockedWhileRebaseConflictsRemain`（[DIFF-494](../diff/494-git-rebase-pull-commit-guard.md)） |
| abort-rebase-unresolved | 同上 | rebase 冲突态 `rebase --abort` | `GitRebaseGuardTest.abortEndsRebaseWithUnresolvedConflicts`（[DIFF-494](../diff/494-git-rebase-pull-commit-guard.md)） |
| concurrent-commits | 同上 | 双 `VaultGitService` 并行 commit | `GitEngineTest.serializesConcurrentCommitsForSameVaultRoot`（两线程两 message，仅一条 `concurrent.txt` 提交，对齐 Electron `serializes Git actions…separate service instances`） |
| push-non-ff | Electron 行为 | 远程领先时 `push` 失败 | `GitEngineTest.pushFailsWhenRemoteIsAheadWithoutPull`（[DIFF-469](../diff/469-vault-mcp-zod-args-git-push-reject.md)） |
| fetch-prune-behind | 同上 | `fetch` action | `GitEngineTest.fetchUpdatesBehindWithoutPullingWorkingTree` / `fetchFailsWhenRemoteIsNotConfigured`（[DIFF-474](../diff/474-git-fetch-prune-behind-parity.md)） |
| fetch-prune-stale-ref | 同上 | `fetch --prune` | `GitEngineTest.fetchPruneRemovesStaleRemoteTrackingBranch`（[DIFF-478](../diff/478-git-fetch-prune-stale-ref-parity.md)） |
| fetch-diverged-ahead-behind | 同上 | `fetch` + `status` ahead/behind | `GitEngineTest.fetchReportsAheadAndBehindWhenBranchesDiverge`（[DIFF-479](../diff/479-git-fetch-diverged-ahead-behind-parity.md)） |
| configure-remote-remove | 同上 | 空 URL 删除 origin | `GitEngineTest.configureRemoteRemoveClearsOriginFromStatus` / `configureRemoteRemoveWhenAlreadyRemovedIsIdempotent`（[DIFF-475](../diff/475-git-configure-remote-remove-parity.md)） |
| push-unresolved-conflicts | 同上 | 冲突未清禁止 push | `GitPushGuardTest.pushBlockedWhileMergeConflictsRemain`（[DIFF-517](../diff/517-settings-catalog-git-push-host-json.md)、[DIFF-518](../diff/518-settings-translation-commit-listpaths.md)） |

UI：`VaultGitDialog` 冲突行「使用本地/远端」Compose Tab 帧 `142` 见 [DIFF-437](../diff/437-git-conflict-actions-quicknote-copy.md)（非产品主窗）；顶栏分支图标 + 操作钮 Lucide 对齐见 [DIFF-540](../diff/540-vault-git-conflict-reformat-nav-slice.md)。
