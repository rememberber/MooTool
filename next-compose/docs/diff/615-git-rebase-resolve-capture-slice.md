# DIFF-615：A03 rebase §C ours/theirs 帧 210

基线：DIFF-614（工作区）。

## 范围

- **A03**：`GitRebaseResolveCaptureTest` → `210-compose-git-rebase-resolve-tab-focus.png`（rebase 冲突期与 merge 共用 `GitMergeProductFlowPresentation.resolveActionsEnabled` / `mooGitMergeResolveRow`；**不重复** merge 帧 `158`）。

**不重复** 614：rebase hint 帧 `209`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
