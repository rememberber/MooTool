# DIFF-624：A03 Vault Git 未解决冲突期 pull UI 禁用

基线：DIFF-623（工作区）。

## 范围

- **A03**：`GitVaultRemotePresentation.pullEnabled`/`pullActionEnabled` 在 `conflicts > 0` 时禁用 pull（落实 [DIFF-520](520-vault-numeric-git-http-curl.md)；与 DIFF-623 push、`commitEnabled` 一致；**fetch 仍可用**）。
- **单测**：`GitVaultRemotePresentationTest.pullDisabledWhileUnresolvedConflicts`；merge/rebase 产品证据流传入 `conflicts`。
- **证据帧**：`217`（`GitVaultPullDisabledConflictsCaptureTest`）。

**不重复** 619/623：619 为 rebase 进行中 fetch 可用 + pull 禁用（`merging=true`）；623 为 push + `conflicts`；本切片补齐 pull + `conflicts` 且 `merging=false` 场景。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
