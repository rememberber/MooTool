# DIFF-625：A03 `GitEngine.pull` 未解决冲突引擎守卫

基线：DIFF-624（工作区）。

## 范围

- **A03**：`GitEngine.pull` 在 `conflicts > 0` 且非 merge/rebase 进行中时拒绝（文案对齐 push：`Resolve all conflicts before pulling`）；merge/rebase 进行中仍优先返回原有 merging 文案。
- **单测**：`GitPullGuardTest.pullBlockedWhileMergeConflictsRemain`（与 `GitPushGuardTest` 同构 merge 冲突夹具）。

**不重复** 624：624 为 Vault Git 顶栏 `pullActionEnabled(..., conflicts)` UI；本切片补引擎层与 DIFF-520 文档闭环。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
