# DIFF-594：F02/F10/F21 失败 toast 呈现层 + 帧 189

基线：DIFF-593（工作区）。

## 范围

- **F02**：`TextDiffPresentation.shouldToastImportFailure` + `notifyDiffImportFailure`（左右导入读盘失败）。
- **F10**：`HostWiringPresentation.shouldToastOperationFailure` / `shouldToastIoFailure` + 扩展 `notifyHostFailure`（方案导入/导出 IO 失败走 `io=true`）。
- **F21**：`CalculatorWiringPresentation.shouldToastOperationFailure` + `notifyCalculatorFailure`。
- **证据**：`CalculatorEvaluateTabCaptureTest` → `189-compose-calculator-evaluate-tab-focus.png`。

**不重复** 593：F14/F15/F16/F19 / 帧 188。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
