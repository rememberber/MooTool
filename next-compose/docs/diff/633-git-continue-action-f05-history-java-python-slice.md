# DIFF-633：A03 `continueActionEnabled` + 产品证据流 commit 钮 + F05 历史 Java/Python

基线：DIFF-632（工作区）。

## 范围

- **A03**：`GitOperationPresentation.continueActionEnabled(busy, …)` 与 `commitActionEnabled` 同层；`GitMergeProductFlowPresentation.mergeContinueActionEnabled` 委托该 helper。
- **A03**：`GitMergeProductEvidenceFlowTest` / `GitRebaseProductEvidenceFlowTest` 冲突期断言改走 `commitActionEnabled`（与 `VaultGitDialog`/帧 `212`/`213` 一致）。
- **F05**：`CodeRunHistoryRestoreTest` 无 `options` 时从摘要前缀恢复 **Java** / **Python** Tab（`CodeRunWiringPresentation.displayName`）。

**不重复** 632：632 已做 Groovy/Node 摘要推断与 commit 钮接线。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
