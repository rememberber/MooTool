# DIFF-610：F04 §A 外部删除冲突证据 + 帧 205

基线：DIFF-609（工作区）。

## 范围

- **F04 §A**：`VaultConflictDeletedProductEvidenceFlowTest`（`sample.json` 脏编辑 + `rm` → `deleted` 冲突 + Presentation 断言）；`prepare-vault-conflict-evidence.sh` 步骤 3b 注释。
- **证据**：`VaultConflictDeletedProductEvidenceCaptureTest` → `205-compose-vault-external-conflict-sample-deleted-savecopy-tab-focus.png`（**不重复** 200 reload / 146 泛化删除帧）。

**不重复** 609：A03 merge push 禁用 / 帧 204。

## 其它

- `composeApp/build.gradle.kts`：`desktopTest` 设 `maxParallelForks=1` / `forkEvery=80` / `-Xmx3g`，减轻 macOS 全量 Compose 测试 JVM exit 134。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`（macOS Gradle daemon 下偶发 executor exit 134；定向与全量均已验证）
