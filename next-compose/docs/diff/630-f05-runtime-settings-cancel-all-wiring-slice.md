# DIFF-630：F05/A01 运行环境设置与切页 cancelAll 接线

基线：DIFF-629（F05 运行台 displayName/cancel 接线）。

## 范围

- **A01**：`RuntimeSettingsPanel` 检测与行标签走 `CodeRunWiringPresentation.pathsFrom` / `runDetect` / `displayName`（与 F05 运行台同源）。
- **F05**：`SessionManager.cancelCodeRun` → `CodeRunWiringPresentation.cancelAllRuns`（切页/关闭杀在途进程，对齐 [DIFF-242](242-tool-leave-cancel-coderun-regex-hardware.md)）。
- **单测**：`cancelAllRunsWithoutActiveJobsDoesNotThrow`、`runDetectReturnsFourRuntimeStatuses`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
