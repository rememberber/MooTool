# DIFF-614：A03 rebase §C 走查 hint 帧 209

基线：DIFF-613（工作区）。

## 范围

- **A03**：`GitRebaseFlowOverlayCaptureTest` 锁定变基进行中状态胶囊 + `git.rebaseUnresolvedHint` + `git.rebaseProductFlowSelect` + 刷新钮 Tab 焦点（对齐 merge 帧 `153` / `GitMergeFlowOverlayCaptureTest`）。
- **证据**：`209-compose-git-rebase-flow-hint-tab-focus.png`（**不重复** 207 continue / 204 push 禁用链）。

**不重复** 613：F04 检查器结果复制 / 帧 208。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
