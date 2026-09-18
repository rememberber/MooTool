# DIFF-611：F01 §A 外部删除冲突证据 + 帧 206

基线：DIFF-610（工作区）。

## 范围

- **F01 §A**：`QuickNoteVaultConflictDeletedProductEvidenceFlowTest`（`sample-external.md` 脏编辑 + `rm` → `deleted` 冲突）；`prepare-vault-conflict-evidence.sh` 步骤 3c。
- **证据**：`QuickNoteConflictDeletedProductEvidenceCaptureTest` → `206-compose-quicknote-external-conflict-deleted-savecopy-tab-focus.png`（**不重复** 202 keep / 610 JSON 帧 205）。

**不重复** 610：F04 JSON 外部删除链。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`（macOS 上若 Gradle daemon 下 executor exit 134，加 `--no-daemon`）
