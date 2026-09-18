# DIFF-634：A03 Vault Git fetch/init `*ActionEnabled`

基线：DIFF-633（工作区）。

## 范围

- **A03**：`GitVaultRemotePresentation.fetchActionEnabled(persistedRemote, busy)` 与顶栏 fetch 钮、`GitVaultFetchDuringConflictCaptureTest` 帧 `214` 一致（冲突期 fetch 仍可用、pull 禁用）。
- **A03**：`GitOperationPresentation.initActionEnabled`；`VaultGitDialog` init 钮去掉重复 `!busy` 判断。
- **单测**：`GitVaultRemotePresentationTest.fetchActionEnabledRespectsBusyAndRemote` / `fetchStaysEnabledDuringMergeWithConflicts`。

**不重复** 633：633 为 continue/commit 与 F05 历史；本切片仅 fetch/init 顶栏 helper。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
