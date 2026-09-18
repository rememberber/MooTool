# DIFF-632：A03 commit 钮 `commitActionEnabled` + F05 历史恢复显示名

基线：DIFF-631（A03 冲突期 automaticCheckpoint 跳过）。

## 范围

- **A03**：`GitOperationPresentation.commitActionEnabled(busy, …)` 与 push/pull 顶栏 `*ActionEnabled` 同层；`VaultGitDialog` 提交钮与证据帧 `212`/`213` 走同一 helper。
- **F05**：`CodeRunHistoryRestore` 摘要前缀匹配 `CodeRunWiringPresentation.displayName`；`CodeRunHistoryRestoreTest.infersRuntimeFromSummaryUsingWiringDisplayNames`（Groovy 无 options JSON）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
