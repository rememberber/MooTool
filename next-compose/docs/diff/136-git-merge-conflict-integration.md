# DIFF-136：Git merge 冲突解析/中止/继续

对照 Electron `vaultGitService.integration.test.ts` 的 merge 冲突场景。

## 范围

- `resolvesAndAbortsMergeConflicts`：merge 冲突 → 合并中自动 checkpoint 不新增提交 → `resolveConflict(ours)` → `abortMerge`。
- `continuesMergeAfterConflictResolved`：merge 冲突 → `resolveConflict(theirs)` → `continueOperation`（`commit --no-edit`）→ 工作区干净且内容为 theirs。

## 文件

- `GitEngineTest.kt`
