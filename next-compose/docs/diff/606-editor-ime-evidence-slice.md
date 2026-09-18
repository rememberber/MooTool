# DIFF-606：P0/F04 列编辑·IME 证据脚本对齐 + 帧 201

基线：DIFF-605（工作区）。

## 范围

- **P0/F04**：`EditorColumnEditPresentation` 扩展 IME 样本正文/标记与 `matchesEvidence*`（对齐 `prepare-editor-ime-evidence.sh`）；`ProductEvidencePrepScriptTest` 断言路径与正文。
- **证据**：`JsonColumnEditLatchCaptureTest` → `201-compose-json-column-edit-latch-tab-focus.png`（**不重复** 产品窗 `115`/`122`）。

**不重复** 605：Vault §A / 帧 200。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
