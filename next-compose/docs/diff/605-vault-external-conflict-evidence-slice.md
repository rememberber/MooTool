# DIFF-605：F04 §A Vault 外部冲突证据链 + 帧 200

基线：DIFF-604（工作区）。

## 范围

- **F04/A03**：`VaultConflictProductEvidencePresentation` 对齐 `prepare-vault-conflict-evidence.sh`（`sample.json` / `sample-external.md` / 磁盘改写 JSON）；`VaultConflictProductEvidenceFlowTest` 锁定监视器→冲突叠层 Presentation；`ProductEvidencePrepScriptTest` 断言路径与正文。
- **证据**：`VaultConflictProductEvidenceCaptureTest` → `200-compose-vault-external-conflict-sample-reload-tab-focus.png`（**不重复** 141 通用 reload 路径文案）。

**不重复** 604：Git merge §B / 帧 199。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
