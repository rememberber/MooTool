# DIFF-609：A03 merge §B push/pull 禁用证据 + 帧 204

基线：DIFF-608（工作区）。

## 范围

- **A03**：`GitVaultRemotePresentation.pushActionEnabled`/`pullActionEnabled`（对齐 `VaultGitDialog` 顶栏 busy+merge）；`GitMergeProductEvidenceFlowTest` 锁定 merge 冲突期 push/pull 禁用、merge 完成后 push 恢复。
- **证据**：`GitMergePushDisabledCaptureTest` → `204-compose-git-merge-push-disabled-tab-focus.png`（**不重复** 199 continue 链）。

**不重复** 608：F01 列编辑闩锁 / 帧 203。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
