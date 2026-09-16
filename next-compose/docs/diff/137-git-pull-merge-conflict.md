# DIFF-137：Git pull 产生 merge 冲突

对照产品需求「pull/push/冲突」与本地 merge 冲突单测（DIFF-136），用 bare remote 验证 **pull** 路径。

## 范围

- `GitEngineTest.pullLeavesMergeConflictWhenHistoriesDiverge`：上游 push 与本地提交分叉 → `pull` 失败并进入 merge 冲突 → `resolveConflict(theirs)` + `continueOperation` → 内容为 remote 侧。

## 文件

- `GitEngineTest.kt`
