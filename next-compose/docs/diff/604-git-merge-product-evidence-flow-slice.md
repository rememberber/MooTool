# DIFF-604：A03 Vault Git merge §B 证据链 + 帧 199

基线：DIFF-603（工作区）。

## 范围

- **A03**：`GitMergeProductFlowPresentation.preferredConflictSelectionPath`（多冲突优先 `conflict.json`，对齐 `prepare-git-merge-conflict-evidence.sh`）；`GitMergeProductEvidenceFlowTest` 锁定 pull→autoSelect→resolve→continue 与 `mergeContinueActionEnabled`。
- **证据**：`GitMergeContinueCaptureTest` → `199-compose-git-merge-continue-tab-focus.png`（**不重复** 158 ours / 153 hint）。

**不重复** 603：F04 检查器 JSONPath toast / 帧 198。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
