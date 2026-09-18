# DIFF-619：A03 merge/rebase 冲突期 fetch 可用 + 帧 214

基线：DIFF-618（工作区）。

## 范围

- **A03**：对齐 Electron `VaultGitDialog`：`fetch` 在 merge/rebase 进行中仍可用（仅要求 remote）；`pull` 在 `merging` 时禁用。
- **证据流**：`GitMergeProductEvidenceFlowTest` / `GitRebaseProductEvidenceFlowTest` 冲突态断言 `fetchEnabled` + 真实 `GitEngine.fetch` 成功。
- **证据帧**：`GitVaultFetchDuringConflictCaptureTest` → `214-compose-git-rebase-fetch-enabled-pull-disabled-tab-focus.png`（**不重复** 211 push/pull 禁用链）。

**不重复** 618：提交禁用帧 `212`/`213`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
