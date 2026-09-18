# DIFF-612：A03 Vault Git rebase §C 证据链 + 帧 207

基线：DIFF-611（工作区）。

## 范围

- **A03**：`GitMergeProductFlowPresentation.productFlowHintKey(..., operation)` 在变基时输出 `git.rebaseProductFlow*`；`VaultGitDialog` 传入 `status.operation`。
- **证据**：`prepare-git-rebase-conflict-evidence.sh` + `GitRebaseProductEvidenceFlowTest`（pull --rebase → autoSelect → resolve → continue）；`GitRebaseContinueCaptureTest` → `207-compose-git-rebase-continue-tab-focus.png`（**不重复** 199 merge continue 链）。

**不重复** 611：F01 外部删除冲突 / 帧 206。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`；`./scripts/verify-product-evidence-prep.sh`
