# DIFF-618：A03 merge/rebase 冲突期提交禁用 + 帧 212/213

基线：DIFF-617（工作区）。

## 范围

- **A03**：`GitMergeProductEvidenceFlowTest` / `GitRebaseProductEvidenceFlowTest` 补充冲突未清时 `GitOperationPresentation.commitEnabled` 为 false（对齐 Electron `VaultGitDialog` + DIFF-494 引擎守卫）。
- **证据**：`GitVaultCommitDisabledCaptureTest` → `212-compose-git-merge-commit-disabled-tab-focus.png`、`213-compose-git-rebase-commit-disabled-tab-focus.png`（**不重复** 211 push/pull 链）。

**不重复** 617：resolve 提示 / pull 恢复断言链。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
