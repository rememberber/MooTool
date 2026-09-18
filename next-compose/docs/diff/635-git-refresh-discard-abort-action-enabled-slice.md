# DIFF-635：A03 Vault Git refresh/discard/abort/resolve `*ActionEnabled`

基线：DIFF-634（工作区）。

## 范围

- **A03**：`GitOperationPresentation.refreshActionEnabled` / `discardActionEnabled` / `abortActionEnabled` / `resolveConflictActionEnabled`；`VaultGitDialog` 顶栏刷新、变更丢弃、中止与 merge resolve 钮接线。
- **A03**：`GitMergeProductFlowPresentation.resolveActionsEnabled` 委托 `resolveConflictActionEnabled`。
- **单测**：`GitOperationPresentationTest` 补充 busy 门禁。

**不重复** 634：634 为 fetch/init；本切片为 refresh/discard/abort/resolve。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
