# DIFF-607：F01 §A 随手记外部冲突证据链 + 帧 202

基线：DIFF-606（工作区）。

## 范围

- **F01**：`VaultConflictProductEvidencePresentation` 扩展 `sample-external.md` 正文/标记；`QuickNoteVaultConflictProductEvidenceFlowTest`；`ProductEvidencePrepScriptTest` 断言随手记样本。
- **证据**：`QuickNoteConflictProductEvidenceCaptureTest` → `202-compose-quicknote-external-conflict-keep-tab-focus.png`（**不重复** 145 通用 keep / 200 JSON sample）。

**不重复** 606：IME / 帧 201。

## 附带

- 稳定 `UpdateAutoCheckSchedulerTest.passesLatestAutoDownloadFlagOnEachCheck` 时序（全量 desktopTest 偶发失败）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
