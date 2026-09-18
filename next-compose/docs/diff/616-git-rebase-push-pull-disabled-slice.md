# DIFF-616：A03 rebase §C push/pull 禁用 + 帧 211

基线：DIFF-615（工作区）。

## 范围

- **A03**：`GitRebaseProductEvidenceFlowTest` 补充 rebase 冲突期 `pullActionEnabled` 禁用（对齐 DIFF-609 merge 链）。
- **证据**：`GitRebasePushPullDisabledCaptureTest` → `211-compose-git-rebase-push-pull-disabled-tab-focus.png`（变基胶囊 + 禁用 push/pull；**不重复** merge 帧 `204`）。

**不重复** 615：resolve 帧 `210`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
